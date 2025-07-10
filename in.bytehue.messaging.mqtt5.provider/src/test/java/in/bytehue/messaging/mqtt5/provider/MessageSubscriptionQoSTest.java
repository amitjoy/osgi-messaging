/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.SUBSCRIBER;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_FEATURE_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_NAME_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;
import org.osgi.util.pushstream.PushStream;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;

@RunWith(LaunchpadRunner.class)
public final class MessageSubscriptionQoSTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessageSubscription subscriber;

	@Service
	private MessageServiceRuntime runtime;

	@Service
	private MqttMessageContextBuilder mcb;

	@Service
	private ConfigurationAdmin configAdmin;

	private ServiceRegistration<?> handlerRegistration;
	private PushStream<?> subscriptionStream;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@After
	public void cleanup() throws IOException, InterruptedException {
		if (handlerRegistration != null) {
			handlerRegistration.unregister();
		}
		if (subscriptionStream != null) {
			subscriptionStream.close();
		}
		// Allow time for processing before resetting config
		TimeUnit.SECONDS.sleep(2);

		// Reset subscriber configuration to its default by deleting the config
		final Configuration config = configAdmin.getConfiguration("in.bytehue.messaging.mqtt5.provider.subscriber",
				"?");
		if (config.getProperties() != null) {
			config.delete();
			// Allow time for component to restart with default config
			TimeUnit.SECONDS.sleep(2);
		}
	}

	@Test
	public void test_replyToSubscription_with_explicit_qos() throws Exception {
		final String requestChannel = "test/qos/request";
		final int qos = 2;

		final ReplyToSingleSubscriptionHandler handler = (request, response) -> response.buildMessage();

		final String targetKey = REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;
		final String targetValue = String.format("(&(%s=%s)(%s=%s)(%s=%s))", MESSAGING_PROTOCOL_PROPERTY,
				MESSAGING_PROTOCOL, MESSAGING_NAME_PROPERTY, MESSAGING_ID, MESSAGING_FEATURE_PROPERTY, REPLY_TO);

		final String channelKey = REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
		final String[] channelValue = { requestChannel };

		final String qosKey = EXTENSION_QOS;
		final int qosValue = qos;

		// Pass properties as separate key-value arguments
		handlerRegistration = launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey,
				targetValue, channelKey, channelValue, qosKey, qosValue);

		TimeUnit.SECONDS.sleep(2);

		final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		assertThat(runtimeDTO.replyToSubscriptions).hasSize(1);

		final ReplyToSubscriptionDTO replyToDTO = runtimeDTO.replyToSubscriptions[0];
		assertThat(replyToDTO.qos).isEqualTo(qos);
	}

	@Test
	public void test_replyToSubscription_without_explicit_qos_uses_global_default() throws Exception {
		final String requestChannel = "test/qos/request/default";
		// The default QoS from the subscriber configuration is 0
		final int defaultQos = 0;

		final ReplyToSingleSubscriptionHandler handler = (request, response) -> response.buildMessage();

		final String targetKey = REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;
		final String targetValue = String.format("(&(%s=%s)(%s=%s)(%s=%s))", MESSAGING_PROTOCOL_PROPERTY,
				MESSAGING_PROTOCOL, MESSAGING_NAME_PROPERTY, MESSAGING_ID, MESSAGING_FEATURE_PROPERTY, REPLY_TO);

		final String channelKey = REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
		final String[] channelValue = { requestChannel };

		// Pass properties as separate key-value arguments
		handlerRegistration = launchpad.register(ReplyToSingleSubscriptionHandler.class, handler, targetKey,
				targetValue, channelKey, channelValue);

		TimeUnit.SECONDS.sleep(2);

		final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		assertThat(runtimeDTO.replyToSubscriptions).hasSize(1);

		final ReplyToSubscriptionDTO replyToDTO = runtimeDTO.replyToSubscriptions[0];
		assertThat(replyToDTO.qos).isEqualTo(defaultQos);
	}

	@Test
	public void test_normalSubscription_with_explicit_qos() throws Exception {
		final String channel = "test/qos/normal";
		final int qos = 1;

		final MessageContext context = mcb.channel(channel).extensionEntry(EXTENSION_QOS, qos).buildContext();
		subscriptionStream = subscriber.subscribe(context);
		TimeUnit.SECONDS.sleep(2);

		final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		assertThat(runtimeDTO.subscriptions).hasSize(1);

		final SubscriptionDTO subscriptionDTO = runtimeDTO.subscriptions[0];
		assertThat(subscriptionDTO.channel.name).isEqualTo(channel);
		assertThat(subscriptionDTO.qos).isEqualTo(qos);
	}

	@Test
	public void test_normalSubscription_without_explicit_qos_uses_global_default() throws Exception {
		final String channel = "test/qos/normal/default";
		// The default QoS from the subscriber configuration is 0
		final int defaultQos = 0;

		subscriptionStream = subscriber.subscribe(channel);
		TimeUnit.SECONDS.sleep(2);

		final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		assertThat(runtimeDTO.subscriptions).hasSize(1);

		final SubscriptionDTO subscriptionDTO = runtimeDTO.subscriptions[0];
		assertThat(subscriptionDTO.channel.name).isEqualTo(channel);
		assertThat(subscriptionDTO.qos).isEqualTo(defaultQos);
	}

	@Test
	public void test_global_qos_configuration_change_is_reflected_in_subscriptions() throws Exception {
		final int newDefaultQos = 2;

		// Change the global default QoS for the subscriber
		final Configuration config = configAdmin.getConfiguration(SUBSCRIBER, "?");
		final Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("qos", newDefaultQos);
		config.update(properties);

		// Robustly wait for the configuration to be applied
		final MessageSubscriptionProvider provider = launchpad.getService(MessageSubscriptionProvider.class).get();
		await().atMost(5, SECONDS).until(() -> provider.config().qos() == newDefaultQos);

		// ** Test Scenario 1: Normal subscription should use the new global QoS **
		final String normalChannel = "test/qos/global/normal";
		subscriptionStream = subscriber.subscribe(normalChannel);
		TimeUnit.SECONDS.sleep(2);

		MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		assertThat(runtimeDTO.subscriptions).hasSize(1);

		SubscriptionDTO subscriptionDTO = runtimeDTO.subscriptions[0];
		assertThat(subscriptionDTO.channel.name).isEqualTo(normalChannel);
		assertThat(subscriptionDTO.qos).isEqualTo(newDefaultQos);
		subscriptionStream.close();
		TimeUnit.SECONDS.sleep(2);

		// ** Test Scenario 2: Reply-To subscription should use the new global QoS **
		final String replyToChannel = "test/qos/global/replyto";
		final ReplyToSingleSubscriptionHandler handler = (request, response) -> response.buildMessage();

		handlerRegistration = launchpad.register(ReplyToSingleSubscriptionHandler.class, handler,
				REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY,
				String.format("(&(%s=%s)(%s=%s)(%s=%s))", MESSAGING_PROTOCOL_PROPERTY, MESSAGING_PROTOCOL,
						MESSAGING_NAME_PROPERTY, MESSAGING_ID, MESSAGING_FEATURE_PROPERTY, REPLY_TO),
				REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY, new String[] { replyToChannel });

		// Wait for the reply-to subscription to appear in the runtime DTO
		await().atMost(5, SECONDS).until(() -> runtime.getRuntimeDTO().replyToSubscriptions.length == 1);

		runtimeDTO = runtime.getRuntimeDTO();
		ReplyToSubscriptionDTO replyToDTO = runtimeDTO.replyToSubscriptions[0];
		assertThat(replyToDTO.requestChannel.name).isEqualTo(replyToChannel);
		assertThat(replyToDTO.qos).isEqualTo(newDefaultQos);
	}

}