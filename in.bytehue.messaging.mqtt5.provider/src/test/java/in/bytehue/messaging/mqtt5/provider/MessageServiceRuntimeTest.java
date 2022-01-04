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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForRequestProcessing;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.EXTENSION_AUTO_ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.EXTENSION_LAST_WILL;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.MESSAGE_CONTEXT_BUILDER;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttMessageConstants;
import in.bytehue.messaging.mqtt5.provider.helper.MessageHelper;

@RunWith(LaunchpadRunner.class)
public final class MessageServiceRuntimeTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MessageContextBuilder mcb;

    @Service
    private MessagePublisher publisher;

    @Service
    private MessageSubscription subscriber;

    @Service
    private MessageServiceRuntime runtime;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Before
    public void setup() throws InterruptedException {
        waitForMqttConnectionReady(launchpad);
    }

    @Test
    public void test_connection_uri() throws Exception {
        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        assertThat(runtimeDTO.connectionURI).isEqualTo("broker.emqx.io");
    }

    @Test
    public void test_provider_name() throws Exception {
        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        assertThat(runtimeDTO.providerName).isEqualTo(MqttMessageConstants.MESSAGING_PROVIDER);
    }

    @Test
    public void test_protocols() throws Exception {
        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        assertThat(runtimeDTO.protocols).hasSize(1).contains(MqttMessageConstants.MESSAGING_PROTOCOL);
    }

    @Test
    public void test_instance_id() throws Exception {
        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        final ServiceReference<MessageClientProvider> client = launchpad
                .waitForServiceReference(MessageClientProvider.class, 3000L).get();

        assertThat(runtimeDTO.instanceId).isEqualTo(client.getProperty(Constants.SERVICE_ID).toString());
    }

    @Test
    public void test_features() throws Exception {
        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        // @formatter:off
        assertThat(runtimeDTO.features).contains(RETAIN,
                REPLY_TO,
                ACKNOWLEDGE,
                RECEIVE_LOCAL,
                EXTENSION_QOS,
                USER_PROPERTIES,
                EXTENSION_LAST_WILL,
                REPLY_TO_MANY_PUBLISH,
                GENERATE_REPLY_CHANNEL,
                MESSAGE_EXPIRY_INTERVAL,
                GENERATE_CORRELATION_ID,
                REPLY_TO_MANY_SUBSCRIBE,
                MESSAGE_CONTEXT_BUILDER,
                EXTENSION_AUTO_ACKNOWLEDGE,
                EXTENSION_GUARANTEED_ORDERING,
                EXTENSION_GUARANTEED_DELIVERY);
        // @formatter:on
    }

    @Test
    public void test_service_dto() throws Exception {
        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        final ServiceReference<MessageServiceRuntime> runtimeSref = launchpad
                .waitForServiceReference(MessageServiceRuntime.class, 3000L).get();

        final ServiceReferenceDTO dto = MessageHelper.toServiceReferenceDTO(runtimeSref);

        assertThat(runtimeDTO.serviceDTO.id).isEqualTo(dto.id);
        assertThat(runtimeDTO.serviceDTO.properties).isEqualTo(dto.properties);
        assertThat(runtimeDTO.serviceDTO.bundle).isEqualTo(dto.bundle);
        assertThat(runtimeDTO.serviceDTO.usingBundles).isEqualTo(dto.usingBundles);
    }

    @Test
    public void test_subscription() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String channel = "a/b";
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
        TimeUnit.SECONDS.sleep(3);

        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertThat(runtimeDTO.subscriptions).hasSize(1);
        assertThat(runtimeDTO.subscriptions[0].channel.connected).isTrue();
        assertThat(runtimeDTO.subscriptions[0].channel.name).isEqualTo(channel);
    }

    @Test
    public void test_reply_to_single_subscription() throws Exception {
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
        final String[] channelValue = new String[] { channel };

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

        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertThat(runtimeDTO.subscriptions).hasSize(1);
        assertThat(runtimeDTO.replyToSubscriptions).hasSize(1);
        assertThat(runtimeDTO.replyToSubscriptions[0].responseChannel).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].requestChannel).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].serviceDTO).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].generateCorrelationId).isTrue();
        assertThat(runtimeDTO.replyToSubscriptions[0].generateReplyChannel).isTrue();
        assertThat(runtimeDTO.replyToSubscriptions[0].requestChannel.name).isEqualTo(channel);
        assertThat(runtimeDTO.replyToSubscriptions[0].responseChannel.name).isEqualTo(replyToChannel);
    }

    @Test
    public void test_reply_to_subscription() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        final ReplyToSubscriptionHandler handler = m -> {
            flag.set(true);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

        final String channelKey = "osgi.messaging.replyToSubscription.channel";
        final String[] channelValue = new String[] { channel };

        launchpad.register(ReplyToSubscriptionHandler.class, handler, targetKey, targetValue, channelKey, channelValue);

        // @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

        publisher.publish(message);
        waitForRequestProcessing(flag);

        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertThat(runtimeDTO.subscriptions).isEmpty();
        assertThat(runtimeDTO.replyToSubscriptions).hasSize(1);
        assertThat(runtimeDTO.replyToSubscriptions[0].responseChannel).isNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].requestChannel).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].requestChannel.name).isEqualTo(channel);
        assertThat(runtimeDTO.replyToSubscriptions[0].serviceDTO).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].generateCorrelationId).isTrue();
        assertThat(runtimeDTO.replyToSubscriptions[0].generateReplyChannel).isTrue();
    }

    @Test
    public void test_reply_to_many_subscription() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String replyToChannel = "c/d";
        final String payload = "abc";
        final String responsePyload = "test";
        final String contentType = "text/plain";

        final PushStreamProvider provider = new PushStreamProvider();
        final SimplePushEventSource<Message> source = provider.createSimpleEventSource(Message.class);

        final ReplyToManySubscriptionHandler handler = (m, b) -> {
            final Message message = b.content(ByteBuffer.wrap(responsePyload.getBytes())).buildMessage();
            new Thread(() -> {
                while (true) {
                    source.publish(message);
                    flag1.set(true);
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

        final String channelKey = "osgi.messaging.replyToSubscription.channel";
        final String[] channelValue = new String[] { channel };

        final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
        final String replyToChannelValue = replyToChannel;

        launchpad.register(ReplyToManySubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
                channelValue, replyToChannelKey, replyToChannelValue);

        // @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

        subscriber.subscribe(replyToChannel).forEach(m -> {
            if (responsePyload.equals(new String(m.payload().array(), StandardCharsets.UTF_8))) {
                flag2.set(true);
            }
        });

        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);

        final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertThat(runtimeDTO.subscriptions).hasSize(1);
        assertThat(runtimeDTO.replyToSubscriptions).hasSize(1);
        assertThat(runtimeDTO.replyToSubscriptions[0].responseChannel).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].requestChannel).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].serviceDTO).isNotNull();
        assertThat(runtimeDTO.replyToSubscriptions[0].generateCorrelationId).isTrue();
        assertThat(runtimeDTO.replyToSubscriptions[0].generateReplyChannel).isTrue();
        assertThat(runtimeDTO.replyToSubscriptions[0].requestChannel.name).isEqualTo(channel);
        assertThat(runtimeDTO.replyToSubscriptions[0].responseChannel.name).isEqualTo(replyToChannel);
    }

}
