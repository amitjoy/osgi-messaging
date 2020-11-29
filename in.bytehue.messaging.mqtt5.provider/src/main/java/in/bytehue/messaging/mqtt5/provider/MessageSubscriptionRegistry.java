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

import static com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode.NO_SUBSCRIPTIONS_EXISTED;
import static com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode.SUCCESS;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CLIENT;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getDTOFromClass;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.serviceReferenceDTO;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.util.pushstream.PushStream;

import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAck;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode;

//@formatter:off
@Component(service = MessageSubscriptionRegistry.class, configurationPid = CLIENT)
public final class MessageSubscriptionRegistry {

    @interface Config {
        boolean cleanStart() default false;
    }

    @Activate
    private Config config;

    @Activate
    private BundleContext bundleContext;

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private MessageClientProvider messagingClient;

    private final Map<String, ExtendedSubscriptionDTO> subscriptions = new HashMap<>();

    public void addSubscription(
            final String pubChannel,
            final String subChannel,
            final PushStream<Message> stream,
            final ServiceReference<?> handlerReference) {

        final String topicFilter = toTopicFilter(subChannel);
        subscriptions.computeIfAbsent(
                            topicFilter,
                            e -> new ExtendedSubscriptionDTO(
                                    pubChannel,
                                    subChannel,
                                    handlerReference))
                     .connectedStreams
                     .add(stream);
    }

    public void removeSubscription(final String subChannel) {
        removeSubscription(subChannel, ps -> true);
    }

    public void removeSubscription(final String subChannel, final Predicate<PushStream<Message>> predicate) {
        final String topicFilter = toTopicFilter(subChannel);

        if (subscriptions.containsKey(topicFilter)) {
            final List<PushStream<Message>> connectedStreams = subscriptions.get(topicFilter).connectedStreams;
            connectedStreams.removeIf(predicate::test);

            if (connectedStreams.isEmpty() && !config.cleanStart()) {
                messagingClient.client
                               .unsubscribeWith()
                               .addTopicFilter(topicFilter)
                               .send()
                               .thenAccept(ack -> {
                                   if (isUnsubscriptionAcknowledged(ack)) {
                                       subscriptions.remove(topicFilter);
                                       logger.debug("Unsubscription request for '{}' processed successfully - {}", subChannel, ack);
                                   } else {
                                       logger.error("Unsubscription request for '{}' failed - {}", subChannel, ack);
                                   }
                               });
            }
        }
    }

    @Deactivate
    public void clearAllSubscriptions() {
        subscriptions.keySet().forEach(this::removeSubscription);
    }

    private class ExtendedSubscriptionDTO {

        ChannelDTO pubChannel;
        ChannelDTO subChannel;
        ServiceReference<?> handlerReference;
        List<PushStream<Message>> connectedStreams;

        public ExtendedSubscriptionDTO(
                final String pubChannel,
                final String subChannel,
                final ServiceReference<?> handlerReference) {

            // a channel is always connected if a subscription in client exists
            final boolean isConnected = true;

            this.pubChannel       = createChannelDTO(pubChannel, isConnected);
            this.subChannel       = createChannelDTO(subChannel, isConnected);
            this.handlerReference = handlerReference;

            connectedStreams = new ArrayList<>();
        }

        private ChannelDTO createChannelDTO(final String name, final boolean isConnected) {
            if (name == null) {
                return null;
            }
            final String rountingKey = null; // no routing key for MQTT
            final ChannelDTO dto = new ChannelDTO();

            dto.name = name;
            dto.extension = rountingKey;
            dto.connected = isConnected;

            return dto;
        }
    }

    public SubscriptionDTO[] getSubscriptionDTOs() {
        final List<ChannelDTO> subChannels = subscriptionChannels();
        return subChannels.stream()
                          .map(this::getSubscriptionDTO)
                          .toArray(SubscriptionDTO[]::new);
    }

    private List<ChannelDTO> subscriptionChannels() {
        return subscriptions.values()
                            .stream()
                            .map(c -> c.subChannel)
                            .collect(toList());
    }

    public ReplyToSubscriptionDTO[] getReplyToSubscriptionDTOs() {
        final List<ReplyToSubscriptionDTO> replyToSubscriptions = new ArrayList<>();

        for (final Entry<String, ExtendedSubscriptionDTO> dto : subscriptions.entrySet()) {

            final ExtendedSubscriptionDTO subscription = dto.getValue();
            if (subscription.handlerReference != null) {

                final ReplyToSubscriptionDTO replyToSub =
                        getReplyToSubscriptionDTO(
                                subscription.pubChannel,
                                subscription.subChannel,
                                subscription.handlerReference);

                replyToSubscriptions.add(replyToSub);
            }
        }
        return replyToSubscriptions.toArray(new ReplyToSubscriptionDTO[0]);
    }

    private SubscriptionDTO getSubscriptionDTO(final ChannelDTO channelDTO) {
        final SubscriptionDTO subscriptionDTO = new SubscriptionDTO();

        subscriptionDTO.serviceDTO = getDTOFromClass(MessageSubscription.class, bundleContext);
        subscriptionDTO.channel = channelDTO;

        return subscriptionDTO;
    }

    private ReplyToSubscriptionDTO getReplyToSubscriptionDTO(
            final ChannelDTO pubDTO,
            final ChannelDTO subDTO,
            final ServiceReference<?> handlerReference) {

        final ReplyToSubscriptionDTO subscriptionDTO = new ReplyToSubscriptionDTO();

        subscriptionDTO.requestChannel = subDTO;
        subscriptionDTO.responseChannel = pubDTO;
        subscriptionDTO.handlerService = serviceReferenceDTO(handlerReference, bundleContext.getBundle().getBundleId());
        subscriptionDTO.serviceDTO = getDTOFromClass(MessageSubscription.class, bundleContext);
        subscriptionDTO.generateCorrelationId = true;
        subscriptionDTO.generateReplyChannel = true;

        return subscriptionDTO;
    }

    private String toTopicFilter(final String subChannel) {
        final int indexOfFirstSlash = subChannel.indexOf('/');
        return indexOfFirstSlash != -1 ? subChannel.substring(0, indexOfFirstSlash) + "/#" : subChannel;
    }

    private boolean isUnsubscriptionAcknowledged(final Mqtt5UnsubAck ack) {
        final List<Mqtt5UnsubAckReasonCode> acceptedCodes = Arrays.asList(
                SUCCESS,
                NO_SUBSCRIPTIONS_EXISTED
        );
        final List<Mqtt5UnsubAckReasonCode> reasonCodes = ack.getReasonCodes();
        return reasonCodes.stream().findFirst().filter(acceptedCodes::contains).isPresent();
    }

}
