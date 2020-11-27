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

import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Component.MESSAGE_PUBLISHER;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.findQoS;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.stackTraceToString;
import static java.util.Collections.emptyMap;
import static org.osgi.service.messaging.Features.GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.QOS;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.propertytypes.MessagingFeature;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder.Send.Complete;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;

//@formatter:off
@MessagingFeature(
        name = MESSAGE_PUBLISHER,
        protocol = MQTT_PROTOCOL,
        feature = {
                QOS,
                RETAIN,
                USER_PROPERTIES,
                GUARANTEED_DELIVERY,
                GUARANTEED_ORDERING,
                MESSAGE_EXPIRY_INTERVAL })
//@formatter:on
@Component(service = { MessagePublisher.class, SimpleMessagePublisher.class })
public final class SimpleMessagePublisher implements MessagePublisher {

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private SimpleMessageClient messagingClient;

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

    private void publish(final Message message, MessageContext context, String channel) {
        try {
            if (context == null) {
                context = message.getContext();
            }
            if (channel == null) {
                channel = context.getChannel();
            }
            final String ch = channel; // needed for lambda as it needs to be effectively final :(
            final Map<String, Object> extensions = context.getExtensions();

            final String contentType = context.getContentType();
            final String ctxCorrelationId = context.getCorrelationId();
            final String correlationId = ctxCorrelationId != null ? ctxCorrelationId : UUID.randomUUID().toString();
            final ByteBuffer content = message.payload();
            final Long messageExpiryInterval = (Long) extensions.getOrDefault(MESSAGE_EXPIRY_INTERVAL, null);
            final int qos = findQoS(extensions);
            final boolean retain = (boolean) extensions.getOrDefault(RETAIN, false);

            @SuppressWarnings("unchecked")
            final Map<String, String> userProperties = //
                    (Map<String, String>) extensions.getOrDefault(USER_PROPERTIES, emptyMap());

            final Mqtt5UserPropertiesBuilder propsBuilder = Mqtt5UserProperties.builder();
            userProperties.forEach((k, v) -> propsBuilder.add(k, v));

            // @formatter:off
            final Complete<CompletableFuture<Mqtt5PublishResult>> publishRequest =
                    messagingClient.client.publishWith()
                                              .topic(channel)
                                              .contentType(contentType)
                                              .payload(content)
                                              .qos(MqttQos.fromCode(qos))
                                              .retain(retain)
                                              .userProperties(propsBuilder.build());
            if (messageExpiryInterval == null) {
                publishRequest.noMessageExpiry();
            } else {
                publishRequest.messageExpiryInterval(messageExpiryInterval);
            }
            final String replyToChannel = context.getReplyToChannel();
            if (replyToChannel != null) {
                publishRequest.responseTopic(replyToChannel);
            }
            if (correlationId != null) {
                publishRequest.correlationData(correlationId.getBytes());
            }
            publishRequest.send()
                          .thenAccept(result -> {
                              if (isPublishSuccessful(result)) {
                                  logger.debug("New publish request for '{}' has been processed successfully", ch);
                              } else {
                                  logger.error("New publish request for '{}' failed - {}", ch, stackTraceToString(result.getError().get()));
                              }
                          });
            // @formatter:on
        } catch (final Exception e) {
            logger.error("Eror while publishing data", e);
        }
    }

    private boolean isPublishSuccessful(final Mqtt5PublishResult result) {
        return result.getError().isPresent();
    }

}
