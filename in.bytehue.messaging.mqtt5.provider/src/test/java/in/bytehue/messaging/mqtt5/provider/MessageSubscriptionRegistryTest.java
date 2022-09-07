/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.util.pushstream.PushStream;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageSubscriptionRegistryTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageSubscription subscriber;

	@Service
	private ReplyToPublisher replyToPublisher;

	@Service
	private MessageSubscriptionRegistry registry;

	@Service
	private MessageContextBuilder mcb;

	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_registry_when_subscription_happens() throws Exception {
		final String channel = "ab/ba";
		final PushStream<Message> stream = subscriber.subscribe(channel);
		TimeUnit.SECONDS.sleep(2);

		assertThat(registry.getExistingSubsciption(channel)).isNotNull();
		assertThat(registry.getExistingSubsciption(channel).connectedStream).isEqualTo(stream);

		stream.close();
		TimeUnit.SECONDS.sleep(2);

		assertThat(registry.getExistingSubsciption(channel)).isNull();
	}

	@Test
	public void test_registry_when_unsubscription_happens() throws Exception {
		final String channel = "ab/ba";
		final PushStream<Message> stream = subscriber.subscribe(channel);
		TimeUnit.SECONDS.sleep(2);

		assertThat(registry.getExistingSubsciption(channel)).isNotNull();
		assertThat(registry.getExistingSubsciption(channel).connectedStream).isEqualTo(stream);

		final boolean isRemoved = registry.removeSubscription(channel);
		TimeUnit.SECONDS.sleep(2);

		assertThat(isRemoved).isTrue();
		assertThat(registry.getExistingSubsciption(channel)).isNull();
	}

	@Test
	public void test_registry_when_unsubscription_happens_for_non_existing_subscription() throws Exception {
		final String channel = "ab/ba";
		final boolean isRemoved = registry.removeSubscription(channel);

		TimeUnit.SECONDS.sleep(2);

		assertThat(isRemoved).isFalse();
		assertThat(registry.getExistingSubsciption(channel)).isNull();
	}

	@Test
	public void test_registry_when_existing_subscription_is_unsubscribed() throws Exception {
		final String channel = "ab/ba";
		final PushStream<Message> stream = subscriber.subscribe(channel);
		TimeUnit.SECONDS.sleep(2);

		stream.close();
		TimeUnit.SECONDS.sleep(2);

		assertThat(registry.getExistingSubsciption(channel)).isNull();
	}

	@Test
	public void test_clear_subscriptions() throws Exception {
		final String channel1 = "ab/ba";
		final String channel2 = "ba/ab";

		subscriber.subscribe(channel1);
		subscriber.subscribe(channel2);

		TimeUnit.SECONDS.sleep(2);

		registry.clearAllSubscriptions();

		TimeUnit.SECONDS.sleep(2);

		assertThat(registry.getExistingSubsciption(channel1)).isNull();
		assertThat(registry.getExistingSubsciption(channel2)).isNull();
	}

	@Test
	public void test_all_reply_to_subscriptions_for_req_res_pattern() throws Exception {
		final String reqChannel = "a/b";
		final String resChannel = "c/d";
		final String payload = "abc";
		final String contentType = "text/plain";

		// @formatter:off
        final Message message = mcb.contentType(contentType)
        		                   .channel(resChannel)
        		                   .replyTo(reqChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on
		replyToPublisher.publishWithReply(message);

		TimeUnit.SECONDS.sleep(2);

		assertThat(registry.getSubscriptionDTOs()).isEmpty();
		assertThat(registry.getReplyToSubscriptionDTOs()).isNotNull().hasSize(1);
		assertThat(registry.getExistingSubsciption(reqChannel)).isNotNull();
		assertThat(registry.getExistingSubsciption(reqChannel).pubChannel.name).isEqualTo(resChannel);
		assertThat(registry.getExistingSubsciption(reqChannel).subChannel.name).isEqualTo(reqChannel);
	}

	@Test
	public void test_all_reply_to_subscriptions_for_handler() throws Exception {
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

		assertThat(registry.getReplyToSubscriptionDTOs()).isNotNull();
		assertThat(registry.getReplyToSubscriptionDTOs()[0].requestChannel.name).isEqualTo(channel);
		assertThat(registry.getReplyToSubscriptionDTOs()[0].responseChannel.name).isEqualTo(replyToChannel);
	}

	@Test
	public void test_all_non_reply_to_subscriptions() throws Exception {
		final String channel1 = "ab/ba";
		final String channel2 = "ba/ab";

		subscriber.subscribe(channel1);
		subscriber.subscribe(channel2);

		TimeUnit.SECONDS.sleep(2);

		final SubscriptionDTO[] subscriptionDTOs = registry.getSubscriptionDTOs();

		assertThat(subscriptionDTOs).isNotNull().hasSize(2);
		assertThat(subscriptionDTOs[0].channel.name).isIn(channel1, channel2);
		assertThat(subscriptionDTOs[1].channel.name).isIn(channel1, channel2);
	}

	@Test
	public void test_subscription_of_same_topic_multiple_times() throws Exception {
		final String channel = "ab/ba";
		final PushStream<Message> stream1 = subscriber.subscribe(channel);
		TimeUnit.SECONDS.sleep(2);

		final PushStream<Message> stream2 = subscriber.subscribe(channel);
		TimeUnit.SECONDS.sleep(2);

		assertThat(registry.getExistingSubsciption(channel)).isNotNull();
		assertThat(stream1).isEqualTo(stream2);
	}

}
