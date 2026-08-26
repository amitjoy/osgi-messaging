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
import static org.awaitility.Awaitility.await;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.condition.Condition;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.provider.command.MqttCommand;

@RunWith(LaunchpadRunner.class)
public final class MqttCommandTest {

	@Service
	private Launchpad launchpad;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_mqtt_command_activation_with_gogo_condition() {
		// Before condition is registered, MqttCommand may not be active if gogo bundle is absent
		launchpad.register(Condition.class, Condition.INSTANCE, "osgi.condition.id", "gogo-available");

		await().atMost(5, SECONDS).until(() -> launchpad.getService(MqttCommand.class).isPresent());

		final Optional<MqttCommand> command = launchpad.getService(MqttCommand.class);
		assertThat(command).isPresent();
	}

	@Test
	public void test_mqtt_command_runtime_output_table() {
		launchpad.register(Condition.class, Condition.INSTANCE, "osgi.condition.id", "gogo-available");
		await().atMost(5, SECONDS).until(() -> launchpad.getService(MqttCommand.class).isPresent());

		final MqttCommand command = launchpad.getService(MqttCommand.class).get();
		final String output = command.runtime();

		assertThat(output).isNotNull();
		assertThat(output).contains("Connection URI");
		assertThat(output).contains("localhost");
		assertThat(output).contains("Connection State");
		assertThat(output).contains("CONNECTED");
		assertThat(output).contains("Provider");
		assertThat(output).contains("Supported Protocols");
		assertThat(output).contains("Subscriptions:");
		assertThat(output).contains("ReplyTo Subscriptions:");
	}

	@Test
	public void test_mqtt_command_runtime_config_subcommands() {
		launchpad.register(Condition.class, Condition.INSTANCE, "osgi.condition.id", "gogo-available");
		await().atMost(5, SECONDS).until(() -> launchpad.getService(MqttCommand.class).isPresent());

		final MqttCommand command = launchpad.getService(MqttCommand.class).get();

		final String clientConfig = command.runtime("config", "client");
		assertThat(clientConfig).contains("Client Configuration:");
		assertThat(clientConfig).contains("Server Host Address");

		final String pubConfig = command.runtime("config", "pub");
		assertThat(pubConfig).contains("Publisher Configuration:");

		final String subConfig = command.runtime("config", "sub");
		assertThat(subConfig).contains("Subscriber Configuration:");

		final String replyToPubConfig = command.runtime("config", "replytopub");
		assertThat(replyToPubConfig).contains("Reply-To Publisher Configuration:");
	}

}
