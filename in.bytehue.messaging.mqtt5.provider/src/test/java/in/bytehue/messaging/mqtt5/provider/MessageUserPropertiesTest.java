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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForRequestProcessing;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageUserPropertiesTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageSubscription subscriber;

	@Service
	private MessageContextBuilder mcb;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_single_user_property_roundtrip() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<Map<String, String>> receivedProps = new AtomicReference<>();

		final String channel = "test/userprops/single/" + UUID.randomUUID().toString();
		final String payload = "test-payload-single-prop";

		final Map<String, Object> userProps = new HashMap<>();
		userProps.put("trace-id", "txn-1001");

		subscriber.subscribe(channel).forEach(m -> {
			final Object props = m.getContext().getExtensions().get(USER_PROPERTIES);
			if (props instanceof Map) {
				receivedProps.set((Map<String, String>) props);
			}
			flag.set(true);
		});

		final Message message = mcb.channel(channel)
				.extensionEntry(USER_PROPERTIES, userProps)
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		waitForRequestProcessing(flag);

		assertThat(receivedProps.get()).isNotNull();
		assertThat(receivedProps.get().get("trace-id")).isEqualTo("txn-1001");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_multiple_user_properties_roundtrip() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<Map<String, String>> receivedProps = new AtomicReference<>();

		final String channel = "test/userprops/multi/" + UUID.randomUUID().toString();
		final String payload = "test-payload-multi-props";

		final Map<String, Object> userProps = new HashMap<>();
		userProps.put("device-id", "dev-99");
		userProps.put("firmware", "v2.1.0");
		userProps.put("environment", "staging");

		subscriber.subscribe(channel).forEach(m -> {
			final Object props = m.getContext().getExtensions().get(USER_PROPERTIES);
			if (props instanceof Map) {
				receivedProps.set((Map<String, String>) props);
			}
			flag.set(true);
		});

		final Message message = mcb.channel(channel)
				.extensionEntry(USER_PROPERTIES, userProps)
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		waitForRequestProcessing(flag);

		assertThat(receivedProps.get()).isNotNull();
		assertThat(receivedProps.get().get("device-id")).isEqualTo("dev-99");
		assertThat(receivedProps.get().get("firmware")).isEqualTo("v2.1.0");
		assertThat(receivedProps.get().get("environment")).isEqualTo("staging");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_user_properties_with_special_and_utf8_characters() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<Map<String, String>> receivedProps = new AtomicReference<>();

		final String channel = "test/userprops/utf8/" + UUID.randomUUID().toString();
		final String payload = "payload-with-special-props";

		final Map<String, Object> userProps = new HashMap<>();
		userProps.put("city/location", "Zürich-München");
		userProps.put("metric:key#1", "value with spaces & symbols!");
		userProps.put("greeting", "こんにちは");

		subscriber.subscribe(channel).forEach(m -> {
			final Object props = m.getContext().getExtensions().get(USER_PROPERTIES);
			if (props instanceof Map) {
				receivedProps.set((Map<String, String>) props);
			}
			flag.set(true);
		});

		final Message message = mcb.channel(channel)
				.extensionEntry(USER_PROPERTIES, userProps)
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		waitForRequestProcessing(flag);

		assertThat(receivedProps.get()).isNotNull();
		assertThat(receivedProps.get().get("city/location")).isEqualTo("Zürich-München");
		assertThat(receivedProps.get().get("metric:key#1")).isEqualTo("value with spaces & symbols!");
		assertThat(receivedProps.get().get("greeting")).isEqualTo("こんにちは");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_empty_user_properties_map() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<Map<String, String>> receivedProps = new AtomicReference<>();

		final String channel = "test/userprops/empty/" + UUID.randomUUID().toString();
		final String payload = "payload-empty-props";

		subscriber.subscribe(channel).forEach(m -> {
			final Object props = m.getContext().getExtensions().get(USER_PROPERTIES);
			if (props instanceof Map) {
				receivedProps.set((Map<String, String>) props);
			}
			flag.set(true);
		});

		final Message message = mcb.channel(channel)
				.extensionEntry(USER_PROPERTIES, new HashMap<>())
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		waitForRequestProcessing(flag);

		assertThat(receivedProps.get()).isNotNull().isEmpty();
	}

}
