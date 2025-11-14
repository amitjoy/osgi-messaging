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

import static com.hivemq.client.mqtt.MqttClientState.CONNECTED;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

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
import in.bytehue.messaging.mqtt5.provider.helper.LogHelper;
import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

@Designate(ocd = RegistryConfig.class)
@EventTopics(MQTT_CLIENT_DISCONNECTED_EVENT_TOPIC)
@Component(service = { EventHandler.class, MessageSubscriptionRegistry.class })
public final class MessageSubscriptionRegistry implements EventHandler {

	@ObjectClassDefinition(name = "MQTT 5.0 Messaging Subscription Registry Configuration", description = "This configuration is used to configure the MQTT 5.0 messaging subscription registry")
	public @interface RegistryConfig {
		@AttributeDefinition(name = "Clear existing subscriptions on disconnect", description = "Remove the existing subscriptions whenever the client gets disconnected.")
		boolean clearSubscriptionsOnDisconnect() default true;

		@AttributeDefinition(name = "Number of threads for the internal thread pool", description = "Number of threads in the fixed pool for handling unsubscribe tasks.", min = "1")
		int numThreads() default 5;

		@AttributeDefinition(name = "Prefix of the thread name")
		String threadNamePrefix() default "mqtt-registry-unsubscribe";

		@AttributeDefinition(name = "Suffix of the thread name (supports only {@code %d} format specifier)")
		String threadNameSuffix() default "-%d";

		@AttributeDefinition(name = "Flag to set if the threads will be daemon threads")
		boolean isDaemon() default true;
	}

	private volatile RegistryConfig config;

	@Activate
	private BundleContext bundleContext;

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private LogMirrorService logMirror;

	@Reference
	private MessageClientProvider messagingClient;

	private LogHelper logHelper;
	private ExecutorService unsubscribeExecutor;

	// topic as outer map's key and subscription id as internal map's key
	// there can be multiple subscriptions for a single topic
	final Map<String, Map<String, ExtendedSubscription>> subscriptions = new ConcurrentHashMap<>();

	@Activate
	@Modified
	void init(final RegistryConfig config) {
		this.config = config;
		this.logHelper = new LogHelper(logger, logMirror);
		// (Re)create the executor
		if (unsubscribeExecutor != null) {
			unsubscribeExecutor.shutdownNow();
		}
		//@formatter:off
        final ThreadFactory threadFactory =
                new ThreadFactoryBuilder()
                        .setThreadFactoryName(config.threadNamePrefix())
                        .setThreadNameFormat(config.threadNameSuffix())
                        .setDaemon(config.isDaemon())
                        .build();
        //@formatter:on
		unsubscribeExecutor = Executors.newFixedThreadPool(config.numThreads(), threadFactory);
		logHelper.info("Messaging subscription registry has been activated/modified");
	}

	/**
	 * Adds a new subscription to the registry. This method is synchronized to
	 * prevent race conditions with clearAllSubscriptions.
	 * 
	 * <p>
	 * <b>Acceptable Race Window:</b> When a subscription is added,
	 * {@code isAcknowledged} is initially {@code false} and will be set to
	 * {@code true} asynchronously when the broker acknowledges the subscription
	 * (typically within milliseconds to seconds, bounded by the configured
	 * timeout). If {@code getSubscriptionDTOs()} or
	 * {@code getReplyToSubscriptionDTOs()} is called during this window, the
	 * subscription may be excluded from the DTO snapshot. This is acceptable
	 * because:
	 * <ul>
	 * <li>DTOs represent informational snapshots, not authoritative state</li>
	 * <li>The race window is small and bounded by the subscription timeout</li>
	 * <li>The issue is self-correcting on the next DTO retrieval</li>
	 * <li>The subscription itself is fully active and processing messages
	 * correctly</li>
	 * </ul>
	 * </p>
	 */
	public synchronized ExtendedSubscription addSubscription(final String subChannel, final String pubChannel,
			final int qos, final Runnable connectedStreamCloser, final boolean isReplyToSub,
			final long ownerServiceId) {
		final ExtendedSubscription sub = new ExtendedSubscription(subChannel, pubChannel, qos, connectedStreamCloser,
				isReplyToSub, ownerServiceId);
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
				logHelper.info("Removed subscription from '{}' successfully", channel);
			}

			if (existingSubscriptions.isEmpty()) {
				subscriptions.remove(channel);
				logHelper.info("Removed the last subscription from '{}' successfully", channel);
				// Signal to the caller that the last subscriber is gone
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all subscriptions for a given topic and closes their streams. This is
	 * the FAST, state-only removal method. It is synchronized to be thread-safe.
	 * 
	 * <p>
	 * <b>Reentrant Synchronization Note:</b> This method makes a reentrant
	 * synchronized call. When {@code connectedStreamCloser.run()} is invoked (which
	 * triggers {@code stream.close()}), it eventually calls back into
	 * {@code removeSubscription(channel, id)}. Since Java's {@code synchronized} is
	 * reentrant, the same thread can reacquire this lock. However, the channel is
	 * already removed from the map at line 176, so the reentrant call finds nothing
	 * and returns immediately. This design is intentional for bulk cleanup
	 * scenarios (e.g., client disconnect) where individual MQTT unsubscribe packets
	 * are unnecessary.
	 * </p>
	 */
	public synchronized void removeSubscription(final String channel) {
		final Map<String, ExtendedSubscription> exisitngSubscriptions = subscriptions.remove(channel);
		if (exisitngSubscriptions != null) {
			exisitngSubscriptions.forEach((k, v) -> v.connectedStreamCloser.run());
			logHelper.info("Removed all subscriptions from '{}' successfully", channel);
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
	 * Checks if there is any active subscription for the given channel. Fast,
	 * non-locking, thread-safe read from ConcurrentHashMap.
	 */
	public boolean hasActiveSubscription(final String channel) {
		final Map<String, ExtendedSubscription> existingSubscriptions = subscriptions.get(channel);
		return existingSubscriptions != null && !existingSubscriptions.isEmpty();
	}

	/**
	 * Checks if there is an active subscription for the given channel owned by a
	 * specific service. Fast, non-locking, thread-safe read.
	 */
	public boolean hasActiveSubscription(final String channel, final long serviceId) {
		final Map<String, ExtendedSubscription> existingSubscriptions = subscriptions.get(channel);
		if (existingSubscriptions == null || existingSubscriptions.isEmpty()) {
			return false;
		}
		// Check if any of the subscriptions for this channel are owned by the specific
		// service
		return existingSubscriptions.values().stream().anyMatch(sub -> sub.ownerServiceId == serviceId);
	}

	/**
	 * Sends a blocking UNSUBSCRIBE packet to the broker. This method BLOCKS THE
	 * CALLER, and holds the component-wide lock.
	 *
	 * <p>
	 * This method MUST NOT modify the local subscription registry. The local state
	 * is managed by the {@code onClose} handler in
	 * {@link MessageSubscriptionProvider}. This method's only job is to send the
	 * network packet and log the result.
	 * </p>
	 */
	public synchronized void unsubscribeSubscription(final String subChannel) {
		// This method is called asynchronously. We must re-check the subscription
		// state to prevent the A-B-A race.
		final Map<String, ExtendedSubscription> currentSubs = subscriptions.get(subChannel);
		if (currentSubs != null && !currentSubs.isEmpty()) {
			// A new subscriber (S2) has arrived before this (S1) async task
			// could run. We must CANCEL the unsubscribe packet.
			logHelper.info("Cancelling stale unsubscribe for '{}', a new subscriber has joined.", subChannel);
			return;
		}
		try {
			// Time-of-Check to Time-of-Use (TOCTOU) race condition that can occur during
			// bundle startup or client reconfiguration
			final Mqtt5AsyncClient currentClient = messagingClient.client; // Read volatile field ONCE

			if (currentClient == null) {
				logHelper.warn(
						"Cannot unsubscribe from '{}' - client not yet initialized (likely during startup/shutdown)",
						subChannel);
				// Do not throw, just log. The local stream is already closed.
				return;
			}
			final MqttClientState clientState = currentClient.getState();
			if (clientState != CONNECTED) {
				logHelper.warn("Cannot unsubscribe from '{}' - client state is {} (likely during reconnection)",
						subChannel, clientState);
				// Do not throw, just log. The local stream is already closed.
				return;
			}
			// Block for max 2 seconds
			final Mqtt5UnsubAck ack = currentClient.unsubscribeWith().addTopicFilter(subChannel).send().get(2, SECONDS);

			if (isUnsubscriptionAcknowledged(ack)) {
				logHelper.info("Unsubscription request for '{}' processed successfully - {}", subChannel, ack);
			} else {
				logHelper.error("Unsubscription request for '{}' failed - {}", subChannel, ack);
			}
		} catch (final Exception e) {
			logHelper.error("Unsubscription for '{}' failed with exception", subChannel, e);
		}
	}

	@Deactivate
	void deactivate() {
		clearAllSubscriptions();
		// Shut down our executor AFTER streams are closed
		if (unsubscribeExecutor != null) {
			unsubscribeExecutor.shutdownNow();
		}
		logHelper.info("Messaging subscription registry has been deactivated");
	}

	public ExecutorService getUnsubscribeExecutor() {
		return unsubscribeExecutor;
	}

	/**
	 * Clears all subscriptions during component deactivation. This method is
	 * synchronized to prevent a race with addSubscription. It is non-blocking and
	 * fast, as it only performs in-memory cleanup.
	 */
	public synchronized void clearAllSubscriptions() {
		// if the client library like HiveMQ supports automatic resubscription, this can
		// be disabled through config
		if (!config.clearSubscriptionsOnDisconnect()) {
			logHelper.info("Skipping subscription clear on disconnect (clearSubscriptionsOnDisconnect=false)");
			return;
		}
		// Iterate a snapshot of the keys to avoid ConcurrentModificationException
		// while removeSubscription(channel) modifies the map.
		final List<String> topics = new ArrayList<>(subscriptions.keySet());

		// Call the FAST, non-blocking, SYNCHRONIZED removeSubscription(channel)
		// This safely cleans up all internal streams without network I/O.
		topics.forEach(this::removeSubscription);
		logHelper.info("Messaging subscription registry has been cleaned");
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
				if (!sub.isReplyToSub && sub.isAcknowledged.get()) {
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
				if (sub.isReplyToSub && sub.isAcknowledged.get()) {
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
		clearAllSubscriptions();
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

		final int qos;
		final String id;
		final long ownerServiceId;
		final boolean isReplyToSub;
		final ChannelDTO subChannel;
		final AtomicBoolean isAcknowledged;
		final Runnable connectedStreamCloser;
		final Map<String, ChannelDTO> pubChannels = new ConcurrentHashMap<>();

		ServiceReferenceDTO handlerReference;

		private ExtendedSubscription(final String subChannel, final String pubChannel, final int qos,
				final Runnable connectedStreamCloser, final boolean isReplyToSub, final long ownerServiceId) {
			id = UUID.randomUUID().toString();
			this.qos = qos;
			this.connectedStreamCloser = connectedStreamCloser;
			this.subChannel = createChannelDTO(subChannel);
			this.isReplyToSub = isReplyToSub;
			this.ownerServiceId = ownerServiceId;
			this.isAcknowledged = new AtomicBoolean(false);
			if (pubChannel != null) {
				pubChannels.put(pubChannel, createChannelDTO(pubChannel));
			}
		}

		public void setAcknowledged(final boolean isAcknowledged) {
			this.isAcknowledged.set(isAcknowledged);
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