/*******************************************************************************
 * Copyright 2020-2024 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.EXTENSION_AUTO_ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.EXTENSION_LAST_WILL;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.MESSAGE_CONTEXT_BUILDER;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttMessageConstants;

@RunWith(LaunchpadRunner.class)
public final class ServicePropertyExistenceTest {

	@Service
	private Launchpad launchpad;

	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_message_context_builder() {
		final String propertyKey1 = "osgi.messaging.name";
		final String propertyKey2 = "osgi.messaging.protocol";
		final String propertyKey3 = "osgi.messaging.feature";

		final String propertyValue1 = MqttMessageConstants.MESSAGING_ID;
		final String[] propertyValue2 = new String[] { MqttMessageConstants.MESSAGING_PROTOCOL };
		final String[] propertyValue3 = new String[] { MESSAGE_CONTEXT_BUILDER, ACKNOWLEDGE };

		// @formatter:off
        final Optional<ServiceReference<MessageContextBuilder>> ref =
                launchpad.waitForServiceReference(MessageContextBuilder.class, 10_000L);
        // @formatter:on

		if (ref.isPresent()) {
			final Dictionary<String, Object> properties = ref.get().getProperties();

			final Map<String, Object> check = new HashMap<>();
			check.put(propertyKey1, propertyValue1);
			check.put(propertyKey2, propertyValue2);
			check.put(propertyKey3, propertyValue3);

			assertThat(TestHelper.toMap(properties)).containsAllEntriesOf(check);
			return;
		}
		throw new AssertionError("Will never reach");
	}

	@Test
	public void test_message_publisher() {
		final String propertyKey1 = "osgi.messaging.name";
		final String propertyKey2 = "osgi.messaging.protocol";
		final String propertyKey3 = "osgi.messaging.feature";

		final String propertyValue1 = MqttMessageConstants.MESSAGING_ID;
		final String[] propertyValue2 = new String[] { MqttMessageConstants.MESSAGING_PROTOCOL };
		// @formatter:off
        final String[] propertyValue3 = new String[] {
                RETAIN,
                EXTENSION_QOS,
                USER_PROPERTIES,
                MESSAGE_EXPIRY_INTERVAL,
                EXTENSION_GUARANTEED_DELIVERY,
                EXTENSION_GUARANTEED_ORDERING };

        final Optional<ServiceReference<MessagePublisher>> ref =
                launchpad.waitForServiceReference(MessagePublisher.class, 10_000L);
        // @formatter:on

		if (ref.isPresent()) {
			final Dictionary<String, Object> properties = ref.get().getProperties();

			final Map<String, Object> check = new HashMap<>();
			check.put(propertyKey1, propertyValue1);
			check.put(propertyKey2, propertyValue2);
			check.put(propertyKey3, propertyValue3);

			assertThat(TestHelper.toMap(properties)).containsAllEntriesOf(check);
			return;
		}
		throw new AssertionError("Will never reach");
	}

	@Test
	public void test_message_reply_to_publisher() {
		final String propertyKey1 = "osgi.messaging.name";
		final String propertyKey2 = "osgi.messaging.protocol";
		final String propertyKey3 = "osgi.messaging.feature";

		final String propertyValue1 = MqttMessageConstants.MESSAGING_ID;
		final String[] propertyValue2 = new String[] { MqttMessageConstants.MESSAGING_PROTOCOL };
		// @formatter:off
        final String[] propertyValue3 = new String[] {
                REPLY_TO,
                REPLY_TO_MANY_PUBLISH,
                REPLY_TO_MANY_SUBSCRIBE,
                GENERATE_CORRELATION_ID,
                GENERATE_REPLY_CHANNEL };

        final Optional<ServiceReference<ReplyToPublisher>> ref =
                launchpad.waitForServiceReference(ReplyToPublisher.class, 10_000L);
        // @formatter:on

		if (ref.isPresent()) {
			final Dictionary<String, Object> properties = ref.get().getProperties();

			final Map<String, Object> check = new HashMap<>();
			check.put(propertyKey1, propertyValue1);
			check.put(propertyKey2, propertyValue2);
			check.put(propertyKey3, propertyValue3);

			assertThat(TestHelper.toMap(properties)).containsAllEntriesOf(check);
			return;
		}
		throw new AssertionError("Will never reach");
	}

	@Test
	public void test_message_service_runtime() {
		final String propertyKey1 = "osgi.messaging.name";
		final String propertyKey2 = "osgi.messaging.protocol";
		final String propertyKey3 = "osgi.messaging.feature";

		final String propertyValue1 = MqttMessageConstants.MESSAGING_ID;
		final String[] propertyValue2 = new String[] { MqttMessageConstants.MESSAGING_PROTOCOL };
		// @formatter:off
        final String[] propertyValue3 = new String[] {
                RETAIN,
                REPLY_TO,
                ACKNOWLEDGE,
                EXTENSION_QOS,
                RECEIVE_LOCAL,
                USER_PROPERTIES,
                EXTENSION_LAST_WILL,
                REPLY_TO_MANY_PUBLISH,
                GENERATE_REPLY_CHANNEL,
                MESSAGE_EXPIRY_INTERVAL,
                MESSAGE_CONTEXT_BUILDER,
                REPLY_TO_MANY_SUBSCRIBE,
                GENERATE_CORRELATION_ID,
                EXTENSION_AUTO_ACKNOWLEDGE,
                EXTENSION_GUARANTEED_DELIVERY,
                EXTENSION_GUARANTEED_ORDERING };

        final Optional<ServiceReference<MessageServiceRuntime>> ref =
                launchpad.waitForServiceReference(MessageServiceRuntime.class, 10_000L);
        // @formatter:on

		if (ref.isPresent()) {
			final Dictionary<String, Object> properties = ref.get().getProperties();

			final Map<String, Object> check = new HashMap<>();
			check.put(propertyKey1, propertyValue1);
			check.put(propertyKey2, propertyValue2);
			check.put(propertyKey3, propertyValue3);

			assertThat(TestHelper.toMap(properties)).containsAllEntriesOf(check);
			return;
		}
		throw new AssertionError("Will never reach");
	}

	@Test
	public void test_message_subscription() {
		final String propertyKey1 = "osgi.messaging.name";
		final String propertyKey2 = "osgi.messaging.protocol";
		final String propertyKey3 = "osgi.messaging.feature";

		final String propertyValue1 = MqttMessageConstants.MESSAGING_ID;
		final String[] propertyValue2 = new String[] { MqttMessageConstants.MESSAGING_PROTOCOL };
		// @formatter:off
        final String[] propertyValue3 = new String[] {
                EXTENSION_QOS,
                RETAIN,
                ACKNOWLEDGE,
                RECEIVE_LOCAL };

        final Optional<ServiceReference<MessageSubscription>> ref =
                launchpad.waitForServiceReference(MessageSubscription.class, 10_000L);
        // @formatter:on

		if (ref.isPresent()) {
			final Dictionary<String, Object> properties = ref.get().getProperties();

			final Map<String, Object> check = new HashMap<>();
			check.put(propertyKey1, propertyValue1);
			check.put(propertyKey2, propertyValue2);
			check.put(propertyKey3, propertyValue3);

			assertThat(TestHelper.toMap(properties)).containsAllEntriesOf(check);
			return;
		}
		throw new AssertionError("Will never reach");
	}

}
