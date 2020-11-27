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

import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getDTOFromClass;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.serviceReferenceDTO;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.util.pushstream.PushStream;

@Component(service = MessageSubscriptionRegistry.class)
public final class MessageSubscriptionRegistry {

    @Activate
    private BundleContext bundleContext;

    @Reference
    private MessageClientProvider messagingClient;

    private final Map<PushStream<Message>, ExtendedSubscriptionDTO> subscriptions = new HashMap<>();

    public void addSubscription( //
            final PushStream<Message> stream, //
            final String pubChannel, //
            final String subChannel, //
            final ServiceReference<?> handlerReference) {
        subscriptions.put(stream, new ExtendedSubscriptionDTO(pubChannel, subChannel, handlerReference));
    }

    public void removeSubscription(final PushStream<Message> stream) {
        final ExtendedSubscriptionDTO channel = subscriptions.remove(stream);
        if (channel != null) {
            messagingClient.client.unsubscribeWith().addTopicFilter(channel.subChannel.name).send();
        }
    }

    public void clearAllSubscriptions() {
        subscriptions.keySet().forEach(this::removeSubscription);
    }

    private class ExtendedSubscriptionDTO {

        ChannelDTO pubChannel;
        ChannelDTO subChannel;
        ServiceReference<?> handlerReference;

        public ExtendedSubscriptionDTO( //
                final String pubChannel, //
                final String subChannel, //
                final ServiceReference<?> handlerReference) {

            final boolean isConnected = true; // a channel is always connected if a subscription exists
            this.pubChannel = createChannelDTO(pubChannel, isConnected);
            this.subChannel = createChannelDTO(subChannel, isConnected);
            this.handlerReference = handlerReference;
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
        // @formatter:off
        return subChannels.stream()
                          .map(this::getSubscriptionDTO)
                          .toArray(SubscriptionDTO[]::new);
        // @formatter:on
    }

    private List<ChannelDTO> subscriptionChannels() {
        // @formatter:off
        return subscriptions.values()
                            .stream()
                            .map(c -> c.subChannel)
                            .collect(toList());
        // @formatter:on
    }

    public ReplyToSubscriptionDTO[] getReplyToSubscriptionDTOs() {
        final List<ReplyToSubscriptionDTO> replyToSubscriptions = new ArrayList<>();

        for (final Entry<PushStream<Message>, ExtendedSubscriptionDTO> dto : subscriptions.entrySet()) {
            final ExtendedSubscriptionDTO subscription = dto.getValue();
            if (subscription.handlerReference != null) {
                // @formatter:off
                final ReplyToSubscriptionDTO replyToSub =
                        getReplyToSubscriptionDTO(
                                subscription.pubChannel,
                                subscription.subChannel,
                                subscription.handlerReference);
                // @formatter:on
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

    private ReplyToSubscriptionDTO getReplyToSubscriptionDTO( //
            final ChannelDTO pubDTO, //
            final ChannelDTO subDTO, //
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

}
