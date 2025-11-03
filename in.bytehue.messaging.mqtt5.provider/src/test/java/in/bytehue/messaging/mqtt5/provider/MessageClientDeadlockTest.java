package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
 * when disconnect() is called concurrently with callbacks.
 * 
 * This test uses a real MQTT broker connection to validate:
 * 1. Concurrent disconnect operations complete successfully
 * 2. Callbacks can acquire locks while disconnect is in progress
 * 3. Operations complete within reasonable timeframes (no hangs)
 * 
 * Test Scenario:
 * - Multiple threads call disconnect() simultaneously
 * - Callbacks fire during disconnection
 * - Other threads query connection state
 * - All operations should complete without deadlock
 * 
 * NOTE: Requires MQTT broker running at localhost:1883
 *       Use: docker run -p 1883:1883 eclipse-mosquitto:latest
 */
@RunWith(LaunchpadRunner.class)
public class MessageClientDeadlockTest {

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
     * Test: Concurrent disconnect operations don't deadlock.
     * 
     * This test validates that when multiple threads try to disconnect
     * simultaneously while callbacks are firing, no deadlock occurs.
     * 
     * If the deadlock exists, this test will hang and timeout.
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testConcurrentDisconnectNoDeadlock() throws Exception {
        // Client is already injected and connected via setup()
        assertThat(client.isConnected()).isTrue();
        
        final int NUM_THREADS = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        // Launch concurrent operations
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start together
                    
                    if (threadId % 3 == 0) {
                        // Thread group 1: Try to disconnect
                        try {
                            if (client.isConnected()) {
                                client.disconnect().get(3, SECONDS);
                            }
                        } catch (Exception e) {
                            // Expected - other threads might disconnect first
                        }
                    } else if (threadId % 3 == 1) {
                        // Thread group 2: Query connection state
                        for (int j = 0; j < 100; j++) {
                            client.isConnected();
                            client.getConnectedTimestamp();
                            Thread.sleep(1);
                        }
                    } else {
                        // Thread group 3: Try to connect/disconnect rapidly
                        try {
                            if (!client.isConnected()) {
                                client.connect().get(2, SECONDS);
                            }
                            Thread.sleep(10);
                            if (client.isConnected()) {
                                client.disconnect().get(2, SECONDS);
                            }
                        } catch (Exception e) {
                            // Expected - concurrent modifications
                        }
                    }
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion - if deadlock occurs, this will timeout
        boolean completed = completionLatch.await(TIMEOUT_SECONDS - 2, TimeUnit.SECONDS);
        
        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        
        // Assertions
        assertThat(completed)
            .as("All threads should complete without deadlock")
            .isTrue();
        
        assertThat(successCount.get())
            .as("At least some threads should succeed")
            .isGreaterThan(0);
        
        System.out.println("Deadlock test completed successfully:");
        System.out.println("  Threads completed: " + successCount.get() + "/" + NUM_THREADS);
        System.out.println("  Errors (expected): " + errorCount.get());
    }
    
    /**
     * Test: Disconnect callback can acquire lock while disconnect is in progress.
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testDisconnectCallbacksCanAcquireLock() throws Exception {
        // Query state concurrently with disconnect
        for (int i = 0; i < 5; i++) {
            if (client.isConnected()) {
                // Start disconnect
                CompletableFuture<Void> disconnectFuture = client.disconnect();
                
                // Query state from another thread while disconnect is in progress
                CompletableFuture<Boolean> stateFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        client.getConnectedTimestamp();
                        client.getLastDisconnectReason();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
                
                // Both should complete without deadlock
                disconnectFuture.get(5, SECONDS);
                assertThat(stateFuture.get(5, SECONDS)).isTrue();
                
                // Reconnect for next iteration
                client.connect().get(5, SECONDS);
                Thread.sleep(100);
            }
        }
    }
    
    /**
     * Test: Query operations complete even during disconnect.
     */
    @Test(timeout = TIMEOUT_SECONDS * 1000)
    public void testQueryDuringDisconnect() throws Exception {
        final AtomicInteger completedOps = new AtomicInteger(0);
        
        // Stress test: rapid queries while disconnecting
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
                        // Expected during reconnection
                    }
                }
            });
        }
        
        // Disconnect while queries are running
        Thread.sleep(50);
        if (client.isConnected()) {
            client.disconnect().get(5, SECONDS);
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
