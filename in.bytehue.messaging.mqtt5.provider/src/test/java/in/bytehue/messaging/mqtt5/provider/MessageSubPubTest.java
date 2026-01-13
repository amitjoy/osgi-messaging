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
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForRequestProcessing;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Features;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageSubPubTest {

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
	public void test_sub_pub_with_1() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String channel = "ab/ba";
		final String payload = "abc";
		final String contentType = "text/plain";

		// @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		subscriber.subscribe(channel).forEach(m -> {
			final String topic = m.getContext().getChannel();
			final String ctype = m.getContext().getContentType();
			final String content = new String(m.payload().array(), UTF_8);

			assertThat(channel).isEqualTo(topic);
			assertThat(payload).isEqualTo(content);
			assertThat(contentType).isEqualTo(ctype);

			flag.set(true);
		});
		publisher.publish(message);
		waitForRequestProcessing(flag);
	}

	@Test
	public void test_sub_pub_with_2() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String channel = "ab/ba";
		final String payload = "abc";
		final String contentType = "text/plain";

		// @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		subscriber.subscribe(message.getContext()).forEach(m -> {
			final String topic = m.getContext().getChannel();
			final String ctype = m.getContext().getContentType();
			final String content = new String(m.payload().array(), UTF_8);

			assertThat(channel).isEqualTo(topic);
			assertThat(payload).isEqualTo(content);
			assertThat(contentType).isEqualTo(ctype);

			flag.set(true);
		});
		publisher.publish(message);
		waitForRequestProcessing(flag);
	}

	@Test
	public void test_sub_pub_with_3() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String channel = "ab/ba";
		final String inputChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

		subscriber.subscribe(inputChannel).forEach(m -> {
			final String topic = m.getContext().getChannel();
			final String ctype = m.getContext().getContentType();
			final String content = new String(m.payload().array(), UTF_8);

			assertThat(inputChannel).isEqualTo(topic);
			assertThat(payload).isEqualTo(content);
			assertThat(contentType).isEqualTo(ctype);

			flag.set(true);
		});
		// inputChannel has higher priority over message.getContext().getChannel()
		publisher.publish(message, inputChannel);
		waitForRequestProcessing(flag);
	}

	@Test
	public void test_sub_pub_with_4() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String channel = "ab/ba";
		final String inputChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();

        final MessageContext messageContext = mcb.channel(inputChannel)
                .buildContext();
        // @formatter:on

		subscriber.subscribe(inputChannel).forEach(m -> {
			final String topic = m.getContext().getChannel();
			final String ctype = m.getContext().getContentType();
			final String content = new String(m.payload().array(), UTF_8);

			assertThat(inputChannel).isEqualTo(topic);
			assertThat(payload).isEqualTo(content);
			assertThat(contentType).isEqualTo(ctype);

			flag.set(true);
		});
		// messageContext has higher priority over message.getContext().getChannel()
		publisher.publish(message, messageContext);
		waitForRequestProcessing(flag);
	}

	@Test
	public void test_sub_pub_extensions_guaranteedDelivery() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String channel = "ab/ba";
		final String inputChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final Map<String, Object> extensions = new HashMap<>();
		extensions.put(Features.EXTENSION_GUARANTEED_DELIVERY, true);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .extensions(extensions)
                .buildMessage();

        final MessageContext messageContext = mcb.channel(inputChannel).extensions(extensions)
                .buildContext();
        // @formatter:on

		subscriber.subscribe(messageContext).forEach(m -> {
			final MessageContext context = m.getContext();
			final String topic = context.getChannel();
			final String ctype = context.getContentType();
			final String content = new String(m.payload().array(), UTF_8);

			final Map<String, Object> ext = context.getExtensions();

			assertThat(2).isEqualTo(ext.get(Features.EXTENSION_QOS));
			assertThat(inputChannel).isEqualTo(topic);
			assertThat(payload).isEqualTo(content);
			assertThat(contentType).isEqualTo(ctype);

			flag.set(true);
		});
		// messageContext has higher priority over message.getContext().getChannel()
		publisher.publish(message, messageContext);
		waitForRequestProcessing(flag);
	}

	@Test
	public void test_sub_pub_extensions_guaranteedOrdering() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String channel = "ab/ba";
		final String inputChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final Map<String, Object> extensions = new HashMap<>();
		extensions.put(Features.EXTENSION_GUARANTEED_ORDERING, true);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();

        final MessageContext messageContext = mcb.channel(inputChannel).extensions(extensions)
                .buildContext();
        // @formatter:on

		subscriber.subscribe(messageContext).forEach(m -> {
			final MessageContext context = m.getContext();
			final String topic = context.getChannel();
			final String ctype = context.getContentType();
			final String content = new String(m.payload().array(), UTF_8);

			final Map<String, Object> ext = context.getExtensions();

			assertThat(2).isEqualTo(ext.get(Features.EXTENSION_QOS));
			assertThat(inputChannel).isEqualTo(topic);
			assertThat(payload).isEqualTo(content);
			assertThat(contentType).isEqualTo(ctype);

			flag.set(true);
		});
		// messageContext has higher priority over message.getContext().getChannel()
		publisher.publish(message, messageContext);
		waitForRequestProcessing(flag);
	}

	@Test
	public void test_correlation_id() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final String requestTopic = "a/b/request";
		final String responseTopicBase = "a/b/c/";
		final String correlationId = UUID.randomUUID().toString();
		final String payload = "some-payload-data-that-is-long-enough-to-potentially-trigger-buffer-issues";

		// 1. Setup Responder
		subscriber.subscribe(requestTopic).forEach(m -> {
			final String receivedCorrelationId = m.getContext().getCorrelationId();
			// If extraction is buggy, this topic will have garbage characters
			final String responseTopic = responseTopicBase + receivedCorrelationId;
			final Message response = mcb.channel(responseTopic).content(ByteBuffer.wrap("response".getBytes()))
					.buildMessage();
			publisher.publish(response);
		});

		// 2. Setup Requestor
		subscriber.subscribe(responseTopicBase + "#").forEach(m -> {
			final String topic = m.getContext().getChannel();
			// Verify the topic matches exactly what we expect (no extra nulls/garbage)
			if (topic.equals(responseTopicBase + correlationId)) {
				flag.set(true);
			}
		});

		// 3. Publish Request
		final Message request = mcb.channel(requestTopic).correlationId(correlationId)
				.content(ByteBuffer.wrap(payload.getBytes())).buildMessage();
		publisher.publish(request);

		waitForRequestProcessing(flag);
	}

}
