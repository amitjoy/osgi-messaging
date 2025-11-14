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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MQTT_CONNECTION_READY_SERVICE_PROPERTY_FILTER;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.PID;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.ReplyToSubDTO.Type.REPLY_TO_MANY_SUB;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.ReplyToSubDTO.Type.REPLY_TO_SINGLE_SUB;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.ReplyToSubDTO.Type.REPLY_TO_SUB;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.adaptTo;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.prepareExceptionAsMessage;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_FEATURE_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_NAME_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_RESPONSE_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.tracker.ServiceTracker;

import in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.Config;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionRegistry.ExtendedSubscription;
import in.bytehue.messaging.mqtt5.provider.helper.FilterParser;
import in.bytehue.messaging.mqtt5.provider.helper.FilterParser.Expression;
import in.bytehue.messaging.mqtt5.provider.helper.LogHelper;
import in.bytehue.messaging.mqtt5.provider.helper.SubscriptionAck;
import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

@Designate(ocd = Config.class)
@Component(configurationPid = PID)
@MessagingFeature(name = MESSAGING_ID, protocol = MESSAGING_PROTOCOL)
public final class MessageReplyToWhiteboardProvider {

	public static final String PID = "in.bytehue.messaging.whiteboard";
	public static final String REPLY_TO_SUBSCRIPTION_REQUEST_QOS_PROPERTY = "osgi.messaging.replyToSubscription.channel.qos";
	public static final String REPLY_TO_SUBSCRIPTION_RESPONSE_QOS_PROPERTY = "osgi.messaging.replyToSubscription.replyChannel.qos";

	@ObjectClassDefinition(name = "MQTT 5.0 Reply-To Whiteboard Configuration", description = "This configuration is used to configure the MQTT 5.0 messaging reply-to whiteboard. "
			+ "Note that, all time-based configurations are in seconds.")
	public @interface Config {
		@AttributeDefinition(name = "Flag denoting to store the channel info if the channel is specified in the received message")
		boolean storeReplyToChannelInfoIfReceivedInMessage() default true;

		@AttributeDefinition(name = "Prefix of the threads' names in the pool")
		String threadNamePrefix() default "reply-to-handler";

		@AttributeDefinition(name = "Suffix of the threads' names in the pool (supports only {@code %d} format specifier)")
		String threadNameSuffix() default "-%d";

		@AttributeDefinition(name = "Flag to set if the threads will be daemon threads")
		boolean isDaemon() default true;

		@AttributeDefinition(name = "Core Pool Size (O set as default for cached behaviour)")
		int corePoolSize() default 0;

		@AttributeDefinition(name = "Maximum Pool Size")
		int maxPoolSize() default 3;

		@AttributeDefinition(name = "Idle time for threads before interrupted")
		long idleTime() default 60L;

		@AttributeDefinition(name = "Enable subscription health check", description = "Enable periodic health check for reply-to subscriptions")
		boolean enableHealthCheck() default true;

		@AttributeDefinition(name = "Health check interval", description = "Interval in seconds between subscription health checks", min = "1")
		int healthCheckIntervalSeconds() default 5;

		@AttributeDefinition(name = "Initial health check delay", description = "Initial delay in seconds before starting health checks", min = "1")
		int healthCheckInitialDelaySeconds() default 10;

		@AttributeDefinition(name = "Maximum retry attempts", description = "Maximum number of times to retry a failed subscription (0 to disable)", min = "0")
		int maxRetryAttempts() default 0;
	}

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private LogMirrorService logMirror;

	@Reference
	private ConverterAdapter converter;

	@Reference
	private MessagePublisherProvider publisher;

	@Reference
	private MessageSubscriptionProvider subscriber;

	@Reference
	private MessageSubscriptionRegistry registry;

	@Reference(service = AnyService.class, target = MQTT_CONNECTION_READY_SERVICE_PROPERTY_FILTER)
	private Object mqttConnectionReady;

	@Reference
	private ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

	private LogHelper logHelper;
	private volatile Config config;
	private ExecutorService executorService;
	private ScheduledExecutorService healthCheckExecutor;
	private final List<ReplyToSubDTO> subscriptions = new CopyOnWriteArrayList<>();

	private ServiceTracker<ReplyToSingleSubscriptionHandler, ReplyToSingleSubscriptionHandler> tracker1;
	private ServiceTracker<ReplyToSubscriptionHandler, ReplyToSubscriptionHandler> tracker2;
	private ServiceTracker<ReplyToManySubscriptionHandler, ReplyToManySubscriptionHandler> tracker3;

	@Activate
	void activate(final Config config, final BundleContext context) {
		this.config = config;
		this.logHelper = new LogHelper(logger, logMirror);
		// @formatter:off
		final ThreadFactory threadFactory =
                new ThreadFactoryBuilder()
                        .setThreadFactoryName(config.threadNamePrefix())
                        .setThreadNameFormat(config.threadNameSuffix())
                        .setDaemon(config.isDaemon())
                        .build();
		executorService = new ThreadPoolExecutor(
	            config.corePoolSize(),
	            config.maxPoolSize(),
	            config.idleTime(),
	            SECONDS,
	            new LinkedBlockingQueue<>(),
	            threadFactory
	    );

		// Initialize health check executor if enabled
		if (config.enableHealthCheck()) {
			final ThreadFactory healthCheckThreadFactory = new ThreadFactoryBuilder()
					.setThreadFactoryName("reply-to-health-check")
					.setThreadNameFormat("-%d")
					.setDaemon(true)
					.build();
			healthCheckExecutor = new ScheduledThreadPoolExecutor(1, healthCheckThreadFactory);
			healthCheckExecutor.scheduleAtFixedRate(
					this::checkSubscriptionHealth,
					config.healthCheckInitialDelaySeconds(),
					config.healthCheckIntervalSeconds(),
					SECONDS);
			logHelper.info("Subscription health check enabled - interval: {}s, initial delay: {}s",
					config.healthCheckIntervalSeconds(), config.healthCheckInitialDelaySeconds());
		}
		// @formatter:on

		subscriptions.stream().filter(sub -> !sub.isProcessed()).forEach(sub -> {
			switch (sub.type) {
			case REPLY_TO_SUB:
				processReplyToSubscriptionHandler(sub);
				break;
			case REPLY_TO_SINGLE_SUB:
				processReplyToSingleSubscriptionHandler(sub);
				break;
			case REPLY_TO_MANY_SUB:
				processReplyToManySubscriptionHandler(sub);
				break;
			}
		});
		tracker1 = new ServiceTracker<ReplyToSingleSubscriptionHandler, ReplyToSingleSubscriptionHandler>(context,
				ReplyToSingleSubscriptionHandler.class, null) {
			@Override
			public synchronized ReplyToSingleSubscriptionHandler addingService(
					final ServiceReference<ReplyToSingleSubscriptionHandler> reference) {
				final ReplyToSingleSubscriptionHandler handler = super.addingService(reference);

				final ReplyToSubDTO sub = new ReplyToSubDTO(handler, REPLY_TO_SINGLE_SUB, reference);
				subscriptions.add(sub);

				logHelper.info(
						"Reply-To Single Subscription Handler tracked. Service ID: {}. Submitting for processing...",
						reference.getProperty(SERVICE_ID));

				executorService.submit(() -> processReplyToSingleSubscriptionHandler(sub));
				return handler;
			}

			@Override
			public synchronized void modifiedService(final ServiceReference<ReplyToSingleSubscriptionHandler> reference,
					final ReplyToSingleSubscriptionHandler service) {
				logHelper.info("Reply-To Single Subscription Handler modified. Service ID: {}",
						reference.getProperty(SERVICE_ID));
				removedService(reference, service);
				addingService(reference);
			}

			@Override
			public synchronized void removedService(final ServiceReference<ReplyToSingleSubscriptionHandler> reference,
					final ReplyToSingleSubscriptionHandler service) {
				logHelper.info("Reply-To Single Subscription Handler removed. Service ID: {}",
						reference.getProperty(SERVICE_ID));
				removeSubscription(reference);
			}
		};
		tracker2 = new ServiceTracker<ReplyToSubscriptionHandler, ReplyToSubscriptionHandler>(context,
				ReplyToSubscriptionHandler.class, null) {
			@Override
			public synchronized ReplyToSubscriptionHandler addingService(
					final ServiceReference<ReplyToSubscriptionHandler> reference) {
				final ReplyToSubscriptionHandler handler = super.addingService(reference);

				final ReplyToSubDTO sub = new ReplyToSubDTO(handler, REPLY_TO_SUB, reference);
				subscriptions.add(sub);

				logHelper.info("Reply-To Subscription Handler tracked. Service ID: {}. Submitting for processing...",
						reference.getProperty(SERVICE_ID));

				executorService.submit(() -> processReplyToSubscriptionHandler(sub));
				return handler;
			}

			@Override
			public synchronized void modifiedService(final ServiceReference<ReplyToSubscriptionHandler> reference,
					final ReplyToSubscriptionHandler service) {
				logHelper.info("Reply-To Subscription Handler modified. Service ID: {}",
						reference.getProperty(SERVICE_ID));
				removedService(reference, service);
				addingService(reference);
			}

			@Override
			public synchronized void removedService(final ServiceReference<ReplyToSubscriptionHandler> reference,
					final ReplyToSubscriptionHandler service) {
				logHelper.info("Reply-To Subscription Handler removed. Service ID: {}",
						reference.getProperty(SERVICE_ID));
				removeSubscription(reference);
			}
		};
		tracker3 = new ServiceTracker<ReplyToManySubscriptionHandler, ReplyToManySubscriptionHandler>(context,
				ReplyToManySubscriptionHandler.class, null) {
			@Override
			public synchronized ReplyToManySubscriptionHandler addingService(
					final ServiceReference<ReplyToManySubscriptionHandler> reference) {
				final ReplyToManySubscriptionHandler handler = super.addingService(reference);

				final ReplyToSubDTO sub = new ReplyToSubDTO(handler, REPLY_TO_MANY_SUB, reference);
				subscriptions.add(sub);

				logHelper.info(
						"Reply-To Many Subscription Handler tracked. Service ID: {}. Submitting for processing...",
						reference.getProperty(SERVICE_ID));

				executorService.submit(() -> processReplyToManySubscriptionHandler(sub));
				return handler;
			}

			@Override
			public synchronized void modifiedService(final ServiceReference<ReplyToManySubscriptionHandler> reference,
					final ReplyToManySubscriptionHandler service) {
				logHelper.info("Reply-To Many Subscription Handler modified. Service ID: {}",
						reference.getProperty(SERVICE_ID));
				removedService(reference, service);
				addingService(reference);
			}

			@Override
			public synchronized void removedService(final ServiceReference<ReplyToManySubscriptionHandler> reference,
					final ReplyToManySubscriptionHandler service) {
				logHelper.info("Reply-To Many Subscription Handler removed. Service ID: {}",
						reference.getProperty(SERVICE_ID));
				removeSubscription(reference);
			}
		};

		tracker1.open();
		tracker2.open();
		tracker3.open();

		logHelper.info("Messaging reply-to whiteboard has been activated");
	}

	@Deactivate
	void deactivate() {
		// Shutdown health check executor first
		if (healthCheckExecutor != null) {
			healthCheckExecutor.shutdownNow();
			logHelper.info("Health check executor shut down");
		}

		subscriptions.stream().forEach(sub -> sub.subAcks.stream().forEach(s -> s.stream().close()));
		subscriptions.clear();

		tracker1.close();
		tracker2.close();
		tracker3.close();

		executorService.shutdownNow();
		logHelper.info("Messaging reply-to whiteboard has been deactivated");
	}

	private void processReplyToSingleSubscriptionHandler(final ReplyToSubDTO sub) {
		try {
			logHelper.info("Processing Reply-To Single Subscription Handler. Service ID: {}",
					sub.reference.getProperty(SERVICE_ID));

			// Check if this specific DTO instance is still in the main list.
			// If it's not, it means a "remove" or "modify" operation has
			// already removed it, and this task is stale.
			if (!subscriptions.contains(sub)) {
				logHelper.warn(
						"Cancelling stale subscription task for Reply-To Single Subscription Handler - Service ID: {}. This is normal.",
						sub.reference.getProperty(SERVICE_ID));
				return; // Abort
			}

			final ReplyToDTO replyToDTO = new ReplyToDTO(sub.reference);

			logHelper.info(
					"Validated Reply-To Single Subscription Handler. Service ID: {}. SubChannels: {}. PubChannel: {}",
					sub.reference.getProperty(SERVICE_ID), Arrays.toString(replyToDTO.subChannels),
					replyToDTO.pubChannel);

			final long serviceId = (long) sub.reference.getProperty(SERVICE_ID);

			Stream.of(replyToDTO.subChannels).forEach(c -> {
				try {
					logHelper.debug(
							"Processing Reply-To Single Subscription Handler for Sub-Channel: {} and Pub-Channel: {}",
							c, replyToDTO.pubChannel);
					final SubscriptionAck ack = subscriber.replyToSubscribe(c, replyToDTO.pubChannel, replyToDTO.subQos,
							serviceId);
					sub.addAck(ack);

					logHelper.info(
							"MQTT subscription successful for Reply-To Single Subscription Handler. Service ID: {}. Channel: {}. Sub-ID: {}",
							sub.reference.getProperty(SERVICE_ID), c, ack.id());

					ack.stream().map(m -> {
						logHelper.debug("[Reply-To Single Subscription] Received message '{}' on '{}'", m.getContext(),
								sub.handler.getClass().getSimpleName());
						return handleResponse(m, (ReplyToSingleSubscriptionHandler) sub.handler);
					}).forEach(m -> handleMessageReceive(sub.reference, replyToDTO, c, ack, m));
				} catch (Exception e) {
					logHelper.error("Cannot process reply-to single subscription: {}", c, e);
					sub.markForRetry();
				}
			});
			sub.clearRetry();
		} catch (Exception e) {
			logHelper.error(
					"Failed to validate Reply-To Single Subscription Handler. Service ID: {}. Reason: {}. Check service properties.",
					sub.reference.getProperty(SERVICE_ID), e.getMessage());
			sub.markForRetry();
		}
	}

	private void processReplyToSubscriptionHandler(final ReplyToSubDTO sub) {
		try {
			logHelper.info("Processing Reply-To Subscription Handler. Service ID: {}",
					sub.reference.getProperty(SERVICE_ID));

			// Check if this specific DTO instance is still in the main list.
			// If it's not, it means a "remove" or "modify" operation has
			// already removed it, and this task is stale.
			if (!subscriptions.contains(sub)) {
				logHelper.warn(
						"Cancelling stale subscription task for Reply-To Subscription Handler. Service ID: {}. This is normal.",
						sub.reference.getProperty(SERVICE_ID));
				return; // Abort
			}

			final ReplyToDTO replyToDTO = new ReplyToDTO(sub.reference);

			logHelper.info("Validated Reply-To Subscription Handler. Service ID: {}. SubChannels: {}. PubChannel: {}",
					sub.reference.getProperty(SERVICE_ID), Arrays.toString(replyToDTO.subChannels),
					replyToDTO.pubChannel);

			final long serviceId = (long) sub.reference.getProperty(SERVICE_ID);

			Stream.of(replyToDTO.subChannels).forEach(c -> {
				try {
					logHelper.debug("Processing Reply-To Subscription Handler for Sub-Channel: {} and Pub-Channel: {}",
							c, replyToDTO.pubChannel);
					final SubscriptionAck ack = subscriber.replyToSubscribe(c, replyToDTO.pubChannel, replyToDTO.subQos,
							serviceId);
					sub.addAck(ack);

					logHelper.info(
							"MQTT subscription successful for Reply-To Subscription Handler. Service ID: {}. Channel: {}. Sub-ID: {}",
							sub.reference.getProperty(SERVICE_ID), c, ack.id());

					ack.stream().forEach(m -> {
						logHelper.debug("[Reply-To Subscription] Received message '{}' on '{}'", m.getContext(),
								sub.handler.getClass().getSimpleName());
						((ReplyToSubscriptionHandler) sub.handler).handleResponse(m);
					});
				} catch (Exception e) {
					logHelper.error("Cannot process reply-to subscription: {}", c, e);
					sub.markForRetry();
				}
			});
			sub.clearRetry();
		} catch (Exception e) {
			logHelper.error(
					"Failed to validate Reply-To Subscription Handler. Service ID: {}. Reason: {}. Check service properties.",
					sub.reference.getProperty(SERVICE_ID), e.getMessage());
			sub.markForRetry();
		}
	}

	private void processReplyToManySubscriptionHandler(final ReplyToSubDTO sub) {
		try {
			logHelper.info("Processing Reply-To Many Subscription Handler. Service ID: {}",
					sub.reference.getProperty(SERVICE_ID));

			// Check if this specific DTO instance is still in the main list.
			// If it's not, it means a "remove" or "modify" operation has
			// already removed it, and this task is stale.
			if (!subscriptions.contains(sub)) {
				logHelper.warn(
						"Cancelling stale subscription task for Reply-To Many Subscription Handler. Service ID: {}. This is normal.",
						sub.reference.getProperty(SERVICE_ID));
				return; // Abort
			}

			final ReplyToDTO replyToDTO = new ReplyToDTO(sub.reference);

			logHelper.info(
					"Validated Reply-To Many Subscription Handler. Service ID: {}. SubChannels: {}. PubChannel: {}",
					sub.reference.getProperty(SERVICE_ID), Arrays.toString(replyToDTO.subChannels),
					replyToDTO.pubChannel);

			final long serviceId = (long) sub.reference.getProperty(SERVICE_ID);

			Stream.of(replyToDTO.subChannels).forEach(c -> {
				try {
					logHelper.debug(
							"Processing Reply-To Many Subscription Handler for Sub-Channel: {} and Pub-Channel: {}", c,
							replyToDTO.pubChannel);
					final SubscriptionAck ack = subscriber.replyToSubscribe(c, replyToDTO.pubChannel, replyToDTO.subQos,
							serviceId);
					sub.addAck(ack);

					logHelper.info(
							"MQTT subscription successful for Reply-To Many Subscription Handler. Service ID: {}. Channel: {}. Sub-ID: {}",
							sub.reference.getProperty(SERVICE_ID), c, ack.id());

					ack.stream().forEach(m -> {
						logHelper.debug("[Reply-To Many Subscription] Received message '{}' on '{}'", m.getContext(),
								sub.handler.getClass().getSimpleName());
						handleResponses(m, (ReplyToManySubscriptionHandler) sub.handler)
								.forEach(msg -> handleMessageReceive(sub.reference, replyToDTO, c, ack, msg));
					});
				} catch (Exception e) {
					logHelper.error("Cannot process reply-to many subscription: {}", c, e);
					sub.markForRetry();
				}
			});
			sub.clearRetry();
		} catch (Exception e) {
			logHelper.error(
					"Failed to validate Reply-To Many Subscription Handler. Service ID: {}. Reason: {}. Check service properties.",
					sub.reference.getProperty(SERVICE_ID), e.getMessage());
			sub.markForRetry();
		}
	}

	private Message handleResponse(final Message request, final ReplyToSingleSubscriptionHandler handler) {
		final MessageContextBuilderProvider mcb = getResponse(request);
		logHelper.debug("Triggering callback in Reply-To Single Subscription Handler: {}",
				handler.getClass().getName());
		try {
			return handler.handleResponse(request, mcb);
		} catch (final Exception e) {
			logHelper.warn("Exception occurred while retrieving response for message: {}", request.getContext(), e);
			return prepareExceptionAsMessage(e, mcb);
		} finally {
			mcbFactory.ungetService(mcb);
		}
	}

	private MessageContextBuilderProvider getResponse(final Message request) {
		final MessageContext context = request.getContext();
		final String channel = context.getChannel();
		final String replyToChannel = context.getReplyToChannel();
		final String correlation = context.getCorrelationId();

		// @formatter:off
		return (MessageContextBuilderProvider) mcbFactory.getService()
				                                         .channel(channel)
				                                         .replyTo(replyToChannel)
				                                         .correlationId(correlation)
				                                         .content(request.payload());
		// @formatter:on
	}

	private PushStream<Message> handleResponses(final Message request, final ReplyToManySubscriptionHandler handler) {
		final MessageContextBuilder mcb = getResponse(request);
		logHelper.debug("Triggering callback in Reply-To Many Subscription Handler: {}", handler.getClass().getName());
		return handler.handleResponses(request, mcb);
	}

	private void handleMessageReceive(final ServiceReference<?> reference, final ReplyToDTO replyToDTO,
			final String channel, final SubscriptionAck sub, final Message msg) {

		final String pubChannelProp = replyToDTO.pubChannel;
		final String pubChannel = pubChannelProp == null || pubChannelProp.isEmpty()
				? msg.getContext().getReplyToChannel()
				: pubChannelProp;

		logHelper.debug("Publishing channel: {}", pubChannel);
		if (pubChannel == null) {
			logHelper.error("No reply-to channel is specified for the subscription handler");
			return;
		}
		if (config.storeReplyToChannelInfoIfReceivedInMessage()) {
			logHelper.debug("Updating subscription info to contain the reply-to channel");
			// update the subscription
			final ExtendedSubscription subscription = registry.getSubscription(channel, sub.id());
			subscription.updateReplyToHandlerSubscription(pubChannel, reference);
		}
		final MessageContextBuilderProvider mcb = mcbFactory.getService();
		try {
			final MessageContextProvider responseMsgContext = (MessageContextProvider) msg.getContext();
			responseMsgContext.getExtensions().put(EXTENSION_QOS, replyToDTO.pubQos);

			logHelper.debug("Publishing reply from Service ID: {}. Topic: {}", reference.getProperty(SERVICE_ID),
					pubChannel);

			publisher.publish(msg, pubChannel);
		} finally {
			mcbFactory.ungetService(mcb);
		}
	}

	private class ReplyToDTO {

		int pubQos;
		int subQos;
		boolean isConform;
		String pubChannel;
		String[] subChannels;

		ReplyToDTO(final ServiceReference<?> reference) {
			final Dictionary<String, ?> properties = reference.getProperties();

			final Object replyToSubResponse = properties.get(REPLY_TO_SUBSCRIPTION_RESPONSE_CHANNEL_PROPERTY);
			final Object replyToSubRequest = properties.get(REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY);
			final Object replyToSubResponseQos = properties.get(REPLY_TO_SUBSCRIPTION_RESPONSE_QOS_PROPERTY);
			final Object replyToSubRequestQos = properties.get(REPLY_TO_SUBSCRIPTION_REQUEST_QOS_PROPERTY);

			if (replyToSubResponseQos == null) {
				pubQos = publisher.config().qos();
			} else {
				pubQos = adaptTo(replyToSubResponseQos, int.class, converter);
			}
			if (replyToSubRequestQos == null) {
				subQos = subscriber.config().qos();
			} else {
				subQos = adaptTo(replyToSubRequestQos, int.class, converter);
			}

			pubChannel = adaptTo(replyToSubResponse, String.class, converter);
			subChannels = adaptTo(replyToSubRequest, String[].class, converter);

			if (subChannels == null) {
				logHelper.error("Service {} missing required property '{}'", reference.getProperty(SERVICE_ID),
						REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY);
				throw new IllegalStateException("The '" + reference
						+ "' handler instance doesn't specify the reply-to subscription channel(s)");
			}

			final Object replyToSubTgt = properties.get(REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY);
			final String replyToSubTarget = adaptTo(replyToSubTgt, String.class, converter);

			final FilterParser fp = new FilterParser();
			final Expression exp = fp.parse(replyToSubTarget);

			final Map<String, String> requiredValues = new HashMap<>();

			requiredValues.put(MESSAGING_FEATURE_PROPERTY, REPLY_TO);
			requiredValues.put(MESSAGING_NAME_PROPERTY, MESSAGING_ID);
			requiredValues.put(MESSAGING_PROTOCOL_PROPERTY, MESSAGING_PROTOCOL);

			isConform = exp.eval(requiredValues);

			if (!isConform) {
				logHelper.error("Service {} has non-conformant target filter '{}'. Required values: {}",
						reference.getProperty(SERVICE_ID), replyToSubTarget, requiredValues);
				throw new IllegalStateException(
						"The '" + reference + "' handler service doesn't specify the reply-to target filter");
			}
		}
	}

	static class ReplyToSubDTO {

		enum Type {
			REPLY_TO_SUB, REPLY_TO_SINGLE_SUB, REPLY_TO_MANY_SUB
		}

		Type type;
		Object handler;
		ServiceReference<?> reference;
		final long createdAt = System.currentTimeMillis();
		List<SubscriptionAck> subAcks = new CopyOnWriteArrayList<>();
		AtomicInteger retryCount = new AtomicInteger(0);
		AtomicBoolean needsRetry = new AtomicBoolean(false);
		AtomicBoolean isRetrying = new AtomicBoolean(false);

		public ReplyToSubDTO(final Object handler, final Type type, final ServiceReference<?> reference) {
			this.handler = handler;
			this.type = type;
			this.reference = reference;
		}

		public void addAck(final SubscriptionAck subAck) {
			subAcks.add(subAck);
		}

		public boolean isProcessed() {
			return !subAcks.isEmpty();
		}

		public boolean needsRetry() {
			return needsRetry.get();
		}

		public void markForRetry() {
			needsRetry.set(true);
		}

		public void clearRetry() {
			needsRetry.set(false);
			retryCount.set(0);
		}

		public boolean startRetry() {
			return isRetrying.compareAndSet(false, true);
		}

		public void finishRetry() {
			isRetrying.set(false);
		}

	}

	private synchronized void removeSubscription(final ServiceReference<?> reference) {
		subscriptions.stream().filter(sub -> sub.reference == reference).forEach(sub -> {
			logHelper.info("Closing {} active subscription streams created by the reply-to handler. Service ID: {}",
					sub.subAcks.size(), reference.getProperty(SERVICE_ID));
			sub.subAcks.stream().forEach(s -> s.stream().close());
		});
		subscriptions.removeIf(sub -> sub.reference == reference);
	}

	/**
	 * Periodic health check that verifies all tracked handlers have active
	 * subscriptions. For each handler:
	 * <ul>
	 * <li>Get configured channel(s) from service properties</li>
	 * <li>Check if registry has active subscription for those channels</li>
	 * <li>If not, mark for retry</li>
	 * </ul>
	 */
	private void checkSubscriptionHealth() {
		try {
			logHelper.debug("Running whiteboard subscription health check...");

			subscriptions.forEach(sub -> {
				final long serviceId = (long) sub.reference.getProperty(SERVICE_ID);

				if (!sub.isProcessed()) {
					// Safety Net: Check if the handler has been in a pending state for too long
					final long pendingDuration = System.currentTimeMillis() - sub.createdAt;
					final long timeoutThreshold = config.healthCheckIntervalSeconds() * 2 * 1000L;

					if (pendingDuration > timeoutThreshold && !sub.needsRetry()) {
						logHelper.warn(
								"Handler for Service ID {} has been in a pending state for over {}ms. It might be stuck. Forcing a retry.",
								serviceId, pendingDuration);
						sub.markForRetry();
					}
					return; // Skip main check for unprocessed handlers
				}
				try {
					// Get the configured channels for this handler
					final ReplyToDTO replyToDTO = new ReplyToDTO(sub.reference);
					final String[] configuredChannels = replyToDTO.subChannels;

					// Check if registry has an active subscription owned by this specific handler
					for (final String channel : configuredChannels) {
						if (!registry.hasActiveSubscription(channel, serviceId)) {
							logHelper.warn(
									"Health check: No active subscription found for channel '{}' on Service ID: {}. Marking for retry.",
									channel, serviceId);
							if (!sub.needsRetry()) {
								sub.markForRetry();
							}
							break; // No need to check other channels for this handler
						} else {
							logHelper.debug("Health check: Subscription for channel '{}' on Service ID: {} is active.",
									channel, serviceId);
						}
					}
				} catch (final Exception e) {
					// Failed to validate - likely bad service properties
					// Don't mark for retry if it's a configuration issue
					logHelper.debug("Could not validate handler during health check. Service ID: {}. Reason: {}",
							serviceId, e.getMessage());
				}
			});
			// Retry any subscriptions marked for retry
			retryFailedSubscriptions();
		} catch (Exception e) {
			logHelper.error("Error during subscription health check", e);
		}
	}

	/**
	 * Retries all subscriptions that are marked for retry.
	 */
	private void retryFailedSubscriptions() {
		final List<ReplyToSubDTO> subsToRetry = subscriptions.stream().filter(ReplyToSubDTO::needsRetry)
				.collect(toList());

		if (subsToRetry.isEmpty()) {
			return;
		}

		logHelper.info("Found {} subscription(s) that need retry", subsToRetry.size());

		for (ReplyToSubDTO sub : subsToRetry) {
			// Prevent concurrent retry attempts for the same handler
			if (!sub.startRetry()) {
				logHelper.debug("Skipping retry for Service ID: {} - already being retried",
						sub.reference.getProperty(SERVICE_ID));
				continue;
			}

			final int retryAttempt = sub.retryCount.incrementAndGet();
			final long maxAttempts = config.maxRetryAttempts();

			if (maxAttempts > 0 && retryAttempt > maxAttempts) {
				logHelper.error(
						"Max retry attempts ({}) exceeded for Service ID: {}. Giving up and clearing retry flag.",
						maxAttempts, sub.reference.getProperty(SERVICE_ID));
				sub.clearRetry();
				sub.finishRetry();
				continue;
			}

			logHelper.info("Retrying subscription (attempt {}/{}) for Service ID: {}", retryAttempt,
					maxAttempts == 0 ? "unlimited" : maxAttempts, sub.reference.getProperty(SERVICE_ID));

			// Close any remaining stale streams before retry
			sub.subAcks.forEach(ack -> {
				try {
					ack.stream().close();
				} catch (Exception e) {
					// Ignore - stream might already be closed
					logHelper.debug("Failed to close stale stream during retry: {}", e.getMessage());
				}
			});
			sub.subAcks.clear();

			// Retry based on type
			executorService.submit(() -> {
				try {
					switch (sub.type) {
					case REPLY_TO_SUB:
						processReplyToSubscriptionHandler(sub);
						break;
					case REPLY_TO_SINGLE_SUB:
						processReplyToSingleSubscriptionHandler(sub);
						break;
					case REPLY_TO_MANY_SUB:
						processReplyToManySubscriptionHandler(sub);
						break;
					}
				} catch (Exception e) {
					logHelper.error("Error during subscription retry for Service ID: {}",
							sub.reference.getProperty(SERVICE_ID), e);
					// Keep retry flag set so it will be retried in next health check
				} finally {
					sub.finishRetry();
				}
			});
		}
	}

}
