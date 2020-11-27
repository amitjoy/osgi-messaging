/*******************************************************************************
 * Copyright 2020 Amit Kumar Mondal
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

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode.GRANTED_QOS_0;
import static com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode.GRANTED_QOS_1;
import static com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode.GRANTED_QOS_2;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Component.MESSAGE_SUBSCRIBER;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.acknowledgeMessage;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.findQoS;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.findServiceRefAsDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.initChannelDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.toMessage;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.QOS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode;

//@formatter:off
@MessagingFeature(
        name = MESSAGE_SUBSCRIBER,
        protocol = MQTT_PROTOCOL,
        feature = {
                QOS,
                RETAIN,
                ACKNOWLEDGE,
                RECEIVE_LOCAL })
//@formatter:on
@Component(service = { MessageSubscription.class, SimpleMessageSubscriber.class })
public final class SimpleMessageSubscriber implements MessageSubscription {

    @Activate
    private BundleContext bundleContext;

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private SimpleMessageClient messagingClient;

    @Reference
    private ComponentServiceObjects<SimpleMessageContextBuilder> mcbFactory;

    private final Map<PushStream<Message>, ChannelDTO> subscriptions = new HashMap<>();

    @Override
    public PushStream<Message> subscribe(final String channel) {
        return subscribe(null, channel);
    }

    @Override
    public PushStream<Message> subscribe(final MessageContext context) {
        return subscribe(context, null);
    }

    private PushStream<Message> subscribe(MessageContext context, final String channel) {
        final PushStreamProvider provider = new PushStreamProvider();
        final SimplePushEventSource<Message> source = provider.createSimpleEventSource(Message.class);
        final PushStream<Message> stream = provider.createStream(source);
        try {
            if (context == null) {
                context = mcbFactory.getService().channel(channel).buildContext();
            }
            final SimpleMessageContext ctx = (SimpleMessageContext) context;
            requireNonNull(channel, "Channel cannot be null");
            final Map<String, Object> extensions = context.getExtensions();

            int qos;
            boolean receiveLocal;
            boolean retainAsPublished;

            if (extensions != null) {
                qos = findQoS(extensions);
                receiveLocal = (boolean) extensions.getOrDefault(RECEIVE_LOCAL, false);
                retainAsPublished = (boolean) extensions.getOrDefault(RETAIN, false);
            } else {
                qos = DEFAULT_QOS.getCode();
                receiveLocal = true;
                retainAsPublished = false;
            }

            // @formatter:off
            messagingClient.client.subscribeWith()
                                      .topicFilter(channel)
                                      .qos(MqttQos.fromCode(qos))
                                      .noLocal(receiveLocal)
                                      .retainAsPublished(retainAsPublished)
                                      .callback(p -> {
                                          final SimpleMessageContextBuilder mcb = mcbFactory.getService();
                                          try {
                                              final Message message = toMessage(p, mcb);
                                              acknowledgeMessage(message, ctx, source::publish);
                                          } catch (final Exception e) {
                                              source.error(e);
                                          } finally {
                                              mcbFactory.ungetService(mcb);
                                          }
                                      })
                                      .send()
                                      .thenAccept(ack -> {
                                          if (isSubscriptionAcknowledged(ack)) {
                                              subscriptions.put(
                                                      stream,
                                                      initChannelDTO(channel, null, true));
                                              logger.debug("New subscription request for '{}' has been processed successfully", channel);
                                          } else {
                                              logger.error("New subscription request for '{}' failed - {}", channel, ack);
                                          }
                                      });
            stream.onClose(() -> {
                final ChannelDTO dto = subscriptions.get(stream);
                dto.connected = false;
                messagingClient.client.unsubscribeWith()
                                          .addTopicFilter(channel)
                                      .send();
                subscriptions.remove(stream); // remove the subscription from registry
                source.close();
                // @formatter:on

            });
            return stream;
        } catch (final Exception e) {
            logger.error("Error while subscribing to {}", channel, e);
            throw e;
        }
    }

    public SubscriptionDTO[] getSubscriptionDTOs() {
        // @formatter:off
        return subscriptions.values()
                            .stream()
                            .map(this::initSubscriptionDTO)
                            .toArray(SubscriptionDTO[]::new);
        // @formatter:on
    }

    private SubscriptionDTO initSubscriptionDTO(final ChannelDTO channelDTO) {
        final SubscriptionDTO subscriptionDTO = new SubscriptionDTO();

        subscriptionDTO.serviceDTO = findServiceRefAsDTO(MessageSubscription.class, bundleContext);
        subscriptionDTO.channel = channelDTO;

        return subscriptionDTO;
    }

    private boolean isSubscriptionAcknowledged(final Mqtt5SubAck ack) {
        // @formatter:off
        final List<Mqtt5SubAckReasonCode> acceptedCodes = Arrays.asList(
                GRANTED_QOS_0,
                GRANTED_QOS_1,
                GRANTED_QOS_2
        );
        // @formatter:off
        final List<Mqtt5SubAckReasonCode> reasonCodes = ack.getReasonCodes();
        return reasonCodes.stream().findFirst().filter(e -> acceptedCodes.contains(e)).isPresent();
    }

}
