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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttClient;

@RunWith(LaunchpadRunner.class)
public final class MessageClientLifecycleTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MqttClient client;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_client_initial_state_and_uptime() {
		assertThat(client).isNotNull();
		assertThat(client.isConnected()).isTrue();
		assertThat(client.getConnectedTimestamp()).isGreaterThan(0L);
		assertThat(client.getLastDisconnectReason()).isNull();
	}

	@Test
	public void test_client_disconnect_lifecycle() throws Exception {
		assertThat(client.isConnected()).isTrue();

		client.disconnect().get(5, SECONDS);

		await().atMost(5, SECONDS).until(() -> !client.isConnected());
		assertThat(client.isConnected()).isFalse();
		assertThat(client.getConnectedTimestamp()).isEqualTo(-1L);
		assertThat(client.getLastDisconnectReason()).isNotNull();
	}

	@Test
	public void test_reconnect_and_double_connect_error() throws Exception {
		// When already connected, calling connect() should throw IllegalStateException
		assertThatThrownBy(() -> client.connect())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already connected");

		// Username without password should throw IllegalArgumentException
		assertThatThrownBy(() -> client.connect("admin", null))
				.isInstanceOf(IllegalArgumentException.class);

		// Disconnect first
		client.disconnect().get(5, SECONDS);
		await().atMost(5, SECONDS).until(() -> !client.isConnected());

		// Reconnect should succeed
		client.connect().get(5, SECONDS);
		await().atMost(5, SECONDS).until(() -> client.isConnected());
		assertThat(client.isConnected()).isTrue();
		assertThat(client.getConnectedTimestamp()).isGreaterThan(0L);
	}

}
