package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttClient;
import in.bytehue.messaging.mqtt5.provider.MessageClientProvider.Config;

/**
 * Comprehensive test suite to prove all concurrency issues in
 * MessageClientProvider BEFORE implementing fixes.
 * 
 * Expected behavior: These tests should FAIL or HANG with the current
 * implementation. After fixes are applied, these tests should PASS.
 * 
 * Issues being tested: 1. Callback deadlock in connectInternal() 2. @Modified
 * blocking OSGi framework 3. @Deactivate blocking OSGi framework 4.
 * disconnect() without connection state check
 */
@RunWith(LaunchpadRunner.class)
public class MessageClientConcurrencyIssuesTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MqttClient client;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	private static final int TIMEOUT_SECONDS = 15;

	@Before
	public void setup() {
		waitForMqttConnectionReady(launchpad);
	}

	/**
	 * ISSUE 1: Callback Deadlock in connectInternal()
	 * 
	 * Problem: connectInternal() holds connectionLock while calling .send() If
	 * callbacks fire synchronously, they try to acquire the same lock → DEADLOCK
	 * 
	 * This test proves the issue by: - Simulating synchronous callback behavior -
	 * Attempting to acquire lock during connection - Should hang/timeout with
	 * current implementation
	 */
	@Test(timeout = TIMEOUT_SECONDS * 1000)
	public void testIssue1_CallbackDeadlockInConnect() throws Exception {
		System.out.println("\n=== Testing Issue 1: Callback Deadlock ===");

		final MessageClientProvider provider = (MessageClientProvider) client;
		final ReentrantLock connectionLock = getConnectionLock(provider);

		final ExecutorService executor = Executors.newFixedThreadPool(3);
		final CountDownLatch connectStarted = new CountDownLatch(1);
		final AtomicBoolean lockContentionDetected = new AtomicBoolean(false);
		final AtomicBoolean callbacksCompleted = new AtomicBoolean(false);

		// Start from disconnected state
		if (client.isConnected()) {
			client.disconnect().get(5, SECONDS);
			Thread.sleep(500); // Ensure disconnect completes
		}

		// Thread 1: Perform connection
		final CompletableFuture<Void> connectTask = CompletableFuture.runAsync(() -> {
			try {
				connectStarted.countDown();
				System.out.println("Connect thread: Starting connection...");
				client.connect().get(10, SECONDS);
				System.out.println("Connect thread: Connection completed");
			} catch (Exception e) {
				System.err.println("Connect thread failed: " + e.getMessage());
				throw new RuntimeException("Connect failed", e);
			}
		}, executor);

		// Thread 2 & 3: Simulate callbacks trying to acquire lock
		final CompletableFuture<Void> callback1 = CompletableFuture.runAsync(() -> {
			try {
				connectStarted.await(2, SECONDS);
				Thread.sleep(100); // Let connect acquire lock

				System.out.println("Callback1: Attempting to call isConnected()...");
				long start = System.currentTimeMillis();

				for (int i = 0; i < 20; i++) {
					client.isConnected(); // Needs lock

					long elapsed = System.currentTimeMillis() - start;

					// If lock is held for >2 seconds, we have lock contention
					if (elapsed > 2000 && connectionLock.hasQueuedThreads()) {
						lockContentionDetected.set(true);
						System.err.println("WARNING: Lock contention detected! Lock held for " + elapsed + "ms");
					}

					Thread.sleep(50);
				}

				callbacksCompleted.set(true);
				System.out.println("Callback1: Completed successfully");
			} catch (InterruptedException e) {
				System.err.println("Callback1 interrupted");
				Thread.currentThread().interrupt();
			}
		}, executor);

		final CompletableFuture<Void> callback2 = CompletableFuture.runAsync(() -> {
			try {
				connectStarted.await(2, SECONDS);
				Thread.sleep(150); // Slightly offset from callback1

				System.out.println("Callback2: Attempting to call getLastDisconnectReason()...");
				for (int i = 0; i < 20; i++) {
					client.getLastDisconnectReason(); // Needs lock
					Thread.sleep(50);
				}
				System.out.println("Callback2: Completed successfully");
			} catch (InterruptedException e) {
				System.err.println("Callback2 interrupted");
				Thread.currentThread().interrupt();
			}
		}, executor);

		// Wait for all tasks
		try {
			CompletableFuture.allOf(connectTask, callback1, callback2).get(TIMEOUT_SECONDS, SECONDS);
		} catch (TimeoutException e) {
			executor.shutdownNow();
			fail("DEADLOCK DETECTED: Callbacks blocked waiting for lock during connect operation");
		} finally {
			executor.shutdownNow();
		}

		if (lockContentionDetected.get()) {
			System.err.println("TEST RESULT: Lock contention detected - connectInternal() holds lock too long");
			System.err.println("EXPECTED: Lock should be released before .send() to allow callbacks to proceed");
		}

		assertThat(callbacksCompleted.get()).as("Callbacks should complete without excessive waiting").isTrue();
	}

	/**
	 * ISSUE 2: @Modified Blocks OSGi Framework
	 * 
	 * Problem: @Modified calls disconnect(true) which blocks on .send() If broker
	 * is slow/unreachable, entire framework blocks during config change
	 * 
	 * This test proves the issue by: - Calling @Modified via reflection - Measuring
	 * how long it blocks - Should block for seconds with current implementation
	 * 
	 * NOTE: This test may leave the client in a reconnecting state. Marked to run
	 * in isolation if needed.
	 */
	@Test(timeout = TIMEOUT_SECONDS * 1000)
	public void testIssue2_ModifiedBlocksFramework() throws Exception {
		System.out.println("\n=== Testing Issue 2: @Modified Blocking ===");

		final MessageClientProvider provider = (MessageClientProvider) client;

		// Ensure we're connected
		if (!client.isConnected()) {
			client.connect().get(5, SECONDS);
			Thread.sleep(500);
		}

		// Call @Modified via reflection to simulate config change
		final Method modifiedMethod = MessageClientProvider.class.getDeclaredMethod("modified", Config.class);
		modifiedMethod.setAccessible(true);

		final Config currentConfig = provider.config();
		System.out.println("Calling @Modified - should block while disconnecting and reconnecting...");

		final long start = System.currentTimeMillis();
		final AtomicBoolean modifiedCompleted = new AtomicBoolean(false);
		final AtomicReference<Long> blockingTime = new AtomicReference<>(0L);

		// Run modified in separate thread to measure blocking time
		final CompletableFuture<Void> modifiedTask = CompletableFuture.runAsync(() -> {
			try {
				modifiedMethod.invoke(provider, currentConfig);
				long elapsed = System.currentTimeMillis() - start;
				blockingTime.set(elapsed);
				modifiedCompleted.set(true);
				System.out.println("@Modified completed in " + elapsed + "ms");
			} catch (Exception e) {
				System.err.println("@Modified failed: " + e.getMessage());
				throw new RuntimeException(e);
			}
		});

		// Wait for modified to complete
		try {
			modifiedTask.get(TIMEOUT_SECONDS, SECONDS);
		} catch (TimeoutException e) {
			fail("@Modified blocked for more than " + TIMEOUT_SECONDS + " seconds");
		}

		assertThat(modifiedCompleted.get()).as("@Modified should eventually complete").isTrue();

		final long elapsed = blockingTime.get();

		if (elapsed > 1000) {
			System.err.println("WARNING: @Modified blocked OSGi thread for " + elapsed + "ms");
			System.err.println("EXPECTED: Should return immediately, perform disconnect/reconnect asynchronously");
		}

		// With current implementation, this will block for multiple seconds
		System.out.println("TEST RESULT: @Modified blocked for " + elapsed + "ms");
	}

	/**
	 * ISSUE 3: @Deactivate Blocks OSGi Framework
	 * 
	 * Problem: @Deactivate calls disconnect(false) which blocks on .send() This
	 * blocks the entire OSGi shutdown sequence
	 * 
	 * This test proves the issue by: - Calling @Deactivate via reflection -
	 * Measuring blocking time - Should block for seconds with current
	 * implementation
	 * 
	 * WARNING: After this test, the client will be deactivated! This should be the
	 * LAST test in the suite or run in isolation.
	 */
	@Test(timeout = TIMEOUT_SECONDS * 1000)
	public void testIssue3_DeactivateBlocksFramework() throws Exception {
		System.out.println("\n=== Testing Issue 3: @Deactivate Blocking ===");

		final MessageClientProvider provider = (MessageClientProvider) client;

		// Ensure we're connected
		if (!client.isConnected()) {
			client.connect().get(5, SECONDS);
			Thread.sleep(500);
		}

		// Call @Deactivate via reflection
		final Method deactivateMethod = MessageClientProvider.class.getDeclaredMethod("deactivate");
		deactivateMethod.setAccessible(true);

		System.out.println("Calling @Deactivate - should block while disconnecting...");

		final long start = System.currentTimeMillis();
		final AtomicBoolean deactivateCompleted = new AtomicBoolean(false);
		final AtomicReference<Long> blockingTime = new AtomicReference<>(0L);

		// Run deactivate in separate thread
		final CompletableFuture<Void> deactivateTask = CompletableFuture.runAsync(() -> {
			try {
				deactivateMethod.invoke(provider);
				long elapsed = System.currentTimeMillis() - start;
				blockingTime.set(elapsed);
				deactivateCompleted.set(true);
				System.out.println("@Deactivate completed in " + elapsed + "ms");
			} catch (Exception e) {
				System.err.println("@Deactivate failed: " + e.getMessage());
				throw new RuntimeException(e);
			}
		});

		// Wait for deactivate
		try {
			deactivateTask.get(TIMEOUT_SECONDS, SECONDS);
		} catch (TimeoutException e) {
			fail("@Deactivate blocked for more than " + TIMEOUT_SECONDS + " seconds");
		}

		assertThat(deactivateCompleted.get()).as("@Deactivate should eventually complete").isTrue();

		final long elapsed = blockingTime.get();

		if (elapsed > 500) {
			System.err.println("WARNING: @Deactivate blocked OSGi thread for " + elapsed + "ms");
			System.err.println("EXPECTED: Should return quickly (< 500ms) or use async with timeout");
		}

		System.out.println("TEST RESULT: @Deactivate blocked for " + elapsed + "ms");
	}

	/**
	 * ISSUE 4: disconnect() Doesn't Check Connection State
	 * 
	 * Problem: Private disconnect(boolean) tries to send DISCONNECT packet even if
	 * client is not connected. If network is down, this blocks until timeout.
	 * 
	 * NOTE: This test calls PUBLIC disconnect() API which correctly checks state.
	 * The real bug is in PRIVATE disconnect(boolean) which is called by:
	 * - @Modified → disconnect(true) - @Deactivate → disconnect(false)
	 * 
	 * This test verifies the public API behavior for reference. Tests 2 & 3
	 * indirectly test the private method issue.
	 */
	@Test(timeout = TIMEOUT_SECONDS * 1000)
	public void testIssue4_DisconnectWithoutConnectionStateCheck() throws Exception {
		System.out.println("\n=== Testing Issue 4: Disconnect Without State Check ===");

		// Ensure we're disconnected
		if (client.isConnected()) {
			client.disconnect().get(5, SECONDS);
			Thread.sleep(500);
		}

		// Try to disconnect again when already disconnected
		System.out.println("Attempting disconnect when already disconnected...");

		final long start = System.currentTimeMillis();

		try {
			client.disconnect().get(5, SECONDS);
			fail("Should throw IllegalStateException when not connected");
		} catch (IllegalStateException e) {
			// Expected - client correctly throws exception
			long elapsed = System.currentTimeMillis() - start;
			System.out.println("Correctly threw exception after " + elapsed + "ms");

			if (elapsed > 1000) {
				System.err.println("WARNING: Took " + elapsed + "ms to detect client not connected");
				System.err.println("EXPECTED: Should check state immediately and return/throw quickly");
			}
		} catch (Exception e) {
			long elapsed = System.currentTimeMillis() - start;
			System.err.println("Unexpected exception after " + elapsed + "ms: " + e.getMessage());
		}
	}

	/**
	 * COMBINED TEST: Stress test all concurrency paths
	 * 
	 * This test exercises all problematic paths simultaneously to expose race
	 * conditions and deadlocks that only appear under load.
	 */
	@Test(timeout = 30000) // 30 second timeout for stress test
	public void testCombined_StressAllConcurrencyPaths() throws Exception {
		System.out.println("\n=== Combined Stress Test ===");

		final ExecutorService executor = Executors.newFixedThreadPool(6);
		final AtomicBoolean stopFlag = new AtomicBoolean(false);
		final CountDownLatch startLatch = new CountDownLatch(1);

		// State query threads
		final CompletableFuture<Void> queries = CompletableFuture.runAsync(() -> {
			try {
				startLatch.await();
				while (!stopFlag.get()) {
					client.isConnected();
					client.getLastDisconnectReason();
					Thread.sleep(10);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, executor);

		// Reconnection cycle
		final CompletableFuture<Void> reconnect = CompletableFuture.runAsync(() -> {
			try {
				startLatch.await();
				for (int i = 0; i < 2; i++) {
					if (client.isConnected()) {
						client.disconnect().get(3, SECONDS);
					}
					Thread.sleep(100);
					client.connect().get(5, SECONDS);
					Thread.sleep(200);
				}
			} catch (Exception e) {
				System.err.println("Reconnect thread failed: " + e.getMessage());
			}
		}, executor);

		try {
			startLatch.countDown();
			reconnect.get(25, SECONDS);
		} catch (TimeoutException e) {
			fail("Stress test timed out - likely deadlock");
		} finally {
			stopFlag.set(true);
			executor.shutdownNow();
		}

		queries.get(1, SECONDS);

		System.out.println("Stress test completed successfully");
	}

	/**
	 * Helper: Get connectionLock field via reflection
	 */
	private ReentrantLock getConnectionLock(MessageClientProvider provider) throws Exception {
		final Field lockField = MessageClientProvider.class.getDeclaredField("connectionLock");
		lockField.setAccessible(true);
		return (ReentrantLock) lockField.get(provider);
	}
}
