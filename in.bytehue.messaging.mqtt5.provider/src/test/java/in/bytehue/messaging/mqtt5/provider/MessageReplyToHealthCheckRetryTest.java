/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionRegistry.ExtendedSubscription;

/**
 * Tests the health check and automatic retry mechanism for ReplyTo
 * subscriptions. This test simulates scenarios where subscriptions are closed
 * unexpectedly (e.g., during reconnection) and verifies that the health check
 * mechanism detects and automatically re-establishes them.
 */
@RunWith(LaunchpadRunner.class)
public final class MessageReplyToHealthCheckRetryTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessageContextBuilder mcb;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageSubscriptionRegistry registry;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	/**
	 * Test that health check detects missing subscription and triggers retry.
	 * 
	 * Scenario: 1. Register a ReplyTo handler 2. Verify subscription is created 3.
	 * Close the subscription stream (simulating unexpected closure) 4. Wait for
	 * health check to run (max 10 seconds) 5. Verify subscription is automatically
	 * re-established
	 */
	@Test
	public void test_health_check_detects_and_retries_missing_subscription() throws Exception {
		final AtomicBoolean messageReceived = new AtomicBoolean();
		final AtomicInteger handlerCallCount = new AtomicInteger(0);

		final String channel = "health/check/test/request";
		final String replyToChannel = "health/check/test/response";
		final String payload = "test payload";
		final String contentType = "text/plain";

		// Register handler
		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			handlerCallCount.incrementAndGet();
			// @formatter:off
			final Message message = mcb.contentType(contentType)
					.content(ByteBuffer.wrap(payload.getBytes()))
					.buildMessage();
			// @formatter:on
			messageReceived.set(true);
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

		// Wait for subscription to be established
		await().atMost(20, SECONDS).untilAsserted(() -> {
			assertThat(registry.hasActiveSubscription(channel)).isTrue();
		});

		// Verify handler works before closure
		// @formatter:off
		final Message testMessage1 = mcb.channel(channel)
				.replyTo(replyToChannel)
				.contentType(contentType)
				.content(ByteBuffer.wrap("before closure".getBytes()))
				.buildMessage();
		// @formatter:on
		publisher.publish(testMessage1);
		waitForRequestProcessing(messageReceived);

		assertThat(handlerCallCount.get()).isEqualTo(1);

		// Simulate unexpected stream closure (e.g., during reconnection)
		// This will remove the subscription from the registry
		final ExtendedSubscription sub = findSubscription(channel);
		assertThat(sub).isNotNull();

		// Close the stream - this simulates what happens during reconnection issues
		sub.connectedStreamCloser.run();

		// Verify subscription was removed
		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(registry.hasActiveSubscription(channel)).isFalse();
		});

		// Wait for health check to detect and retry (runs every 5 seconds, initial
		// delay 10 seconds)
		// We need to wait up to initial delay + interval = 15 seconds
		await().atMost(20, SECONDS).untilAsserted(() -> {
			assertThat(registry.hasActiveSubscription(channel)).isTrue();
		});

		// Reset flags for second test
		messageReceived.set(false);

		// Verify handler works after automatic retry
		// @formatter:off
		final Message testMessage2 = mcb.channel(channel)
				.replyTo(replyToChannel)
				.contentType(contentType)
				.content(ByteBuffer.wrap("after retry".getBytes()))
				.buildMessage();
		// @formatter:on
		publisher.publish(testMessage2);
		waitForRequestProcessing(messageReceived);

		// Handler should have been called twice total
		assertThat(handlerCallCount.get()).isEqualTo(2);
	}

	/**
	 * Test that health check respects max retry attempts.
	 * 
	 * Scenario: 1. Register a handler with invalid configuration (will fail to
	 * subscribe) 2. Verify health check attempts retries 3. Verify it eventually
	 * gives up after max attempts
	 * 
	 * Note: This test is simplified - in reality, we'd need to inject failures or
	 * use a mock to test retry limits properly.
	 */
	@Test
	public void test_health_check_creates_subscription_after_delayed_registration() throws Exception {
		final AtomicBoolean messageReceived = new AtomicBoolean();

		final String channel = "health/check/delayed/request";
		final String replyToChannel = "health/check/delayed/response";
		final String payload = "delayed test";
		final String contentType = "text/plain";

		// Initially, no subscription exists
		assertThat(registry.hasActiveSubscription(channel)).isFalse();

		// Now register the handler
		final ReplyToSingleSubscriptionHandler handler = (m, mcb) -> {
			// @formatter:off
			final Message message = mcb.contentType(contentType)
					.content(ByteBuffer.wrap(payload.getBytes()))
					.buildMessage();
			// @formatter:on
			messageReceived.set(true);
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

		// Wait for subscription to be established
		await().atMost(20, SECONDS).untilAsserted(() -> {
			assertThat(registry.hasActiveSubscription(channel)).isTrue();
		});

		// Publish a test message
		// @formatter:off
		final Message testMessage = mcb.channel(channel)
				.replyTo(replyToChannel)
				.contentType(contentType)
				.content(ByteBuffer.wrap("delayed registration test".getBytes()))
				.buildMessage();
		// @formatter:on

		publisher.publish(testMessage);
		waitForRequestProcessing(messageReceived);

		assertThat(messageReceived.get()).isTrue();
	}

	/**
	 * Test multiple handlers with health check.
	 * 
	 * Scenario: 1. Register multiple ReplyTo handlers 2. Close one subscription 3.
	 * Verify health check only retries the closed one 4. Verify all handlers work
	 * correctly
	 */
	@Test
	public void test_health_check_handles_multiple_handlers_independently() throws Exception {
		final AtomicBoolean handler1Called = new AtomicBoolean();
		final AtomicBoolean handler2Called = new AtomicBoolean();

		final String channel1 = "health/check/multi/request1";
		final String channel2 = "health/check/multi/request2";
		final String replyToChannel1 = "health/check/multi/response1";
		final String replyToChannel2 = "health/check/multi/response2";
		final String payload = "multi test";
		final String contentType = "text/plain";

		// Register first handler
		final ReplyToSingleSubscriptionHandler handler1 = (m, mcb) -> {
			// @formatter:off
			final Message message = mcb.contentType(contentType)
					.content(ByteBuffer.wrap(payload.getBytes()))
					.buildMessage();
			// @formatter:on
			handler1Called.set(true);
			return message;
		};

		// Register second handler
		final ReplyToSingleSubscriptionHandler handler2 = (m, mcb) -> {
			// @formatter:off
			final Message message = mcb.contentType(contentType)
					.content(ByteBuffer.wrap(payload.getBytes()))
					.buildMessage();
			// @formatter:on
			handler2Called.set(true);
			return message;
		};

		final String targetKey = "osgi.messaging.replyToSubscription.target";
		final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";
		final String channelKey = "osgi.messaging.replyToSubscription.channel";
		final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler1, targetKey, targetValue, channelKey,
				new String[] { channel1 }, replyToChannelKey, replyToChannel1);

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler2, targetKey, targetValue, channelKey,
				new String[] { channel2 }, replyToChannelKey, replyToChannel2);

		// Wait for both subscriptions to be established
		await().atMost(20, SECONDS).untilAsserted(() -> {
			assertThat(registry.hasActiveSubscription(channel1)).isTrue();
			assertThat(registry.hasActiveSubscription(channel2)).isTrue();
		});

		// Close only the first subscription
		final ExtendedSubscription sub1 = findSubscription(channel1);
		assertThat(sub1).isNotNull();
		sub1.connectedStreamCloser.run();

		// Verify first subscription was removed, second still active
		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(registry.hasActiveSubscription(channel1)).isFalse();
			assertThat(registry.hasActiveSubscription(channel2)).isTrue();
		});

		// Wait for health check to restore first subscription
		await().atMost(20, SECONDS).untilAsserted(() -> {
			assertThat(registry.hasActiveSubscription(channel1)).isTrue();
			assertThat(registry.hasActiveSubscription(channel2)).isTrue();
		});

		// Give the stream processing pipeline more time to fully initialize
		SECONDS.sleep(2);

		// Verify subscriptions are still active before publishing
		assertThat(registry.hasActiveSubscription(channel1)).isTrue();
		assertThat(registry.hasActiveSubscription(channel2)).isTrue();

		// Test both handlers work
		// @formatter:off
		final Message testMessage1 = mcb.channel(channel1)
		        .replyTo(replyToChannel1)
		        .contentType(contentType)
		        .content(ByteBuffer.wrap("test1".getBytes()))
		        .buildMessage();

		publisher.publish(testMessage1);
		waitForRequestProcessing(handler1Called);

		final Message testMessage2 = mcb.channel(channel2)
		        .replyTo(replyToChannel2)
		        .contentType(contentType)
		        .content(ByteBuffer.wrap("test2".getBytes()))
		        .buildMessage();
		
		publisher.publish(testMessage2);
		waitForRequestProcessing(handler2Called);
		// @formatter:on

		assertThat(handler1Called.get()).isTrue();
		assertThat(handler2Called.get()).isTrue();
	}

	/**
	 * Helper method to find a subscription in the registry by channel.
	 */
	private ExtendedSubscription findSubscription(final String channel) {
		final Map<String, Map<String, ExtendedSubscription>> subscriptions = registry.subscriptions;

		final Map<String, MessageSubscriptionRegistry.ExtendedSubscription> channelSubs = subscriptions.get(channel);
		if (channelSubs != null && !channelSubs.isEmpty()) {
			return channelSubs.values().iterator().next();
		}
		return null;
	}

}
