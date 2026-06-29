/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttClient;

/**
 * Integration test to validate that MessageClientProvider does not leak threads
 * when repeatedly connecting and disconnecting with a custom executor.
 */
@RunWith(LaunchpadRunner.class)
public class MessageClientThreadLeakTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MqttClient client;

    @Service
    private ConfigurationAdmin configAdmin;

    @SuppressWarnings("resource")
    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Before
    public void setup() throws Exception {
        // Configure the client to use a custom executor (which triggers the local thread pool creation)
        final Configuration config = configAdmin.getConfiguration("in.bytehue.messaging.client", "?");
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("server", "localhost");
        props.put("useCustomExecutor", true);
        props.put("numberOfThreads", 2); // Small thread pool
        props.put("threadNamePrefix", "mqtt-leak-test");
        config.updateIfDifferent(props);

        waitForMqttConnectionReady(launchpad);
    }

    @Test
    public void testRepeatedConnectDisconnectDoesNotLeakThreads() throws Exception {
        // Verify initial state
        assertThat(client.isConnected()).isTrue();

        // Perform 10 iterations of connect/disconnect
        final int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            client.disconnect().join();
            assertThat(client.isConnected()).isFalse();

            client.connect().join();
            assertThat(client.isConnected()).isTrue();
        }

        // Wait a short moment to ensure threads from the old executors have time to spin down
        Thread.sleep(1000);

        // Count how many "mqtt-leak-test" threads are currently alive
        long leakTestThreadCount = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().startsWith("mqtt-leak-test"))
                .filter(Thread::isAlive)
                .count();

        // If the leak is present, there will be 10+ threads alive because every disconnect
        // fails to kill the Netty EventLoop.
        // If the fix is active, there should be exactly the number of threads from the CURRENT connection (<= 2).
        assertThat(leakTestThreadCount).isLessThanOrEqualTo(2);
    }
}
