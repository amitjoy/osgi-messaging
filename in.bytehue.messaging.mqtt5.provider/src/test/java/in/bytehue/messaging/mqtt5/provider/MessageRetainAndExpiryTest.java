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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.clearRetainedMessage;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForRequestProcessing;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
import in.bytehue.messaging.mqtt5.provider.helper.MessageHelper;

@RunWith(LaunchpadRunner.class)
public final class MessageRetainAndExpiryTest {

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
	public void test_retained_message_delivered_to_late_subscriber() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<String> receivedPayload = new AtomicReference<>();

		final String channel = "test/retained/" + UUID.randomUUID().toString();
		final String payload = "retained-content";

		// 1. Publish retained message BEFORE subscribing
		final Message message = mcb.channel(channel)
				.extensionEntry(RETAIN, true)
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		TimeUnit.MILLISECONDS.sleep(500);

		try {
			// 2. Late subscriber subscribes to channel
			subscriber.subscribe(channel).forEach(m -> {
				final String content = new String(MessageHelper.toByteArray(m.payload()), UTF_8);
				receivedPayload.set(content);
				flag.set(true);
			});

			waitForRequestProcessing(flag);
			assertThat(receivedPayload.get()).isEqualTo(payload);
		} finally {
			clearRetainedMessage(publisher, mcb, channel);
		}
	}

	@Test
	public void test_retained_message_cleared_by_empty_payload() throws Exception {
		final AtomicBoolean flag1 = new AtomicBoolean();
		final AtomicBoolean flag2 = new AtomicBoolean();

		final String channel = "test/retained/clear/" + UUID.randomUUID().toString();
		final String payload = "initial-retained";

		// 1. Publish retained message
		final Message message = mcb.channel(channel)
				.extensionEntry(RETAIN, true)
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		TimeUnit.MILLISECONDS.sleep(500);

		// 2. Clear retained message by publishing empty payload with retain
		clearRetainedMessage(publisher, mcb, channel);
		TimeUnit.MILLISECONDS.sleep(500);

		// 3. Late subscriber should receive the empty clearing message or nothing new
		subscriber.subscribe(channel).forEach(m -> {
			final byte[] bytes = MessageHelper.toByteArray(m.payload());
			if (bytes.length == 0) {
				flag1.set(true);
			} else {
				flag2.set(true);
			}
		});

		TimeUnit.SECONDS.sleep(1);
		// Should not receive the old non-empty retained message
		assertThat(flag2.get()).isFalse();
	}

	@Test
	public void test_retained_flag_preserved_in_extensions() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<Boolean> retainFlagRef = new AtomicReference<>();

		final String channel = "test/retained/flag/" + UUID.randomUUID().toString();
		final String payload = "retained-flag-test";

		final Message message = mcb.channel(channel)
				.extensionEntry(RETAIN, true)
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		TimeUnit.MILLISECONDS.sleep(500);

		try {
			subscriber.subscribe(channel).forEach(m -> {
				final Object isRetained = m.getContext().getExtensions().get(RETAIN);
				if (isRetained instanceof Boolean) {
					retainFlagRef.set((Boolean) isRetained);
				}
				flag.set(true);
			});

			waitForRequestProcessing(flag);
			assertThat(retainFlagRef.get()).isTrue();
		} finally {
			clearRetainedMessage(publisher, mcb, channel);
		}
	}

}
