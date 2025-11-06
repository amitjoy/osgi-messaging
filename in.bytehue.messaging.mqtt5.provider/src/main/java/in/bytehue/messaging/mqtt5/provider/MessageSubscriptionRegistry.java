/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
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

import static com.hivemq.client.mqtt.MqttClientState.DISCONNECTED;
import static com.hivemq.client.mqtt.MqttClientState.DISCONNECTED_RECONNECT;
import static com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode.NO_SUBSCRIPTIONS_EXISTED;
import static com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode.SUCCESS;
import static in.bytehue.messaging.mqtt5.provider.MessageClientProvider.MQTT_CLIENT_DISCONNECTED_EVENT_TOPIC;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.toServiceReferenceDTO;
import static java.util.concurrent.TimeUnit.SECONDS;

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
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAck;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAckReasonCode;

import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionRegistry.RegistryConfig;

@Designate(ocd = RegistryConfig.class)
@EventTopics(MQTT_CLIENT_DISCONNECTED_EVENT_TOPIC)
@Component(service = { EventHandler.class, MessageSubscriptionRegistry.class })
public final class MessageSubscriptionRegistry implements EventHandler {

	@ObjectClassDefinition(name = "MQTT 5.0 Messaging Subscription Registry Configuration", description = "This configuration is used to configure the MQTT 5.0 messaging subscription registry")
	public @interface RegistryConfig {
		@AttributeDefinition(name = "Clear existing subscriptions on disconnect", description = "Remove the existing subscriptions whenever the client gets disconnected.")
		boolean clearSubscriptionsOnDisconnect() default true;
	}

	private volatile RegistryConfig config;

	@Activate
	private BundleContext bundleContext;

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private MessageClientProvider messagingClient;

	// topic as outer map's key and subscription id as internal map's key
	// there can be multiple subscriptions for a single topic
	private final Map<String, Map<String, ExtendedSubscription>> subscriptions = new ConcurrentHashMap<>();

	@Activate
	@Modified
	void init(final RegistryConfig config) {
		this.config = config;
		logger.info("Messaging subscription registry has been activated/modified");
	}

	/**
	 * Adds a new subscription to the registry. This method is synchronized to
	 * prevent race conditions with clearAllSubscriptions.
	 */
	public synchronized ExtendedSubscription addSubscription(final String subChannel, final String pubChannel, int qos,
			final Runnable connectedStreamCloser, final boolean isReplyToSub) {
		final ExtendedSubscription sub = new ExtendedSubscription(subChannel, pubChannel, qos, connectedStreamCloser,
				isReplyToSub);
		subscriptions.computeIfAbsent(subChannel, c -> new ConcurrentHashMap<>()).put(sub.id, sub);
		return sub;
	}

	/**
	 * Removes a single subscription by its ID. This method is synchronized to be
	 * atomic and thread-safe. It is non-blocking as it only performs in-memory
	 * operations.
	 *
	 * @return true if this was the last subscription for the channel, false
	 *         otherwise.
	 */
	public synchronized boolean removeSubscription(final String channel, final String id) {
		final Map<String, ExtendedSubscription> existingSubscriptions = subscriptions.get(channel);

		if (existingSubscriptions != null) {
			final ExtendedSubscription existingSubscription = existingSubscriptions.remove(id);
			if (existingSubscription != null) {
				existingSubscription.connectedStreamCloser.run();
				logger.info("Removed subscription from '{}' successfully", channel);
			}

			if (existingSubscriptions.isEmpty()) {
				subscriptions.remove(channel);
				logger.info("Removed the last subscription from '{}' successfully", channel);
				// Signal to the caller that the last subscriber is gone
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all subscriptions for a given topic and closes their streams. This is
	 * the FAST, state-only removal method. It is synchronized to be thread-safe.
	 */
	public synchronized void removeSubscription(final String channel) {
		final Map<String, ExtendedSubscription> exisitngSubscriptions = subscriptions.remove(channel);
		if (exisitngSubscriptions != null) {
			exisitngSubscriptions.forEach((k, v) -> v.connectedStreamCloser.run());
			logger.info("Removed all subscriptions from '{}' successfully", channel);
		}
	}

	/**
	 * Retrieves a subscription by its channel and ID. Fast, non-locking,
	 * thread-safe read from ConcurrentHashMap.
	 */
	public ExtendedSubscription getSubscription(final String channel, final String id) {
		final Map<String, ExtendedSubscription> existingSubscriptions = subscriptions.get(channel);
		return existingSubscriptions != null ? existingSubscriptions.get(id) : null;
	}

	/**
	 * Sends a blocking UNSUBSCRIBE packet to the broker. This method BLOCKS THE
	 * CALLER, but holds no component-wide lock.
	 *
	 * <p>
	 * This method MUST NOT modify the local subscription registry. The local state
	 * is managed by the {@code onClose} handler in
	 * {@link MessageSubscriptionProvider}. This method's only job is to send the
	 * network packet and log the result.
	 * </p>
	 */
	public void unsubscribeSubscription(final String subChannel) {
		try {
			// Time-of-Check to Time-of-Use (TOCTOU) race condition that can occur during
			// bundle startup or client reconfiguration
			final Mqtt5AsyncClient currentClient = messagingClient.client; // Read volatile field ONCE

			if (currentClient == null) {
				logger.error("Cannot unsubscribe from '{}' since the client is not yet initialized", subChannel);
				// Do not throw, just log. The local stream is already closed.
				return;
			}
			final MqttClientState clientState = currentClient.getState();
			if (clientState == DISCONNECTED || clientState == DISCONNECTED_RECONNECT) {
				logger.error("Cannot unsubscribe from '{}' since the client is disconnected", subChannel);
				// Do not throw, just log. The local stream is already closed.
				return;
			}
			// Block for max 2 seconds
			final Mqtt5UnsubAck ack = currentClient.unsubscribeWith().addTopicFilter(subChannel).send().get(2, SECONDS);

			if (isUnsubscriptionAcknowledged(ack)) {
				logger.info("Unsubscription request for '{}' processed successfully - {}", subChannel, ack);
			} else {
				logger.error("Unsubscription request for '{}' failed - {}", subChannel, ack);
			}
		} catch (final Exception e) {
			logger.error("Unsubscription for '{}' failed with exception", subChannel, e);
		}
	}

	/**
	 * Clears all subscriptions during component deactivation. This method is
	 * synchronized to prevent a race with addSubscription. It is non-blocking and
	 * fast, as it only performs in-memory cleanup.
	 */
	@Deactivate
	public synchronized void clearAllSubscriptions() {
		// Iterate a snapshot of the keys to avoid ConcurrentModificationException
		// while removeSubscription(channel) modifies the map.
		final List<String> topics = new ArrayList<>(subscriptions.keySet());

		// Call the FAST, non-blocking, SYNCHRONIZED removeSubscription(channel)
		// This safely cleans up all internal streams without network I/O.
		topics.forEach(this::removeSubscription);
		logger.info("Messaging subscription registry has been cleaned");
	}

	/**
	 * DTO methods need to lock to get a consistent snapshot for iteration. This is
	 * fast and non-blocking, so it's safe.
	 */
	public synchronized SubscriptionDTO[] getSubscriptionDTOs() {
		final List<SubscriptionDTO> subscriptionDTOs = new ArrayList<>();
		for (final Entry<String, Map<String, ExtendedSubscription>> entry : subscriptions.entrySet()) {
			for (final Entry<String, ExtendedSubscription> e : entry.getValue().entrySet()) {
				final ExtendedSubscription sub = e.getValue();
				if (!sub.isReplyToSub && sub.isAcknowledged) {
					subscriptionDTOs.add(getSubscriptionDTO(sub));
				}
			}
		}
		return subscriptionDTOs.toArray(new SubscriptionDTO[0]);
	}

	/**
	 * DTO methods need to lock to get a consistent snapshot for iteration. This is
	 * fast and non-blocking, so it's safe.
	 */
	public synchronized ReplyToSubscriptionDTO[] getReplyToSubscriptionDTOs() {
		final List<ReplyToSubscriptionDTO> replyToSubscriptions = new ArrayList<>();

		for (final Entry<String, Map<String, ExtendedSubscription>> entry : subscriptions.entrySet()) {
			for (final Entry<String, ExtendedSubscription> e : entry.getValue().entrySet()) {
				final ExtendedSubscription sub = e.getValue();
				if (sub.isReplyToSub && sub.isAcknowledged) {
					if (sub.pubChannels.isEmpty()) {
						// true for ReplyToSubscriptionHandlers
						final ReplyToSubscriptionDTO replyToSub = getReplyToSubscriptionDTO(sub, null);
						replyToSubscriptions.add(replyToSub);
					} else {
						for (final Entry<String, ChannelDTO> pubEntry : sub.pubChannels.entrySet()) {
							final ReplyToSubscriptionDTO replyToSub = getReplyToSubscriptionDTO(sub,
									pubEntry.getValue());
							replyToSubscriptions.add(replyToSub);
						}
					}
				}
			}
		}
		return replyToSubscriptions.toArray(new ReplyToSubscriptionDTO[0]);
	}

	@Override
	public void handleEvent(Event event) {
		// if the client library like HiveMQ supports automatic resubscription, this can
		// be disabled through config
		if (config.clearSubscriptionsOnDisconnect()) {
			clearAllSubscriptions();
		}
	}

	private SubscriptionDTO getSubscriptionDTO(final ExtendedSubscription sub) {
		final SubscriptionDTO subscriptionDTO = new SubscriptionDTO();

		subscriptionDTO.serviceDTO = toServiceReferenceDTO(MessageSubscriptionProvider.class, bundleContext);
		subscriptionDTO.channel = sub.subChannel;
		subscriptionDTO.qos = sub.qos;

		return subscriptionDTO;
	}

	private ReplyToSubscriptionDTO getReplyToSubscriptionDTO(ExtendedSubscription sub, final ChannelDTO pubDTO) {

		final ReplyToSubscriptionDTO subscriptionDTO = new ReplyToSubscriptionDTO();

		subscriptionDTO.requestChannel = sub.subChannel;
		subscriptionDTO.responseChannel = pubDTO;
		subscriptionDTO.handlerService = sub.handlerReference;
		subscriptionDTO.serviceDTO = toServiceReferenceDTO(MessageSubscriptionProvider.class, bundleContext);
		subscriptionDTO.generateCorrelationId = false;
		subscriptionDTO.generateReplyChannel = false;
		subscriptionDTO.qos = sub.qos;

		return subscriptionDTO;
	}

	private boolean isUnsubscriptionAcknowledged(final Mqtt5UnsubAck ack) {
		final List<Mqtt5UnsubAckReasonCode> acceptedCodes = Arrays.asList(SUCCESS, NO_SUBSCRIPTIONS_EXISTED);
		final List<Mqtt5UnsubAckReasonCode> reasonCodes = ack.getReasonCodes();
		return reasonCodes.stream().findFirst().filter(acceptedCodes::contains).isPresent();
	}

	static class ExtendedSubscription {

		int qos;
		String id;
		volatile boolean isAcknowledged;
		volatile boolean isReplyToSub;
		ChannelDTO subChannel;
		Runnable connectedStreamCloser;
		ServiceReferenceDTO handlerReference;
		Map<String, ChannelDTO> pubChannels = new ConcurrentHashMap<>();

		private ExtendedSubscription(final String subChannel, final String pubChannel, int qos,
				final Runnable connectedStreamCloser, final boolean isReplyToSub) {
			id = UUID.randomUUID().toString();
			this.qos = qos;
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