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
import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator.UTF_8;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.PUBLISHER;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.adapt;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.adaptTo;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.addTopicPrefix;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getCorrelationId;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getQoS;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.converter.TypeReference;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder.Send.Complete;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;

import in.bytehue.messaging.mqtt5.provider.MessagePublisherProvider.PublisherConfig;

//@formatter:off
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                    RETAIN,
                    EXTENSION_QOS,
                    USER_PROPERTIES,
                    MESSAGE_EXPIRY_INTERVAL,
                    EXTENSION_GUARANTEED_DELIVERY,
                    EXTENSION_GUARANTEED_ORDERING
                  }
)
@Designate(ocd = PublisherConfig.class)
@Component(service = {
                       MessagePublisher.class,
                       MessagePublisherProvider.class
                     },
           configurationPid = PUBLISHER
)
//@formatter:on
public final class MessagePublisherProvider implements MessagePublisher {

	//@formatter:off
	@ObjectClassDefinition(
			name = "MQTT 5.0 Messaging Publisher Configuration", 
			description = "This configuration is used to configure the MQTT 5.0 messaging publisher")
	public @interface PublisherConfig {
		@AttributeDefinition(name = "Default timeout for synchronously publishing to the broker", min = "5000")
		long timeoutInMillis() default 30_000L;
		
		@AttributeDefinition(name = "Default QoS for publishes unless specified", min = "0", max = "2")
        int qos() default 0;
	}
	//@formatter:on

	@Activate
	private volatile PublisherConfig config;

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private ConverterAdapter converter;

	@Reference
	private MessageClientProvider messagingClient;

	@Activate
	private BundleContext bundleContext;

	@Override
	public void publish(final Message message) {
		publish(message, null, null);
	}

	@Override
	public void publish(final Message message, final String channel) {
		publish(message, null, channel);
	}

	@Override
	public void publish(final Message message, final MessageContext context) {
		publish(message, context, null);
	}
	
	public synchronized PublisherConfig config() {
        return config;
    }

	private void publish(final Message message, MessageContext context, String channel) {
		try {
			if (context == null) {
				context = message.getContext();
			}
			if (channel == null) {
				channel = context.getChannel();
			}
			final MqttClientState clientState = messagingClient.client.getState();
			if (clientState == DISCONNECTED || clientState == DISCONNECTED_RECONNECT) {
				logger.warn("Cannot publish the message to '{}' since the client is disconnected", channel);
				return;
			}
			// add topic prefix if available
			final String prefix = messagingClient.config.topicPrefix();
			channel = addTopicPrefix(channel, prefix);

			final String ch = channel; // needed for lambda as it needs to be effectively final :(
			final Map<String, Object> extensions = context.getExtensions();

			final String contentType = context.getContentType();
			final String replyToChannel = context.getReplyToChannel();
			final String correlationId = getCorrelationId((MessageContextProvider) context, bundleContext, logger);
			final ByteBuffer content = message.payload();

			final Object messageExpiry = extensions.getOrDefault(MESSAGE_EXPIRY_INTERVAL, null);
			final Long messageExpiryInterval = adaptTo(messageExpiry, Long.class, converter);

			final int qos;
			if (extensions == null || extensions.isEmpty()) {
            	qos = config.qos();
            } else {
            	qos = getQoS(extensions, converter, config.qos());
            }

			final Object isRetain = extensions.getOrDefault(RETAIN, false);
			final boolean retain = adaptTo(isRetain, boolean.class, converter);

			final String contentEncoding = context.getContentEncoding();

			Mqtt5PayloadFormatIndicator payloadFormat = null;
			if ("UTF-8".equalsIgnoreCase(contentEncoding)) {
				payloadFormat = UTF_8;
			}

			// @formatter:off
            final Object userProp = extensions.getOrDefault(USER_PROPERTIES, emptyMap());
            final Map<String, String> userProperties =
                    adapt(
                            userProp,
                            new TypeReference<Map<String, String>>() {},
                            converter);

            final Mqtt5UserPropertiesBuilder propsBuilder = Mqtt5UserProperties.builder();
            userProperties.forEach(propsBuilder::add);

            
			final Mqtt5UserProperties userProps = propsBuilder.build();
			final Complete<CompletableFuture<Mqtt5PublishResult>> publishRequest =
                    messagingClient.client.publishWith()
                                              .topic(channel)
                                              .payloadFormatIndicator(payloadFormat)
                                              .contentType(contentType)
                                              .payload(content)
                                              .qos(MqttQos.fromCode(qos))
                                              .retain(retain)
                                              .responseTopic(replyToChannel)
                                              .correlationData(correlationId.getBytes())
                                              .userProperties(userProps);

            if (messageExpiryInterval == null || messageExpiryInterval == 0) {
                publishRequest.noMessageExpiry();
            } else {
                publishRequest.messageExpiryInterval(messageExpiryInterval);
            }

            logger.debug(
            	    "Publish Request:\n" +
            	    "  Channel           : {}\n" +
            	    "  Payload Format    : {}\n" +
            	    "  Content Type      : {}\n" +
            	    "  QoS              : {}\n" +
            	    "  Retain           : {}\n" +
            	    "  Reply-To Channel  : {}\n" +
            	    "  Correlation ID   : {}\n" +
            	    "  User Properties  : {}",
            	    channel,
            	    payloadFormat,
            	    contentType,
            	    MqttQos.fromCode(qos),
            	    retain,
            	    replyToChannel,
            	    correlationId,
            	    userProps
            	);
            final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
            publishRequest.send()
                          .whenComplete((result, throwable) -> {
                              if (throwable != null) {
                            	  resultFuture.completeExceptionally(throwable);
                                  logger.error("Error occurred while publishing message", throwable);
                              } else if (isPublishSuccessful(result)) {
                            	  resultFuture.complete(null);
                            	  logger.debug("Successful Publish Request: {} ", result);
							      logger.debug("New publish request for '{}' has been processed successfully", ch);
							  } else {
								  final Throwable t = result.getError().get();
								  resultFuture.completeExceptionally(t);
								  logger.error("New publish request for '{}' failed - {}", ch, t);
							  }
                          });
            resultFuture.get(config.timeoutInMillis(), MILLISECONDS);
            // @formatter:on
		} catch (final ExecutionException e) {
			logger.error("Error while publishing data to {}", channel, e);
			throw new RuntimeException(e.getCause());
		} catch (final Exception e) {
			logger.error("Error while publishing data to {}", channel, e);
			throw new RuntimeException(e);
		}
	}

	private boolean isPublishSuccessful(final Mqtt5PublishResult result) {
		return !result.getError().isPresent();
	}

}
