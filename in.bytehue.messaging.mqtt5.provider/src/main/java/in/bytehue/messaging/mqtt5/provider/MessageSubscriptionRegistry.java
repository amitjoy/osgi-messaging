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

import static com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode.NO_SUBSCRIPTIONS_EXISTED;
import static com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode.SUCCESS;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.toServiceReferenceDTO;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.util.pushstream.PushStream;

import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAck;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode;

//@formatter:off
@Component(service = MessageSubscriptionRegistry.class)
public final class MessageSubscriptionRegistry {

    @Activate
    private BundleContext bundleContext;

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private MessageClientProvider messagingClient;

    // wildcard topic filter as key
    private final Map<String, List<ExtendedSubscriptionDTO>> subscriptions = new ConcurrentHashMap<>();

    public void addSubscription(
            final String pubChannel,
            final String subChannel,
            final PushStream<Message> pushStream,
            final ServiceReference<?> handlerReference) {

        final String topicFilter = toTopicFilter(subChannel);
        final ExtendedSubscriptionDTO subDTO =  new ExtendedSubscriptionDTO(
                                                            pubChannel,
                                                            subChannel,
                                                            pushStream,
                                                            handlerReference);

        // there can be multiple subscriptions under the same wildcard filter
        if (subscriptions.containsKey(topicFilter)) {
            final List<ExtendedSubscriptionDTO> dtos = subscriptions.get(topicFilter);
            // there can be multiple connected streams for the same handler
            dtos.stream()
                .filter(dto -> dto.handlerReference == handlerReference)
                .forEach(dto -> dto.connectedStreams.add(pushStream));
            dtos.add(subDTO);
        } else {
            final List<ExtendedSubscriptionDTO> dtos = new ArrayList<>();
            dtos.add(subDTO);
            subscriptions.put(topicFilter, dtos);
        }
    }

    public void removeSubscription(final String subChannel) {
        removeSubscription(subChannel, ps -> true);
    }

    public void removeSubscription(final String subChannel, final Predicate<PushStream<Message>> predicate) {
        final String topicFilter = toTopicFilter(subChannel);

        // check if there exists the wildcard topic filter for the input subscription topic
        if (subscriptions.containsKey(topicFilter)) {
            final List<ExtendedSubscriptionDTO> dtos = subscriptions.get(topicFilter);

            for(final ExtendedSubscriptionDTO dto: dtos) {
                final List<PushStream<Message>> pushStreams = dto.connectedStreams;

                // only remove the input pushstream from the list of associated pushstreams as
                // multiple pushstreams from the same handler can exist simultaneously
                pushStreams.removeIf(predicate::test);

                // if there exists no active pushstreams, the channels are disconnected since
                // messages will never arrive to these topics until further subscription
                if (!pushStreams.isEmpty()) {
                    Optional.ofNullable(dto.pubChannel).ifPresent(e -> e.connected = false);
                    Optional.ofNullable(dto.subChannel).ifPresent(e -> e.connected = false);
                }
            }
            // remove only if there exists no connected streams
            dtos.removeIf(e -> e.connectedStreams.isEmpty());

            // if there exists no subscriptions associated with the wildcard entry, then send
            // an unsubscription request for the wildcard. Since we are using wildcard for the
            // unsubscription, the server will unsubscribe all registered topics under it.
            if (dtos.isEmpty()) {
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

    private static class ExtendedSubscriptionDTO {

        ChannelDTO pubChannel;
        ChannelDTO subChannel;
        ServiceReference<?> handlerReference;
        List<PushStream<Message>> connectedStreams;

        public ExtendedSubscriptionDTO(
                final String pubChannel,
                final String subChannel,
                final PushStream<Message> stream,
                final ServiceReference<?> handlerReference) {

            // a channel is connected if a subscription in client exists
            final boolean isConnected = true;

            this.pubChannel       = createChannelDTO(pubChannel, isConnected);
            this.subChannel       = createChannelDTO(subChannel, isConnected);
            this.handlerReference = handlerReference;

            connectedStreams = new CopyOnWriteArrayList<>();
            connectedStreams.add(stream);
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
        final List<ChannelDTO> subChannels = getSubscriptionChannelDTOs();
        return subChannels.stream()
                          .map(this::getSubscriptionDTO)
                          .toArray(SubscriptionDTO[]::new);
    }

    private List<ChannelDTO> getSubscriptionChannelDTOs() {
        return subscriptions.values()
                            .stream()
                            .flatMap(List::stream)
                            .filter(c -> c.handlerReference == null)
                            .map(c -> c.subChannel)
                            .collect(toList());
    }

    public ReplyToSubscriptionDTO[] getReplyToSubscriptionDTOs() {
        final List<ReplyToSubscriptionDTO> replyToSubscriptions = new ArrayList<>();

        for (final Entry<String, List<ExtendedSubscriptionDTO>> entry : subscriptions.entrySet()) {
            for (final ExtendedSubscriptionDTO dto : entry.getValue()) {
                if (dto.handlerReference != null) {
                    final ReplyToSubscriptionDTO replyToSub =
                            getReplyToSubscriptionDTO(
                                    dto.pubChannel,
                                    dto.subChannel,
                                    dto.handlerReference);
                    replyToSubscriptions.add(replyToSub);
                }
            }
        }
        return replyToSubscriptions.toArray(new ReplyToSubscriptionDTO[0]);
    }

    private SubscriptionDTO getSubscriptionDTO(final ChannelDTO channelDTO) {
        final SubscriptionDTO subscriptionDTO = new SubscriptionDTO();

        subscriptionDTO.serviceDTO = toServiceReferenceDTO(MessageSubscriptionProvider.class, bundleContext);
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
        subscriptionDTO.handlerService = toServiceReferenceDTO(handlerReference);
        subscriptionDTO.serviceDTO = toServiceReferenceDTO(MessageSubscriptionProvider.class, bundleContext);
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
