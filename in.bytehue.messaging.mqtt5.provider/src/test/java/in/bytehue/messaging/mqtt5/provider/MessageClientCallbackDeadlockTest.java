package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttClient;

/**
 * Integration test to validate that MessageClientProvider does not deadlock
 * when callbacks fire while connectInternal() holds the connectionLock.
 * 
 * The potential deadlock scenario:
 * 1. Thread A: connectInternal() acquires connectionLock
 * 2. Thread A: Calls .send() which may invoke callbacks synchronously
 * 3. Callback: Tries to call registerReadyService() → tries to acquire connectionLock
 * 4. DEADLOCK: Thread A waits for callback, callback waits for lock
 * 
 * This test simulates concurrent access to methods that require the lock
 * during the connection process.
 */
@RunWith(LaunchpadRunner.class)
public class MessageClientCallbackDeadlockTest {

    @Service
    private Launchpad launchpad;
    
    @Service
    private MqttClient client;
    
    @SuppressWarnings("resource")
    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");
    
    private static final int TIMEOUT_SECONDS = 10;
    
    @Before
    public void setup() {
        waitForMqttConnectionReady(launchpad);
    }
    
    /**
     * Test: Verify callbacks can acquire lock during connect operation.
     * 
     * This test simulates what happens if HiveMQ callbacks fire synchronously:
     * - One thread attempts connection
     * - Another thread (simulating callback) tries to query state
     * - If lock is held during .send(), this will deadlock
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testCallbacksDuringConnect() throws Exception {
        // Get the connectionLock via reflection to check if it's held
        final MessageClientProvider provider = (MessageClientProvider) client;
        final ReentrantLock connectionLock = getConnectionLock(provider);
        
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch connectStarted = new CountDownLatch(1);
        final AtomicBoolean callbackCompleted = new AtomicBoolean(false);
        final AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        
        // Start from disconnected state
        if (client.isConnected()) {
            client.disconnect().get(5, SECONDS);
        }
        
        // Thread 1: Perform connection
        final CompletableFuture<Void> connectTask = CompletableFuture.runAsync(() -> {
            try {
                connectStarted.countDown();
                client.connect().get(TIMEOUT_SECONDS, SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Connect failed", e);
            }
        }, executor);
        
        // Thread 2: Simulate callback trying to acquire lock
        final CompletableFuture<Void> callbackTask = CompletableFuture.runAsync(() -> {
            try {
                // Wait for connect to start
                connectStarted.await(2, SECONDS);
                
                // Give connect a moment to acquire the lock
                Thread.sleep(50);
                
                // Try to call methods that need the lock (simulating callback behavior)
                // If connectInternal() holds lock during .send(), this will hang
                for (int i = 0; i < 10; i++) {
                    // These methods need the lock
                    client.getLastDisconnectReason();
                    client.isConnected();
                    
                    // Check if lock is being held for too long
                    if (connectionLock.hasQueuedThreads() && connectionLock.isLocked()) {
                        // Lock is held and threads are waiting - potential deadlock
                        deadlockDetected.set(true);
                    }
                    
                    Thread.sleep(100);
                }
                
                callbackCompleted.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, executor);
        
        // Wait for both tasks to complete
        try {
            CompletableFuture.allOf(connectTask, callbackTask).get(TIMEOUT_SECONDS, SECONDS);
        } catch (TimeoutException e) {
            // If we timeout, it means deadlock occurred
            throw new AssertionError("DEADLOCK DETECTED: Callback thread could not acquire lock during connect", e);
        } finally {
            executor.shutdownNow();
        }
        
        // Verify callback completed without deadlock
        assertThat(callbackCompleted.get())
            .as("Callback should complete without deadlock")
            .isTrue();
        
        if (deadlockDetected.get()) {
            System.out.println("WARNING: Potential deadlock pattern detected - lock held for extended period during connect");
        }
    }
    
    /**
     * Test: Rapid reconnection cycles with concurrent state queries.
     * 
     * This stresses the connection path to expose timing-dependent deadlocks.
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testRapidReconnectWithConcurrentQueries() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(4);
        final AtomicBoolean stopFlag = new AtomicBoolean(false);
        final CountDownLatch startLatch = new CountDownLatch(1);
        
        // Thread pool for state queries
        final CompletableFuture<Void> queryTask = CompletableFuture.runAsync(() -> {
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
        
        // Perform reconnection cycle
        try {
            startLatch.countDown();
            
            for (int i = 0; i < 3; i++) {
                if (client.isConnected()) {
                    client.disconnect().get(3, SECONDS);
                }
                
                client.connect().get(5, SECONDS);
                
                // Wait for connection to be established (async operation)
                // The connect() future completes when connection is initiated, not when connected
                int retries = 0;
                while (!client.isConnected() && retries < 50) {
                    Thread.sleep(100);
                    retries++;
                }
                
                // Verify we're still responsive
                assertThat(client.isConnected())
                    .as("Client should be connected after waiting")
                    .isTrue();
                
                Thread.sleep(100);
            }
        } finally {
            stopFlag.set(true);
            executor.shutdownNow();
        }
        
        queryTask.get(1, SECONDS);
    }
    
    /**
     * Get the connectionLock field via reflection for analysis.
     */
    private ReentrantLock getConnectionLock(MessageClientProvider provider) throws Exception {
        final Field lockField = MessageClientProvider.class.getDeclaredField("connectionLock");
        lockField.setAccessible(true);
        return (ReentrantLock) lockField.get(provider);
    }
}
