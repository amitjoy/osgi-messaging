/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_FEATURE_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_NAME_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;
//highlight-next-line
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttMessageConstants;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToRuntimeTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessageContextBuilder mcb;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageServiceRuntime runtime;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_reply_to_subscription_is_correctly_categorized_in_runtime() throws Exception {
		final String requestChannel = "test/runtime/request";
		final String responseChannel = "test/runtime/response";
		final String payload = "runtime-test";
		final String contentType = "text/plain";

		final ReplyToSingleSubscriptionHandler handler = (requestMessage, responseMcb) -> {
			return responseMcb.content(requestMessage.payload()).buildMessage();
		};

		final String targetKey = REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;
		final String targetValue = String.format("(&(%s=%s)(%s=%s)(%s=%s))", MESSAGING_PROTOCOL_PROPERTY,
				MqttMessageConstants.MESSAGING_PROTOCOL, MESSAGING_NAME_PROPERTY, MqttMessageConstants.MESSAGING_ID,
				MESSAGING_FEATURE_PROPERTY, REPLY_TO);

		final String channelKey = REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
		final String[] channelValue = { requestChannel };

		launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
				channelValue);

		// Give the whiteboard provider a moment to process the new service
		Thread.sleep(500);

		// Publish a message that provides the reply-to channel in its context
		final Message messageToPublish = mcb.channel(requestChannel).replyTo(responseChannel).contentType(contentType)
				.content(ByteBuffer.wrap(payload.getBytes())).buildMessage();
		publisher.publish(messageToPublish);

		// Wait for the publish -> handle -> reply cycle to complete
		Thread.sleep(500);

		final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		// Simple subscriptions list should be empty
		assertThat(runtimeDTO.subscriptions.length).isEqualTo(0);

		// Reply-To subscriptions list should contain one item
		assertThat(runtimeDTO.replyToSubscriptions.length).isEqualTo(1);

		final ReplyToSubscriptionDTO replyToDTO = runtimeDTO.replyToSubscriptions[0];
		assertThat(replyToDTO.requestChannel.name).isEqualTo(requestChannel);
		assertThat(replyToDTO.responseChannel.name).isEqualTo(responseChannel);
	}
}