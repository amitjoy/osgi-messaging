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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CLIENT;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.addTopicPrefix;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageTopicPrefixTest {

	@Service
	private Launchpad launchpad;

	@Service
	private ConfigurationAdmin configAdmin;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageSubscription subscriber;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_add_topic_prefix_helper() {
		assertThat(addTopicPrefix(null, "prefix")).isNull();
		assertThat(addTopicPrefix("", "prefix")).isNull();
		assertThat(addTopicPrefix("  ", "prefix")).isNull();
		assertThat(addTopicPrefix("topic", null)).isEqualTo("topic");
		assertThat(addTopicPrefix("topic", "")).isEqualTo("topic");
		assertThat(addTopicPrefix("topic", "  ")).isEqualTo("topic");
		assertThat(addTopicPrefix("topic", "prefix")).isEqualTo("prefix/topic");
		assertThat(addTopicPrefix("a/b/c", "root/sub")).isEqualTo("root/sub/a/b/c");
	}

	@Test
	public void test_dynamic_topic_prefix_configuration() throws Exception {
		final Configuration config = configAdmin.getConfiguration(CLIENT, "?");

		final Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("topicPrefix", "iot/building1");
		config.update(properties);

		// Wait for configuration update and client reconnection
		waitForMqttConnectionReady(launchpad);

		final String subChannel = "sensors/temperature";
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean received = new AtomicBoolean(false);

		subscriber.subscribe(subChannel).forEach(msg -> {
			received.set(true);
			latch.countDown();
		});

		final MessageContextBuilder mcb = launchpad.getService(MessageContextBuilder.class).get();
		final Message message = mcb.channel(subChannel)
		                           .content(ByteBuffer.wrap("22.5C".getBytes()))
		                           .buildMessage();

		publisher.publish(message);

		final boolean completed = latch.await(10, SECONDS);
		assertThat(completed).isTrue();
		assertThat(received.get()).isTrue();

		// Clean up configuration
		final Dictionary<String, Object> resetProperties = new Hashtable<>();
		resetProperties.put("topicPrefix", "");
		config.update(resetProperties);
		waitForMqttConnectionReady(launchpad);
	}

}
