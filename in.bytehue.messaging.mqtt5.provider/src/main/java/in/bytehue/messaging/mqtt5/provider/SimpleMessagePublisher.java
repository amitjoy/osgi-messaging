package in.bytehue.messaging.mqtt5.provider;

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MESSAGE_PUBLISHER_NAME;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.RETAIN;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.USER_PROPERTIES;
import static java.util.Collections.emptyMap;
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

@Component(service = { MessagePublisher.class, SimpleMessagePublisher.class })
@MessagingFeature(name = MESSAGE_PUBLISHER_NAME, protocol = MQTT_PROTOCOL)
public final class SimpleMessagePublisher implements MessagePublisher {

    @Reference
    private SimpleMessageClient messagingClient;

    @Reference(service = LoggerFactory.class)
    private Logger logger;

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
            final Map<String, Object> extensions = context.getExtensions();
            final String contentType = context.getContentType();
            final String ctxCorrelationId = context.getCorrelationId();
            final String correlationId = ctxCorrelationId != null ? ctxCorrelationId : UUID.randomUUID().toString();
            final ByteBuffer content = message.payload();
            final Long messageExpiryInterval = (Long) extensions.getOrDefault(MESSAGE_EXPIRY_INTERVAL, null);
            final int qos = (int) extensions.getOrDefault(QOS, DEFAULT_QOS.getCode());
            final boolean retain = (boolean) extensions.getOrDefault(RETAIN, false);

            @SuppressWarnings("unchecked")
            final Map<String, String> userProperties = (Map<String, String>) extensions.getOrDefault(USER_PROPERTIES,
                    emptyMap());

            final Mqtt5UserPropertiesBuilder propsBuilder = Mqtt5UserProperties.builder();
            userProperties.forEach((k, v) -> propsBuilder.add(k, v));

            // @formatter:off
            final Complete<CompletableFuture<Mqtt5PublishResult>> publishRequest =
                    messagingClient.client.toAsync()
                                          .publishWith()
                                          .topic(channel)
                                          .contentType(contentType)
                                          .payload(content)
                                          .qos(MqttQos.fromCode(qos))
                                          .retain(retain)
                                          .userProperties(propsBuilder.build());
            // @formatter:on
            if (messageExpiryInterval == null) {
                publishRequest.noMessageExpiry();
            } else {
                publishRequest.messageExpiryInterval(messageExpiryInterval);
            }
            final String replyToChannel = context.getReplyToChannel();
            if (replyToChannel != null) {
                publishRequest.responseTopic(replyToChannel).correlationData(correlationId.getBytes());
            }
            publishRequest.send();
        } catch (final Exception e) {
            logger.error("Eror while publishing data", e);
        }
    }

}