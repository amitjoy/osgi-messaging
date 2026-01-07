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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToSingleHandlerTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessageContextBuilder mcb;

	@Service
	private MessagePublisher publisher;

	@Service
	private ReplyToPublisher replyToPublisher;

	@Service
	private MessageSubscription subscriber;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_reply_to_single_subscription_handler_1() throws Exception {
		final AtomicBoolean flag1 = new AtomicBoolean();
		final AtomicBoolean flag2 = new AtomicBoolean();

		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			// @formatter:off
            final Message message = mcb.contentType(contentType)
                                       .content(ByteBuffer.wrap(payload.getBytes()))
                                       .buildMessage();
            // @formatter:on
			flag1.set(true);
			return message;
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> flag2.set(true));

		publisher.publish(message);
		waitForRequestProcessing(flag1);
		waitForRequestProcessing(flag2);
	}

	@Test
	public void test_reply_to_single_subscription_handler_2() throws Exception {
		final AtomicBoolean flag1 = new AtomicBoolean();
		final AtomicBoolean flag2 = new AtomicBoolean();

		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			// @formatter:off
            final Message message = mcb.contentType(contentType)
                                       .content(ByteBuffer.wrap(payload.getBytes()))
                                       .buildMessage();
            // @formatter:on
			flag1.set(true);
			return message;
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel }; // subscribe

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String[] replyToChannelValue = { replyToChannel }; // publish

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel) // publish
                                   .replyTo(replyToChannel) // subscribe
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		replyToPublisher.publishWithReply(message).onSuccess(m -> flag2.set(true));

		waitForRequestProcessing(flag1);
		waitForRequestProcessing(flag2);
	}

	@Test
	public void test_reply_to_single_subscription_handler_3() throws Exception {
		final AtomicBoolean flag1 = new AtomicBoolean();
		final AtomicBoolean flag2 = new AtomicBoolean();

		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			// @formatter:off
			final Message message = mcb.contentType(contentType)
					.content(ByteBuffer.wrap(payload.getBytes()))
					.buildMessage();
			// @formatter:on
			flag1.set(true);
			return message;
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel }; // subscribe

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue);

		// @formatter:off
		final Message message = mcb.channel(channel) // publish
				.replyTo(replyToChannel) // subscribe
				.contentType(contentType)
				.content(ByteBuffer.wrap(payload.getBytes()))
				.buildMessage();
		// @formatter:on

		replyToPublisher.publishWithReply(message).onSuccess(m -> flag2.set(true));

		waitForRequestProcessing(flag1);
		waitForRequestProcessing(flag2);
	}

	@Test
	public void test_reply_to_single_subscription_handler_without_protocol_in_target_key() throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_without_messaging_name_in_target_key() throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_without_messaging_feature_in_target_key() throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_with_different_feature_in_target_key() throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=abc))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_with_different_messaging_name_in_target_key()
			throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=blahblah)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_with_different_protocol_name_in_target_key()
			throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=amqp)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue, replyToChannelKey, replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_without_target_key() throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, channelKey, channelValue, replyToChannelKey,
				replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_without_channel_key() throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
		final String replyToChannelValue = replyToChannel;

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, replyToChannelKey,
				replyToChannelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

	@Test
	public void test_reply_to_single_subscription_handler_without_replyto_channel_key() throws Exception {
		final String channel = "a/b";
		final String replyToChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			throw new AssertionError("Will never be executed");
		};
		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String[] channelValue = { channel };

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue);

		// @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

		subscriber.subscribe(replyToChannel).forEach(m -> {
			throw new AssertionError("Will never be executed");
		});

		publisher.publish(message);
	}

}
