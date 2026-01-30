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
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;
import org.osgi.util.pushstream.PushStream;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToManyPublisherTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessagePublisher publisher;

	@Service
	private ReplyToManyPublisher replyToPublisher;

	@Service
	private MqttMessageContextBuilder mcb;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_publish_with_reply_many() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String reqChannel = "a/b";
		final String resChannel = "c/d";
		final String payload = "abc";
		final String stopPayload = "stop";
		final String correlationId = "a";

		// @formatter:off
        final Message message = mcb.channel(resChannel)
                                   .replyTo(reqChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .correlationId(correlationId)
                                   .buildMessage();

        replyToPublisher.publishWithReplyMany(message).forEach(m -> flag.set(true));

        final Message reqMessage = mcb.channel(resChannel)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .correlationId(correlationId)
                                      .buildMessage();

        final MessageProvider stopMessage = new MessageProvider();
        stopMessage.byteBuffer = ByteBuffer.wrap(stopPayload.getBytes());

        final MessageContextProvider messageContext = new MessageContextProvider();
        messageContext.correlationId = correlationId;

		stopMessage.messageContext = messageContext;
        // @formatter:on

		publisher.publish(reqMessage);
		publisher.publish(reqMessage);
		publisher.publish(reqMessage);

		publisher.publish(stopMessage, reqChannel);

		waitForRequestProcessing(flag);
	}

	@Test
	public void test_publish_with_reply_many_with_context() throws Exception {
		final AtomicInteger count = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(3);

		final String reqChannel = "a/b";
		final String resChannel = "a/b/c";
		final String payload = "abc";
		final String correlationId = UUID.randomUUID().toString();

		// @formatter:off
        final Message req = mcb.channel(resChannel)
                                   .replyTo(reqChannel)
                                   .correlationId(correlationId)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        final MessageContext res = mcb.channel(resChannel)
        		                      .replyTo(reqChannel)
        		                      .correlationId(correlationId)
        		                      .content(ByteBuffer.wrap(payload.getBytes()))
        		                      .buildContext();
        // @formatter:on

		final PushStream<Message> stream = replyToPublisher.publishWithReplyMany(req, res);
		stream.forEach(m -> {
			count.incrementAndGet();
			latch.countDown();
		});

		final Message response = mcb.channel(reqChannel).correlationId(correlationId)
				.content(ByteBuffer.wrap(payload.getBytes())).buildMessage();

		// Publish 3 responses
		publisher.publish(response);
		publisher.publish(response);
		publisher.publish(response);

		latch.await(20, TimeUnit.SECONDS);
		assertEquals("Should have received 3 responses", 3, count.get());
		stream.close();
	}

}
