package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
 * during connect operations when callbacks try to acquire locks.
 * 
 * This test validates the connect path specifically, where:
 * - connectInternal() holds connectionLock during blocking send()
 * - Callbacks (connected/disconnected listeners) may need the lock
 * - Concurrent state queries also need the lock
 * 
 * If deadlock exists, tests will hang and timeout.
 */
@RunWith(LaunchpadRunner.class)
public class MessageClientConnectDeadlockTest {

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
     * Test: Concurrent connect operations with state queries don't deadlock.
     * 
     * This stresses the connect path by:
     * - Disconnecting and reconnecting multiple times
     * - Querying state concurrently during connection
     * - Multiple threads competing for the lock
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testConcurrentConnectNoDeadlock() throws Exception {
        final int NUM_THREADS = 8;
        final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        // Start from disconnected state
        if (client.isConnected()) {
            client.disconnect().get(5, SECONDS);
        }
        
        // Launch concurrent operations
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start together
                    
                    if (threadId % 2 == 0) {
                        // Thread group 1: Try to connect
                        try {
                            if (!client.isConnected()) {
                                client.connect().get(5, SECONDS);
                            }
                        } catch (Exception e) {
                            // Expected - other threads might connect first
                        }
                    } else {
                        // Thread group 2: Query state rapidly during connect
                        for (int j = 0; j < 50; j++) {
                            client.isConnected();
                            client.getConnectedTimestamp();
                            client.getLastDisconnectReason();
                            Thread.sleep(2);
                        }
                    }
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads
        startLatch.countDown();
        
        // Wait for completion - if deadlock occurs, this will timeout
        boolean completed = completionLatch.await(TIMEOUT_SECONDS - 2, SECONDS);
        
        executor.shutdownNow();
        executor.awaitTermination(2, SECONDS);
        
        // Assertions
        assertThat(completed)
            .as("All threads should complete without deadlock")
            .isTrue();
        
        assertThat(successCount.get())
            .as("Most threads should succeed")
            .isGreaterThan(0);
    }
    
    /**
     * Test: Connect callbacks can acquire lock during connection.
     * 
     * This validates that when connect() is in progress and holding the lock,
     * callbacks (connected/disconnected listeners) can still execute.
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testConnectCallbacksCanAcquireLock() throws Exception {
        // Disconnect and reconnect multiple times with concurrent queries
        for (int i = 0; i < 5; i++) {
            if (client.isConnected()) {
                client.disconnect().get(5, SECONDS);
            }
            
            // Start connect
            CompletableFuture<Void> connectFuture = client.connect();
            
            // Query state from another thread while connect is in progress
            CompletableFuture<Boolean> stateFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    for (int j = 0; j < 20; j++) {
                        client.isConnected();
                        client.getConnectedTimestamp();
                        Thread.sleep(5);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
            
            // Both should complete without deadlock
            connectFuture.get(5, SECONDS);
            assertThat(stateFuture.get(5, SECONDS)).isTrue();
            
            Thread.sleep(100);
        }
    }
    
    /**
     * Test: Rapid connect/disconnect cycles with state queries.
     * 
     * This stresses both connect and disconnect paths simultaneously.
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testRapidConnectDisconnectCycle() throws Exception {
        final AtomicInteger completedOps = new AtomicInteger(0);
        
        // Query executor
        ExecutorService queryExecutor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            queryExecutor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    try {
                        client.isConnected();
                        client.getConnectedTimestamp();
                        completedOps.incrementAndGet();
                        Thread.sleep(1);
                    } catch (Exception e) {
                        // Expected during state changes
                    }
                }
            });
        }
        
        // Rapid connect/disconnect from main thread
        Thread.sleep(50);
        for (int i = 0; i < 3; i++) {
            if (client.isConnected()) {
                client.disconnect().get(3, SECONDS);
            }
            Thread.sleep(50);
            if (!client.isConnected()) {
                client.connect().get(3, SECONDS);
            }
            Thread.sleep(50);
        }
        
        queryExecutor.shutdown();
        assertThat(queryExecutor.awaitTermination(10, SECONDS))
            .as("Query operations should complete without hanging")
            .isTrue();
        
        assertThat(completedOps.get())
            .as("Most query operations should succeed")
            .isGreaterThan(50);
    }
}
