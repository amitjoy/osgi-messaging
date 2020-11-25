package in.bytehue.messaging.mqtt5.provider;

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static in.bytehue.messaging.mqtt5.api.ExtendedFeatures.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.ExtendedFeatures.MQTT_5;
import static in.bytehue.messaging.mqtt5.api.ExtendedFeatures.RETAIN;
import static in.bytehue.messaging.mqtt5.api.ExtendedFeatures.USER_PROPERTIES;
import static java.lang.Long.MAX_VALUE;
import static java.util.Collections.emptyMap;
import static org.osgi.service.messaging.Features.QOS;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.propertytypes.MessagingFeature;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder.Send.Complete;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;

@Component
@MessagingFeature(name = "message-publisher", protocol = MQTT_5)
public final class SimpleMessagePublisher implements MessagePublisher {

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
        final long messageExpiryInterval = (long) extensions.getOrDefault(MESSAGE_EXPIRY_INTERVAL, MAX_VALUE);
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
                                  .messageExpiryInterval(messageExpiryInterval)
                                  .payload(content)
                                  .qos(MqttQos.fromCode(qos))
                                  .retain(retain)
                                  .userProperties(propsBuilder.build());
        // @formatter:on
        final String replyToChannel = context.getReplyToChannel();
        if (replyToChannel != null) {
            publishRequest.responseTopic(replyToChannel).correlationData(correlationId.getBytes());
        }
        publishRequest.send();
    }

}