package in.bytehue.messaging.mqtt5.provider.helper;

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator.UTF_8;
import static java.util.stream.Collectors.toMap;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.messaging.Features.QOS;
import static org.osgi.service.messaging.Features.RETAIN;
import static org.osgi.service.messaging.Features.USER_PROPERTIES;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.ACKOWLEDGED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.RECEIVED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.REJECTED;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;

import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import in.bytehue.messaging.mqtt5.provider.SimpleMessageContext;

public final class MessagingHelper {

    private MessagingHelper() {
        throw new IllegalAccessError("Non-instantiable");
    }

    public static <T> T getService(final Class<T> clazz, final String filter, final BundleContext context)
            throws InvalidSyntaxException {
        final Collection<ServiceReference<T>> serviceReferences = context.getServiceReferences(clazz, filter);
        // get the service with highest service ranking
        // @formatter:off
        return serviceReferences.stream()
                .sorted(
                        (sr1, sr2) -> Long.compare(
                                (long) sr1.getProperty(SERVICE_RANKING),
                                (long) sr2.getProperty(SERVICE_RANKING)))
                .findFirst()
                .map(sr -> context.getService(sr))
                .orElseThrow(() -> new RuntimeException("'" + clazz +"' service instance cannot be found"));
        // @formatter:on
    }

    public static Message toMessage(final Mqtt5Publish publish, final MessageContextBuilder messageContextBuilder) {
        final ByteBuffer payload = publish.getPayload().orElse(null);
        // @formatter:off
        final String contentEncoding = publish
                                        .getPayloadFormatIndicator()
                                        .filter(e -> e == UTF_8)
                                        .map(e -> "UTF-8")
                                        .orElse(null);
        // @formatter:on
        final String contentType = publish.getContentType().map(MessagingHelper::asString).orElse(null);
        final String channel = publish.getTopic().toString();
        final String correlationId = publish.getCorrelationData().map(MessagingHelper::asString).orElse(null);
        final int qos = publish.getQos().ordinal();
        final boolean retain = publish.isRetain();
        final Mqtt5UserProperties properties = publish.getUserProperties();

        // @formatter:off
        final Map<String, String> userProperties =  properties.asList()
                                                        .stream()
                                                        .collect(toMap(
                                                                    e -> e.getName().toString(),
                                                                    e -> e.getValue().toString()));
        // @formatter:on

        final Map<String, Object> extensions = new HashMap<>();
        extensions.put(QOS, qos);
        extensions.put(RETAIN, retain);
        extensions.put(USER_PROPERTIES, userProperties);

        // @formatter:off
        return messageContextBuilder.channel(channel)
                                    .content(payload)
                                    .contentType(contentType)
                                    .contentEncoding(contentEncoding)
                                    .correlationId(correlationId)
                                    .extensions(extensions)
                                    .buildMessage();
        // @formatter:on
    }

    public static void acknowledgeMessage(final Message message, final SimpleMessageContext ctx,
            final Consumer<Message> interimConsumer) {
        ctx.acknowledgeState = RECEIVED;
        if (ctx.acknowledgeFilter != null) {
            final boolean isAcknowledged = ctx.acknowledgeFilter.test(message);
            if (isAcknowledged) {
                ctx.acknowledgeState = ACKOWLEDGED;
                if (ctx.acknowledgeHandler != null) {
                    ctx.acknowledgeHandler.accept(message);
                }
                interimConsumer.accept(message);
            } else {
                ctx.acknowledgeState = REJECTED;
            }
        }
        if (ctx.acknowledgeConsumer != null) {
            ctx.acknowledgeConsumer.accept(message);
        }
    }

    private static String asString(final MqttUtf8String string) {
        return StandardCharsets.UTF_8.decode(string.toByteBuffer()).toString();
    }

    private static String asString(final ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

}