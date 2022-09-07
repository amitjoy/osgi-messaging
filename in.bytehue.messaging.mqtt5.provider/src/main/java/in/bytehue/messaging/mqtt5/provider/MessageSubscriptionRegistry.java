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
import java.util.concurrent.ConcurrentHashMap;

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

@Component(service = MessageSubscriptionRegistry.class)
public final class MessageSubscriptionRegistry {

	@Activate
	private BundleContext bundleContext;

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private MessageClientProvider messagingClient;

	// topic as key
	private final Map<String, ExtendedSubscriptionDTO> subscriptions = new ConcurrentHashMap<>();

	public boolean hasSubscription(final String channel) {
		return subscriptions.containsKey(channel);
	}

	public ExtendedSubscriptionDTO getSubscription(final String channel) {
		return subscriptions.get(channel);
	}

	public void addSubscription(final String pubChannel, final String subChannel, final PushStream<Message> pushStream,
			final ServiceReference<?> handlerReference, final boolean isReplyToSubscription) {
		subscriptions.computeIfAbsent(subChannel, entry -> new ExtendedSubscriptionDTO(pubChannel, subChannel,
				pushStream, handlerReference, isReplyToSubscription));
	}

	public boolean removeSubscription(final String subChannel) {
		if (subscriptions.containsKey(subChannel)) {
			messagingClient.client.unsubscribeWith().addTopicFilter(subChannel).send().thenAccept(ack -> {
				if (isUnsubscriptionAcknowledged(ack)) {
					subscriptions.remove(subChannel);
					logger.debug("Unsubscription request for '{}' processed successfully - {}", subChannel, ack);
				} else {
					logger.error("Unsubscription request for '{}' failed - {}", subChannel, ack);
				}
			});
			return true;
		}
		return false;
	}

	@Deactivate
	public void clearAllSubscriptions() {
		subscriptions.keySet().forEach(this::removeSubscription);
	}

	static class ExtendedSubscriptionDTO {

		boolean isReplyToSub;
		ChannelDTO pubChannel;
		ChannelDTO subChannel;
		PushStream<Message> connectedStream;
		ServiceReference<?> handlerReference;

		public ExtendedSubscriptionDTO(final String pubChannel, final String subChannel,
				final PushStream<Message> connectedStream, final ServiceReference<?> handlerReference,
				final boolean isReplyToSub) {

			// a channel is connected if a subscription in client exists
			final boolean isConnected = true;

			this.pubChannel = createChannelDTO(pubChannel, isConnected);
			this.subChannel = createChannelDTO(subChannel, isConnected);
			this.connectedStream = connectedStream;
			this.handlerReference = handlerReference;
			this.isReplyToSub = isReplyToSub;
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
		return subChannels.stream().map(this::getSubscriptionDTO).toArray(SubscriptionDTO[]::new);
	}

	private List<ChannelDTO> getSubscriptionChannelDTOs() {
		return subscriptions.values().stream().filter(c -> !c.isReplyToSub).map(c -> c.subChannel).collect(toList());
	}

	public ReplyToSubscriptionDTO[] getReplyToSubscriptionDTOs() {
		final List<ReplyToSubscriptionDTO> replyToSubscriptions = new ArrayList<>();

		for (final Entry<String, ExtendedSubscriptionDTO> entry : subscriptions.entrySet()) {
			final ExtendedSubscriptionDTO sub = entry.getValue();
			if (sub.isReplyToSub) {
				final ReplyToSubscriptionDTO replyToSub = getReplyToSubscriptionDTO(sub.pubChannel, sub.subChannel,
						sub.handlerReference);
				replyToSubscriptions.add(replyToSub);
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

	private ReplyToSubscriptionDTO getReplyToSubscriptionDTO(final ChannelDTO pubDTO, final ChannelDTO subDTO,
			final ServiceReference<?> handlerReference) {

		final ReplyToSubscriptionDTO subscriptionDTO = new ReplyToSubscriptionDTO();

		subscriptionDTO.requestChannel = subDTO;
		subscriptionDTO.responseChannel = pubDTO;
		subscriptionDTO.handlerService = toServiceReferenceDTO(handlerReference);
		subscriptionDTO.serviceDTO = toServiceReferenceDTO(MessageSubscriptionProvider.class, bundleContext);
		subscriptionDTO.generateCorrelationId = false;
		subscriptionDTO.generateReplyChannel = false;

		return subscriptionDTO;
	}

	private boolean isUnsubscriptionAcknowledged(final Mqtt5UnsubAck ack) {
		final List<Mqtt5UnsubAckReasonCode> acceptedCodes = Arrays.asList(SUCCESS, NO_SUBSCRIPTIONS_EXISTED);
		final List<Mqtt5UnsubAckReasonCode> reasonCodes = ack.getReasonCodes();
		return reasonCodes.stream().findFirst().filter(acceptedCodes::contains).isPresent();
	}

}
