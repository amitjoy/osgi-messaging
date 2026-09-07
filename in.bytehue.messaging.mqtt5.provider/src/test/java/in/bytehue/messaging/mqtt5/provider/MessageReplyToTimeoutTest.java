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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.PUBLISHER_REPLYTO;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.replyto.ReplyToPublisher;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToTimeoutTest {

	@Service
	private Launchpad launchpad;

	@Service
	private ReplyToPublisher replyToPublisher;

	@Service
	private ConfigurationAdmin configAdmin;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws Exception {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_publish_with_reply_missing_reply_to_channel() {
		final MessageContextBuilder mcb = launchpad.getService(MessageContextBuilder.class).get();
		final Message message = mcb.channel("req/channel")
		                           .content(ByteBuffer.wrap("hello".getBytes()))
		                           .buildMessage();

		assertThatThrownBy(() -> replyToPublisher.publishWithReply(message))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Reply-to channel is missing");
	}

	@Test
	public void test_publish_with_reply_custom_timeout() throws Exception {
		final Configuration config = configAdmin.getConfiguration(PUBLISHER_REPLYTO, "?");
		final Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("requestTimeoutInMillis", 2000L);
		config.update(properties);

		// Give config update a moment to take effect
		Thread.sleep(500);

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<Throwable> failureRef = new AtomicReference<>();

		final MessageContextBuilder mcb = launchpad.getService(MessageContextBuilder.class).get();
		final Message message = mcb.channel("unanswered/request/timeout")
		                           .replyTo("unanswered/reply/timeout")
		                           .correlationId(UUID.randomUUID().toString())
		                           .content(ByteBuffer.wrap("ping".getBytes()))
		                           .buildMessage();

		final long start = System.currentTimeMillis();
		replyToPublisher.publishWithReply(message).onFailure(t -> {
			failureRef.set(t);
			latch.countDown();
		});

		final boolean completed = latch.await(10, SECONDS);
		final long elapsed = System.currentTimeMillis() - start;

		assertThat(completed).isTrue();
		assertThat(failureRef.get()).isNotNull();
		assertThat(elapsed).isGreaterThanOrEqualTo(1500L);
		assertThat(elapsed).isLessThan(8000L);
	}

}
