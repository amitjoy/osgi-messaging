/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.PUBLISHER_REPLYTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getCorrelationId;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.annotations.ProvideMessagingReplyToManyFeature;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import in.bytehue.messaging.mqtt5.provider.MessageReplyToPublisherProvider.ReplyToConfig;
import in.bytehue.messaging.mqtt5.provider.helper.SubscriptionAck;
import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

//@formatter:off
@Designate(ocd = ReplyToConfig.class)
@ProvideMessagingReplyToManyFeature
@Component(
		service = {
				ReplyToPublisher.class,
				ReplyToManyPublisher.class,
                MessageReplyToPublisherProvider.class
              },
		configurationPid = PUBLISHER_REPLYTO)
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                    REPLY_TO,
                    REPLY_TO_MANY_PUBLISH,
                    REPLY_TO_MANY_SUBSCRIBE,
                    GENERATE_CORRELATION_ID,
                    GENERATE_REPLY_CHANNEL
                  }
)
public final class MessageReplyToPublisherProvider implements ReplyToPublisher, ReplyToManyPublisher {

    @ObjectClassDefinition(
            name = "MQTT Messaging Reply-To Publisher Executor Configuration",
            description = "This configuration is used to configure the internal thread pool")
    public @interface ReplyToConfig {
        @AttributeDefinition(name = "Number of threads for the internal thread pool")
        int numThreads() default 20;

        @AttributeDefinition(name = "Prefix of the thread name")
        String threadNamePrefix() default "mqtt-replyto-publisher";

        @AttributeDefinition(name = "Suffix of the thread name (supports only {@code %d} format specifier)")
        String threadNameSuffix() default "-%d";

        @AttributeDefinition(name = "Flag to set if the threads will be daemon threads")
        boolean isDaemon() default true;
    }
    //@formatter:on

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private MessagePublisherProvider publisher;

	@Reference
	private MessageSubscriptionProvider subscriber;

	@Reference
	private ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

	@Activate
	private BundleContext bundleContext;

	private volatile ReplyToConfig config;
	private final PromiseFactory promiseFactory;

	@Activate
	public MessageReplyToPublisherProvider(final ReplyToConfig config) {
		this.config = config;
		//@formatter:off
        final ThreadFactory threadFactory =
                new ThreadFactoryBuilder()
                        .setThreadFactoryName(config.threadNamePrefix())
                        .setThreadNameFormat(config.threadNameSuffix())
                        .setDaemon(config.isDaemon())
                        .build();
        promiseFactory = new PromiseFactory(newFixedThreadPool(config.numThreads(), threadFactory));
        //@formatter:on
	}

	public synchronized ReplyToConfig config() {
		return this.config;
	}

	@Override
	public Promise<Message> publishWithReply(final Message requestMessage) {
		return publishWithReply(requestMessage, requestMessage.getContext());
	}

	@Override
	public Promise<Message> publishWithReply(final Message requestMessage, final MessageContext replyToContext) {
		final Deferred<Message> deferred = promiseFactory.deferred();
		final ReplyToDTO dto = new ReplyToDTO(requestMessage, replyToContext);

		SubscriptionAck sub = null;
		try {
			// Create a new, corrected MessageContext for the subscription
			final String replyChannel = replyToContext.getReplyToChannel();
			if (replyChannel == null || replyChannel.trim().isEmpty()) {
				deferred.fail(new IllegalArgumentException("Reply-to channel is missing in the message context"));
				return deferred.getPromise();
			}
			final MessageContextBuilderProvider builder = mcbFactory.getService();
			try {
				// Use the reply-to channel as the primary channel for the subscription
				final MessageContextBuilder mcb = builder.channel(replyChannel);

				// Copy all extensions from the original context to preserve them
				final Map<String, Object> extensions = replyToContext.getExtensions();
				if (extensions != null) {
					extensions.forEach(mcb::extensionEntry);
				}
				final MessageContext subscriptionContext = mcb.extensionEntry(REPLY_TO, true).buildContext();

				// Pass the new, corrected context to the subscriber
				sub = subscriber._subscribe(subscriptionContext);
			} finally {
				mcbFactory.ungetService(builder);
			}
		} catch (final Exception e) {
			deferred.fail(e);
			return deferred.getPromise();
		}
		// subscribe to the channel first
		final PushStream<Message> stream = sub.stream()
				.filter(responseMessage -> matchCorrelationId(requestMessage, responseMessage)).buffer();

		// resolve the promise on first response matching the specified correlation ID
		// and close the stream to proceed with the unsubscription as it is a fire and
		// forget execution
		stream.forEach(m -> {
			deferred.resolve(m);
			stream.close();
		});

		// publish the request to the channel
		publisher.publish(requestMessage, dto.pubChannel);
		return deferred.getPromise();
	}

	@Override
	public PushStream<Message> publishWithReplyMany(final Message requestMessage) {
		return publishWithReplyMany(requestMessage, requestMessage.getContext());
	}

	@Override
	public PushStream<Message> publishWithReplyMany(final Message requestMessage, final MessageContext replyToContext) {
	    final ReplyToDTO dto = new ReplyToDTO(requestMessage, replyToContext);
	    SubscriptionAck sub = null;
	    try {
	        // Create a new, corrected MessageContext for the subscription
	        final String replyChannel = replyToContext.getReplyToChannel();
	        if (replyChannel == null || replyChannel.trim().isEmpty()) {
	            throw new IllegalArgumentException("Reply-to channel is missing in the message context");
	        }
	        final MessageContextBuilderProvider builder = mcbFactory.getService();
	        try {
	            // Use the reply-to channel as the primary channel for the subscription
	            final MessageContextBuilder mcb = builder.channel(replyChannel);

	            // Copy all extensions from the original context to preserve them
	            final Map<String, Object> extensions = replyToContext.getExtensions();
	            if (extensions != null) {
	                extensions.forEach(mcb::extensionEntry);
	            }
				final MessageContext subscriptionContext = mcb.extensionEntry(REPLY_TO, true).buildContext();

	            // Pass the new, corrected context to the subscriber
	            sub = subscriber._subscribe(subscriptionContext);
	        } finally {
	            mcbFactory.ungetService(builder);
	        }
	    } catch (final Exception e) {
	        // If subscription fails, return an empty, closed stream
	        final PushStreamProvider psp = new PushStreamProvider();
	        final SimplePushEventSource<Message> eventSource = psp.createSimpleEventSource(Message.class);
	        eventSource.error(e); // Propagate the error to the stream
	        return psp.createStream(eventSource);
	    }
	    // subscribe to the channel first
	    final PushStream<Message> stream = sub.stream()
	            .filter(responseMessage -> matchCorrelationId(requestMessage, responseMessage));

	    // publish the request to the channel
	    publisher.publish(requestMessage, dto.pubChannel);
	    return stream;
	}

	private class ReplyToDTO {
		String pubChannel;
		@SuppressWarnings("unused")
		String subChannel;

		ReplyToDTO(final Message message, final MessageContext context) {
			autoGenerateCorrelationIdIfAbsent(message);
			autoGenerateReplyToChannelIfAbsent(message);

			pubChannel = context.getChannel();
			subChannel = context.getReplyToChannel();
		}

		private void autoGenerateCorrelationIdIfAbsent(final Message message) {
			final MessageContextProvider context = (MessageContextProvider) message.getContext();
			context.correlationId = getCorrelationId(context, bundleContext, logger);
		}

		private void autoGenerateReplyToChannelIfAbsent(final Message message) {
			final MessageContextProvider context = (MessageContextProvider) message.getContext();

			if (context.getReplyToChannel() == null) {
				context.replyToChannel = UUID.randomUUID().toString();
				logger.info("Auto-generated reply-to channel '{}' as it is missing in the request",
						context.replyToChannel);
			}
		}
	}

	private boolean matchCorrelationId(final Message requestMessage, final Message responseMessage) {
		final String requestCorrelationId = requestMessage.getContext().getCorrelationId();
		if (requestCorrelationId == null) {
			return false;
		}
		final String responseCorrelationId = responseMessage.getContext().getCorrelationId();
		return requestCorrelationId.equals(responseCorrelationId);
	}

}