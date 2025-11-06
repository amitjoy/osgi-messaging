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
import static com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode.GRANTED_QOS_0;
import static com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode.GRANTED_QOS_1;
import static com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode.GRANTED_QOS_2;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MQTT_SUBSCRIPTION_EVENT_TOPIC_PREFIX;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.SUBSCRIBER;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttSubAckDTO.Type.FAILED;
import static in.bytehue.messaging.mqtt5.api.MqttSubAckDTO.Type.NO_ACK;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.acknowledgeMessage;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.adaptTo;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.addTopicPrefix;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getQoS;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.toMessage;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.Features.REPLY_TO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.converter.TypeReference;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAckReasonCode;

import in.bytehue.messaging.mqtt5.api.MqttSubAckDTO;
import in.bytehue.messaging.mqtt5.api.MqttSubAckDTO.Type;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionProvider.SubscriberConfig;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionRegistry.ExtendedSubscription;
import in.bytehue.messaging.mqtt5.provider.helper.InterruptSafe;
import in.bytehue.messaging.mqtt5.provider.helper.SubscriptionAck;

//@formatter:off
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                    EXTENSION_QOS,
                    RETAIN,
                    ACKNOWLEDGE,
                    RECEIVE_LOCAL
                  }
)
@Designate(ocd = SubscriberConfig.class)
@Component(service = {
                       MessageSubscription.class,
                       MessageSubscriptionProvider.class
                     },
           configurationPid = SUBSCRIBER
)
public final class MessageSubscriptionProvider implements MessageSubscription {

	@ObjectClassDefinition(
            name = "MQTT 5.0 Messaging Subscriber Configuration",
            description = "This configuration is used to configure the MQTT 5.0 messaging subscriber")
	public @interface SubscriberConfig {
		@AttributeDefinition(name = "Default timeout for synchronously subscribing to the broker", min = "5000")
		long timeoutInMillis() default 15_000L;

		@AttributeDefinition(name = "Default QoS for subscriptions unless specified", min = "0", max = "2")
        int qos() default 0;
	}

	private volatile SubscriberConfig config;

    @Activate
    private BundleContext bundleContext;

    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private ConverterAdapter converter;

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private MessageClientProvider messagingClient;

    @Reference
    private MessageSubscriptionRegistry subscriptionRegistry;

    @Reference
    private ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

    @Activate
    @Modified
    void init(final SubscriberConfig config) {
        this.config = config;
        logger.info("Messaging subscriber has been activated/updated");
    }

    @Deactivate
    void stop() {
        subscriptionRegistry.clearAllSubscriptions();
        logger.info("Messaging subscriber has been deactivated");
    }

    public SubscriberConfig config() {
        return config;
    }

    @Override
    public PushStream<Message> subscribe(final String subChannel) {
        return _subscribe(subChannel).stream();
    }

    @Override
    public PushStream<Message> subscribe(final MessageContext context) {
        return _subscribe(context).stream();
    }

    public SubscriptionAck _subscribe(final String subChannel) { //NOSONAR
    	requireNonNull(subChannel, "Channel cannot be null");
    	try {
    		MessageContext context = null;
            final MessageContextBuilderProvider builder = mcbFactory.getService();
            try {
            	context = builder.channel(subChannel).buildContext();
            	return _subscribe(context);
            } finally {
                mcbFactory.ungetService(builder);
            }
        }
    	catch (final Exception e) { //NOSONAR
    		logger.error("Error while subscribing to {}", subChannel, e);
    		throw new RuntimeException(e); //NOSONAR
    	}
    }

    public SubscriptionAck replyToSubscribe(
    		final String subChannel,
    		final String pubChannel,
    		int qos) {
    	requireNonNull(subChannel, "Channel cannot be null");
    	try {
    		MessageContext context = null;
    		final MessageContextBuilderProvider builder = mcbFactory.getService();
    		try {
    			context = builder.channel(subChannel)
    					         .replyTo(pubChannel)
    					         .extensionEntry(EXTENSION_QOS, qos)
    					         .extensionEntry(REPLY_TO, true)
    					         .buildContext();
    			return _subscribe(context);
    		} finally {
    			mcbFactory.ungetService(builder);
    		}
    	}
    	catch (final Exception e) { //NOSONAR
    		logger.error("Error while subscribing to {}", subChannel, e);
    		throw new RuntimeException(e); //NOSONAR
    	}
    }

    public SubscriptionAck _subscribe(final MessageContext context) { //NOSONAR
        final PushStreamProvider provider = new PushStreamProvider();
        final SimplePushEventSource<Message> source = acquirePushEventSource(provider);
        final PushStream<Message> stream = provider.createStream(source); //NOSONAR

        // add topic prefix if available
        final String prefix = messagingClient.config.topicPrefix();
        final String sChannel = addTopicPrefix(context.getChannel(), prefix);
        final String pChannel = addTopicPrefix(context.getReplyToChannel(), prefix);

        requireNonNull(sChannel, "Channel cannot be null");
        if (sChannel.isEmpty()) {
        	throw new IllegalArgumentException("Channel cannot be empty");
        }
        // Time-of-Check to Time-of-Use (TOCTOU) race condition that can occur during bundle startup or client reconfiguration
        final Mqtt5AsyncClient currentClient = messagingClient.client; // Read volatile field ONCE

        if (currentClient == null) {
            logger.error("Cannot subscribe to '{}' since the client is not yet initialized", sChannel);
            throw new IllegalStateException("Client is not ready, cannot subscribe to channel: " + sChannel);
        }

        final MqttClientState clientState = currentClient.getState();
        if (clientState == DISCONNECTED || clientState == DISCONNECTED_RECONNECT) {
            logger.error("Cannot subscribe to '{}' since the client is disconnected", sChannel);
            throw new IllegalStateException("Client is disconnected, cannot subscribe to channel: " + sChannel);
        }
		final int qos;
        final boolean isReplyToSub;
        final boolean receiveLocal;
        final boolean retainAsPublished;
        final MessageContextProvider ctx = (MessageContextProvider) context;
        final Map<String, Object> extensions = context.getExtensions();

        if (extensions == null || extensions.isEmpty()) {
        	qos = config.qos();
        	receiveLocal = true;
            retainAsPublished = false;
            isReplyToSub = false;
        } else {
        	qos = getQoS(extensions, converter, config.qos());

        	final Object isReplyTo = extensions.getOrDefault(REPLY_TO, false);
        	isReplyToSub = adaptTo(isReplyTo, boolean.class, converter);

        	final Object receiveLcl = extensions.getOrDefault(RECEIVE_LOCAL, true);
        	receiveLocal = adaptTo(receiveLcl, boolean.class, converter);

        	final Object isRetainAsPublished = extensions.getOrDefault(RETAIN, false);
        	retainAsPublished = adaptTo(isRetainAsPublished, boolean.class, converter);
        }
        try {
            final ExtendedSubscription subscription = subscriptionRegistry.addSubscription(sChannel, pChannel, qos, source::close, isReplyToSub);
			final CompletableFuture<Mqtt5SubAck> future =    currentClient.subscribeWith()
										                                  .topicFilter(sChannel)
										                                  .qos(MqttQos.fromCode(qos))
										                                  .noLocal(!receiveLocal)
										                                  .retainAsPublished(retainAsPublished)
										                                  .callback(p -> {
										                                	  try {
										                                		  final MessageContextBuilderProvider mcb = mcbFactory.getService();
											                                	  try { //NOSONAR
											                                		  final Message message = toMessage(p, ctx, mcb);
											                                		  logger.trace("Successful Subscription Response: {} ", message);
											                                              acknowledgeMessage(
											                                                      message,
											                                                      ctx,
											                                                      source::publish,
											                                                      bundleContext,
											                                                      logger);
											                                      } catch (final Exception e) {
											                                           source.error(e);
											                                      } finally {
											                                           mcbFactory.ungetService(mcb);
											                                      }
										                                	  } catch (final Exception ex) {
										                                		  logger.error("Exception occurred while processing message", ex);
										                                		  source.error(ex);
										                                	  }
										                                   })
										                                  .send();
			future.thenAccept(ack -> {
				final boolean ok = isSubscriptionAcknowledged(ack);

			    final String reason = ack.getReasonString().map(Objects::toString).orElse(null);
			    final int[] codes = ack.getReasonCodes().stream()
			            .mapToInt(Mqtt5SubAckReasonCode::getCode)
			            .toArray();

			    if (ok) {
			        subscription.setAcknowledged(true);
			        final MqttSubAckDTO subAck =
			                createStatusEvent(Type.ACKED, sChannel, qos, isReplyToSub, reason, codes);
			        sendSubscriptionStatusEvent(subAck);
			        logger.info("New subscription request for '{}' processed successfully - {} > ID: {}",
			                     sChannel, ack, subscription.id);
			    } else {
			        final MqttSubAckDTO subNack =
			                createStatusEvent(FAILED, sChannel, qos, isReplyToSub, reason, codes);
			        sendSubscriptionStatusEvent(subNack);
			        logger.error("New subscription request for '{}' failed - {} > ID: {}",
			                     sChannel, ack, subscription.id);
			    }
            });

			stream.onClose(() -> {
				logger.info("Scheduling removal for subscription '{}' on topic '{}'", subscription.id, sChannel);

		        // Atomically remove the subscription
		        final boolean wasLastSubscriber = subscriptionRegistry.removeSubscription(sChannel, subscription.id);

		        // If it was the last, schedule the *network* unsubscribe call
		        if (wasLastSubscriber) {
		            logger.info("Scheduling final unsubscribe for topic '{}'", sChannel);

					try {
						final ExecutorService registryExecutor = subscriptionRegistry.getUnsubscribeExecutor();
						registryExecutor.submit(() -> {
							subscriptionRegistry.unsubscribeSubscription(sChannel);
						});
					} catch (final Exception e) {
						// This will only fail if the registry is also stopping
						logger.warn("Could not schedule unsubscribe task, registry may be shutting down.", e);
					}
		        }
		    });

            future.get(config.timeoutInMillis(), MILLISECONDS);
            return SubscriptionAck.of(stream, subscription.id);
        } catch (final ExecutionException e) {
        	logger.error("Error while subscribing to {}", sChannel, e);
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            final String reason = cause.getMessage();
            // No SubAck available here, hence, no reason codes
            final MqttSubAckDTO subNack =
                    createStatusEvent(NO_ACK, sChannel, qos, isReplyToSub, reason, new int[0]);
            sendSubscriptionStatusEvent(subNack);
            throw new RuntimeException(e.getCause()); //NOSONAR
        } catch (final Exception e) { //NOSONAR
        	logger.error("Error while subscribing to {}", sChannel, e);
            final String reason = e.getMessage();
            // No SubAck available here, hence, no reason codes
            final MqttSubAckDTO subNack =
                    createStatusEvent(NO_ACK, sChannel, qos, isReplyToSub, reason, new int[0]);
            sendSubscriptionStatusEvent(subNack);
            throw new RuntimeException(e); // NOSONAR
        }
    }

	private SimplePushEventSource<Message> acquirePushEventSource(final PushStreamProvider provider) {
		return InterruptSafe.execute(() -> provider.createSimpleEventSource(Message.class));
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

    private void sendSubscriptionStatusEvent(final MqttSubAckDTO ack) { 
        final Map<String, Object> dtoMap = converter.convert(ack).sourceAsDTO()
                .to(new TypeReference<Map<String, Object>>() {
                });

        // Post the canonical event topic (e.g., mqtt/subscription/ACKED or FAILED)
        eventAdmin.postEvent(new Event(MQTT_SUBSCRIPTION_EVENT_TOPIC_PREFIX + ack.type.name(), dtoMap));
    }

    private MqttSubAckDTO createStatusEvent(Type type, String topic, int qos, boolean replyTo, String reason, int[] reasonCodes) {
    	MqttSubAckDTO dto = new MqttSubAckDTO();

    	dto.type = type;
    	dto.topic = topic;
    	dto.qos = qos;
    	dto.replyTo = replyTo;
    	dto.reason = reason;
    	dto.reasonCodes = reasonCodes;
    	dto.timestamp = System.currentTimeMillis();

    	return dto;
    }

}