/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
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
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.PUBLISHER_REPLYTO;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getCorrelationId;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.annotations.ProvideMessagingReplyToManyFeature;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Promise;
import org.osgi.util.pushstream.PushStream;

import in.bytehue.messaging.mqtt5.provider.MessageReplyToPublisherProvider.ReplyToConfig;
import in.bytehue.messaging.mqtt5.provider.helper.LogHelper;

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
//@formatter:on
public final class MessageReplyToPublisherProvider implements ReplyToPublisher, ReplyToManyPublisher {

	@ObjectClassDefinition(name = "MQTT Messaging Reply-To Publisher Configuration", description = "This configuration is used to configure the reply-to publisher")
	public @interface ReplyToConfig {
		@AttributeDefinition(name = "Request Timeout", description = "Default timeout for requests (in milliseconds)", min = "1000")
		long requestTimeoutInMillis() default 15_000L;
	}

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private LogMirrorService logMirror;

	@Reference
	private MessagePublisherProvider publisher;

	@Reference
	private MessageSubscriptionProvider subscriber;

	@Reference
	private ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

	@Activate
	private BundleContext bundleContext;

	private LogHelper logHelper;
	private volatile ReplyToConfig config;

	@Activate
	@Modified
	void init(final ReplyToConfig config) {
		this.config = config;
		this.logHelper = new LogHelper(logger, logMirror);
		logHelper.info("Messaging reply-to publisher has been activated/modified");
	}

	@Deactivate
	void deactivate() {
		logHelper.info("Messaging reply-to publisher has been deactivated");
	}

	public ReplyToConfig config() {
		return this.config;
	}

	@Override
	public Promise<Message> publishWithReply(final Message requestMessage) {
		return publishWithReply(requestMessage, requestMessage.getContext());
	}

	@Override
	public Promise<Message> publishWithReply(final Message requestMessage, final MessageContext replyToContext) {
		final PushStream<Message> stream = publishWithReplyMany(requestMessage, replyToContext);

		// We expect a single response, so we close the stream immediately after
		// resolution or timeout
		// @formatter:off
		return stream.findFirst()
				     .map(o -> o.orElseThrow(() -> new IllegalStateException("Stream closed without response")))
				     .timeout(config.requestTimeoutInMillis())
				     .onFailure(e -> stream.close()) 
				     .onResolve(stream::close);      
		// @formatter:on
	}

	@Override
	public PushStream<Message> publishWithReplyMany(final Message requestMessage) {
		return publishWithReplyMany(requestMessage, requestMessage.getContext());
	}

	@Override
	public PushStream<Message> publishWithReplyMany(final Message requestMessage, final MessageContext replyToContext) {
		final String correlationId = getCorrelationId((MessageContextProvider) requestMessage.getContext(),
				bundleContext, logHelper);

		final String replyToChannel = requestMessage.getContext().getReplyToChannel();
		if (replyToChannel == null || replyToChannel.trim().isEmpty()) {
			throw new IllegalArgumentException("Reply-to channel is missing in the publish message");
		}

		final MessageContextBuilderProvider builder = mcbFactory.getService();
		try {
			final MessageContextProvider contextProvider = (MessageContextProvider) replyToContext;

			// Update the context with the generated correlation ID if it wasn't present
			if (contextProvider.correlationId == null) {
				contextProvider.correlationId = correlationId;
			}

			// Determine the correct subscription channel
			// If the passed context channel is the same as the request channel (publication
			// topic), it means we are likely in the 1-arg scenario or the user reused the
			// context.
			// We must switch to the Reply-To channel to receive the response.
			String subscriptionChannel = replyToContext.getChannel();
			if (subscriptionChannel != null && subscriptionChannel.equals(requestMessage.getContext().getChannel())) {
				subscriptionChannel = replyToChannel;
			}

			PushStream<Message> stream;

			// @formatter:off
			final Map<String, Object> extensions = replyToContext.getExtensions();
			if (extensions != null) {
				extensions.forEach(builder::extensionEntry);
			}
			
			final MessageContext subscriptionContext = 
					builder.channel(subscriptionChannel) // Use the resolved subscription channel
					       .extensionEntry(EXTENSION_QOS, 0)
					       .extensionEntry(REPLY_TO, true)
					       .extensionEntry(RECEIVE_LOCAL, true)
					       .extensionEntry(RETAIN, false)
					       .buildContext();

			stream = subscriber.subscribe(subscriptionContext)
                               .filter(m -> correlationId.equals(m.getContext().getCorrelationId()));
			// @formatter:on

			logHelper.debug("Sending request to '{}' with correlation ID '{}', expecting reply on '{}'",
					requestMessage.getContext().getChannel(), correlationId, subscriptionChannel);

			try {
				publisher.publish(requestMessage);
			} catch (final Exception e) {
				stream.close();
				throw e;
			}

			return stream;

		} finally {
			mcbFactory.ungetService(builder);
		}
	}
}