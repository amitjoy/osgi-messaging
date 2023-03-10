/*******************************************************************************
 * Copyright 2020-2023 Amit Kumar Mondal
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;

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

	// topic as outer map's key and subscription id as internal map's key
	// there can be multiple subscriptions for a single topic
	private final Map<String, Map<String, ExtendedSubscription>> subscriptions = new ConcurrentHashMap<>();

	public synchronized ExtendedSubscription addSubscription(final String subChannel, final String pubChannel,
			final Runnable connectedStreamCloser, final boolean isReplyToSub) {
		final ExtendedSubscription sub = new ExtendedSubscription(subChannel, pubChannel, connectedStreamCloser,
				isReplyToSub);
		subscriptions.computeIfAbsent(subChannel, c -> new ConcurrentHashMap<>()).put(sub.id, sub);
		return sub;
	}

	public synchronized void removeSubscription(final String channel, final String id) {
		final Map<String, ExtendedSubscription> existingSubscriptions = subscriptions.get(channel);
		if (existingSubscriptions == null || existingSubscriptions.isEmpty()) {
			unsubscribeSubscription(channel);
			return;
		}
		final ExtendedSubscription existingSubscription = existingSubscriptions.remove(id);
		if (existingSubscription != null) {
			existingSubscription.connectedStreamCloser.run();
		}
	}

	public synchronized void removeSubscription(final String channel) {
		final Map<String, ExtendedSubscription> exisitngSubscriptions = subscriptions.remove(channel);
		if (exisitngSubscriptions != null) {
			exisitngSubscriptions.forEach((k, v) -> v.connectedStreamCloser.run());
		}
	}

	public synchronized ExtendedSubscription getSubscription(final String channel, final String id) {
		final Map<String, ExtendedSubscription> existingSubscriptions = subscriptions.get(channel);
		return existingSubscriptions != null ? existingSubscriptions.get(id) : null;
	}

	public synchronized void unsubscribeSubscription(final String subChannel) {
		messagingClient.client.unsubscribeWith().addTopicFilter(subChannel).send().thenAccept(ack -> {
			if (isUnsubscriptionAcknowledged(ack)) {
				removeSubscription(subChannel);
				logger.debug("Unsubscription request for '{}' processed successfully - {}", subChannel, ack);
			} else {
				logger.error("Unsubscription request for '{}' failed - {}", subChannel, ack);
			}
		});
	}

	@Deactivate
	public void clearAllSubscriptions() {
		subscriptions.keySet().stream().forEach(this::unsubscribeSubscription);
	}

	public synchronized SubscriptionDTO[] getSubscriptionDTOs() {
		final List<ChannelDTO> subChannels = getSubscriptionChannelDTOs();
		return subChannels.stream().map(this::getSubscriptionDTO).toArray(SubscriptionDTO[]::new);
	}

	public synchronized ReplyToSubscriptionDTO[] getReplyToSubscriptionDTOs() {
		final List<ReplyToSubscriptionDTO> replyToSubscriptions = new ArrayList<>();

		for (final Entry<String, Map<String, ExtendedSubscription>> entry : subscriptions.entrySet()) {
			for (final Entry<String, ExtendedSubscription> e : entry.getValue().entrySet()) {
				final ExtendedSubscription sub = e.getValue();
				if (sub.isReplyToSub && sub.isAcknowledged) {
					if (sub.pubChannels.isEmpty()) {
						// true for ReplyToSubscrriptionHandlers
						final ReplyToSubscriptionDTO replyToSub = getReplyToSubscriptionDTO(null, sub.subChannel,
								sub.handlerReference);
						replyToSubscriptions.add(replyToSub);
					} else {
						for (final Entry<String, ChannelDTO> pubEntry : sub.pubChannels.entrySet()) {
							final ReplyToSubscriptionDTO replyToSub = getReplyToSubscriptionDTO(pubEntry.getValue(),
									sub.subChannel, sub.handlerReference);
							replyToSubscriptions.add(replyToSub);
						}
					}
				}
			}
		}
		return replyToSubscriptions.toArray(new ReplyToSubscriptionDTO[0]);
	}

	private List<ChannelDTO> getSubscriptionChannelDTOs() {
		final List<ChannelDTO> channels = new ArrayList<>();
		for (final Entry<String, Map<String, ExtendedSubscription>> entry : subscriptions.entrySet()) {
			for (final Entry<String, ExtendedSubscription> e : entry.getValue().entrySet()) {
				final ExtendedSubscription sub = e.getValue();
				if (!sub.isReplyToSub && sub.isAcknowledged) {
					channels.add(sub.subChannel);
				}
			}
		}
		return channels;
	}

	private SubscriptionDTO getSubscriptionDTO(final ChannelDTO channelDTO) {
		final SubscriptionDTO subscriptionDTO = new SubscriptionDTO();

		subscriptionDTO.serviceDTO = toServiceReferenceDTO(MessageSubscriptionProvider.class, bundleContext);
		subscriptionDTO.channel = channelDTO;

		return subscriptionDTO;
	}

	private ReplyToSubscriptionDTO getReplyToSubscriptionDTO(final ChannelDTO pubDTO, final ChannelDTO subDTO,
			final ServiceReferenceDTO handlerReference) {

		final ReplyToSubscriptionDTO subscriptionDTO = new ReplyToSubscriptionDTO();

		subscriptionDTO.requestChannel = subDTO;
		subscriptionDTO.responseChannel = pubDTO;
		subscriptionDTO.handlerService = handlerReference;
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

	static class ExtendedSubscription {

		String id;
		volatile boolean isAcknowledged;
		volatile boolean isReplyToSub;
		ChannelDTO subChannel;
		Runnable connectedStreamCloser;
		ServiceReferenceDTO handlerReference;
		Map<String, ChannelDTO> pubChannels = new ConcurrentHashMap<>();

		private ExtendedSubscription(final String subChannel, final String pubChannel,
				final Runnable connectedStreamCloser, final boolean isReplyToSub) {
			id = UUID.randomUUID().toString();
			this.connectedStreamCloser = connectedStreamCloser;
			this.subChannel = createChannelDTO(subChannel);
			this.isReplyToSub = isReplyToSub;
			if (pubChannel != null) {
				pubChannels.put(pubChannel, createChannelDTO(pubChannel));
			}
		}

		public synchronized void setAcknowledged(final boolean isAcknowledged) {
			this.isAcknowledged = isAcknowledged;
		}

		public synchronized void updateReplyToHandlerSubscription(final String pubChannel,
				final ServiceReference<?> handlerReference) {
			this.handlerReference = toServiceReferenceDTO(handlerReference);
			pubChannels.put(pubChannel, createChannelDTO(pubChannel));
		}

		private ChannelDTO createChannelDTO(final String name) {
			if (name == null) {
				return null;
			}
			final String rountingKey = null; // no routing key for MQTT
			final ChannelDTO dto = new ChannelDTO();

			// a channel is connected if a subscription in client exists
			final boolean isConnected = true;

			dto.name = name;
			dto.extension = rountingKey;
			dto.connected = isConnected;

			return dto;
		}
	}

}
