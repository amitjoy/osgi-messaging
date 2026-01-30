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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;
import org.osgi.service.messaging.replyto.ReplyToPublisher;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToPublisherTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageSubscription subscriber;

	@Service
	private ReplyToPublisher replyToPublisher;

	@Service
	private ReplyToManyPublisher replyToManyPublisher;

	@Service
	private MessageReplyToPublisherProvider provider;

	@Service
	private ConfigurationAdmin configAdmin;

	@Service
	private MessageContextBuilder mcb;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws Exception {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_publish_with_reply_1() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String reqChannel = "a/b";
		final String resChannel = "c/d";
		final String payload = "abc";
		final String correlationId = UUID.randomUUID().toString();

		// @formatter:off
        final Message message = mcb.channel(resChannel)
                                   .replyTo(reqChannel)
                                   .correlationId(correlationId)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        replyToPublisher.publishWithReply(message).onSuccess(m -> flag.set(true));

        final Message reqMessage = mcb.channel(reqChannel)
                                      .correlationId(correlationId)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on

		publisher.publish(reqMessage);

		waitForRequestProcessing(flag);
	}

	@Test
	public void test_publish_with_reply_2() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String reqChannel = "a/b";
		final String resChannel = "c/d";
		final String payload = "abc";
		final String correlationId = UUID.randomUUID().toString();

		// @formatter:off
        final Message message = mcb.channel(resChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        final MessageContext context = mcb.channel(resChannel)
                                          .replyTo(reqChannel)
                                          .correlationId(correlationId)
                                          .buildContext();

        replyToPublisher.publishWithReply(message, context).onSuccess(m -> flag.set(true));

        final Message reqMessage = mcb.channel(reqChannel)
                                      .correlationId(correlationId)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on

		publisher.publish(reqMessage);

		waitForRequestProcessing(flag);
	}

	@Test
	public void test_publish_with_reply_3() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();

		final String reqChannel = "a/b";
		final String resChannel = "c/d";
		final String payload = "abc";
		final String correlationId = UUID.randomUUID().toString();

		// @formatter:off
        final Message message = mcb.channel(resChannel)
                                   .replyTo(reqChannel)
                                   .correlationId(correlationId)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        final Message reqMessage = mcb.channel(reqChannel)
                                      .correlationId(correlationId)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on
		replyToPublisher.publishWithReply(message).onSuccess(m -> flag.set(true));
		publisher.publish(reqMessage);
		waitForRequestProcessing(flag);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_publish_with_reply_missing_reply_channel() throws Exception {
		final String resChannel = "c/d";
		final String payload = "abc";

		// @formatter:off
        final Message message = mcb.channel(resChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		replyToPublisher.publishWithReply(message);
	}

	@Test
	public void test_publish_with_reply_timeout() throws Exception {
		final AtomicBoolean failure = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);

		final String reqChannel = "a/b";
		final String resChannel = "c/d";
		final String payload = "abc";
		final String correlationId = UUID.randomUUID().toString();

		// @formatter:off
        final Message message = mcb.channel(resChannel)
                                   .replyTo(reqChannel)
                                   .correlationId(correlationId)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		// We do not publish the response, so it should timeout
		replyToPublisher.publishWithReply(message).onFailure(e -> {
			failure.set(true);
			latch.countDown();
		});

		latch.await(20, TimeUnit.SECONDS); // Wait longer than the timeout
		assertTrue("Promise should have failed with timeout", failure.get());
	}

	@Test
	public void test_publish_with_reply_auto_correlation_id() throws Exception {
		final AtomicBoolean received = new AtomicBoolean();
		final String reqChannel = "test/auto/cid/req";
		final String resChannel = "test/auto/cid/res";
		final CountDownLatch latch = new CountDownLatch(1);

		// Subscribe to REQUEST channel (where the message is published) to verify CID
		subscriber.subscribe(resChannel).forEach(m -> {
			if (m.getContext().getCorrelationId() != null) {
				received.set(true);
				latch.countDown();
			}
		});

		// Publish without CID
		// @formatter:off
		final Message message = mcb.channel(resChannel)
				                   .replyTo(reqChannel)
				                   .content(ByteBuffer.wrap("abc".getBytes()))
				                   .buildMessage();
		// @formatter:on

		replyToPublisher.publishWithReply(message); // Don't care about result, just the publish

		latch.await(20, TimeUnit.SECONDS);
		assertTrue("Request message should have auto-generated correlation ID", received.get());
	}

}
