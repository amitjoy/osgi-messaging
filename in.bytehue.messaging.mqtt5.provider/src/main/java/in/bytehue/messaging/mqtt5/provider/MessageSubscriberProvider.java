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
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getQoS;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.toMessage;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.QOS;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageSubscription;
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
@Component(service = { MessageSubscription.class, MessageSubscriberProvider.class })
public final class MessageSubscriberProvider implements MessageSubscription {

    @Activate
    private BundleContext bundleContext;

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private MessageClientProvider messagingClient;

    @Reference
    private MessageSubscriptionRegistry subscriptionRegistry;

    @Reference
    private ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

    @Deactivate
    void stop() {
        subscriptionRegistry.clearAllSubscriptions();
    }

    @Override
    public PushStream<Message> subscribe(final String subChannel) {
        return subscribe(null, subChannel, null, null);
    }

    @Override
    public PushStream<Message> subscribe(final MessageContext context) {
        return subscribe(context, null, null, null);
    }

    public PushStream<Message> replyToSubscribe( //
            final String subChannel, //
            final String pubChannel, //
            final ServiceReference<?> reference) {
        return subscribe(null, subChannel, pubChannel, reference);
    }

    private PushStream<Message> subscribe( //
            MessageContext context, //
            final String subChannel, //
            final String pubChannel, //
            final ServiceReference<?> reference) {

        final PushStreamProvider provider = new PushStreamProvider();
        final SimplePushEventSource<Message> source = provider.createSimpleEventSource(Message.class);
        final PushStream<Message> stream = provider.createStream(source);
        try {
            final MessageContextBuilderProvider builder = mcbFactory.getService();
            try {
                if (context == null) {
                    context = builder.channel(subChannel).buildContext();
                }
            } finally {
                mcbFactory.ungetService(builder);
            }
            requireNonNull(subChannel, "Channel cannot be null");

            final int qos;
            final boolean receiveLocal;
            final boolean retainAsPublished;
            final MessageContextProvider ctx = (MessageContextProvider) context;
            final Map<String, Object> extensions = context.getExtensions();

            if (extensions != null) {
                qos = getQoS(extensions);
                receiveLocal = (boolean) extensions.getOrDefault(RECEIVE_LOCAL, false);
                retainAsPublished = (boolean) extensions.getOrDefault(RETAIN, false);
            } else {
                qos = DEFAULT_QOS.getCode();
                receiveLocal = true;
                retainAsPublished = false;
            }

            // @formatter:off
            messagingClient.client.subscribeWith()
                                      .topicFilter(subChannel)
                                      .qos(MqttQos.fromCode(qos))
                                      .noLocal(receiveLocal)
                                      .retainAsPublished(retainAsPublished)
                                      .callback(p -> {
                                          final MessageContextBuilderProvider mcb = mcbFactory.getService();
                                          mcb.withContext(ctx);
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
                                              subscriptionRegistry.addSubscription(stream, subChannel, pubChannel, reference);
                                              logger.debug("New subscription request for '{}' processed successfully - {}", subChannel, ack);
                                          } else {
                                              logger.error("New subscription request for '{}' failed - {}", subChannel, ack);
                                          }
                                      });
            stream.onClose(() -> {
                subscriptionRegistry.removeSubscription(stream); // remove the subscription from registry
                source.close(); // close the event source
            });
            return stream;
        } catch (final Exception e) {
            logger.error("Error while subscribing to {}", subChannel, e);
            throw e;
        }
    }

    private boolean isSubscriptionAcknowledged(final Mqtt5SubAck ack) {
        final List<Mqtt5SubAckReasonCode> acceptedCodes = Arrays.asList(
                GRANTED_QOS_0,
                GRANTED_QOS_1,
                GRANTED_QOS_2
        );
        final List<Mqtt5SubAckReasonCode> reasonCodes = ack.getReasonCodes();
        return reasonCodes.stream().findFirst().filter(acceptedCodes::contains).isPresent();
    }

}
