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
package in.bytehue.messaging.mqtt5.provider.helper;

import static com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE;
import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator.UTF_8;
import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.KEY_PROTOCOL;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toMap;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.ACKNOWLEDGED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.RECEIVED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.REJECTED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.UNSUPPORTED;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;

import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import in.bytehue.messaging.mqtt5.provider.MessageContextProvider;

public final class MessageHelper {

    private MessageHelper() {
        throw new IllegalAccessError("Non-instantiable");
    }

    public static <T> T getService(final Class<T> clazz, final String filter, final BundleContext context) {
        try {
            final Collection<ServiceReference<T>> serviceReferences = context.getServiceReferences(clazz, filter);
            // get the service with highest service ranking
            // @formatter:off
            return serviceReferences.stream()
                    .sorted(
                            (sr1, sr2) -> Long.compare(
                                    (long) sr1.getProperty(SERVICE_RANKING),
                                    (long) sr2.getProperty(SERVICE_RANKING)))
                    .findFirst()
                    .map(context::getService)
                    .orElseThrow(() -> new RuntimeException("'" + clazz +"' service instance cannot be found"));
            // @formatter:on
        } catch (final Exception e) {
            throw new RuntimeException("Service '" + clazz.getName() + "' cannot be retrieved", e);
        }
    }

    public static Message toMessage(final Mqtt5Publish publish, final MessageContextBuilder messageContextBuilder) {
        final ByteBuffer payload = publish.getPayload().orElse(null);
        // @formatter:off
        final String contentEncoding = publish
                                            .getPayloadFormatIndicator()
                                            .filter(e -> e == UTF_8)
                                            .map(e -> "UTF-8")
                                            .orElse(null);

        final String contentType = publish.getContentType().map(MessageHelper::asString).orElse(null);
        final String channel = publish.getTopic().toString();
        final String correlationId = publish.getCorrelationData().map(MessageHelper::asString).orElse(null);
        final int qos = publish.getQos().getCode();
        final boolean retain = publish.isRetain();
        final Mqtt5UserProperties properties = publish.getUserProperties();

        final Map<String, String> userProperties =  properties
                                                        .asList()
                                                        .stream()
                                                        .collect(toMap(
                                                                    e -> e.getName().toString(),
                                                                    e -> e.getValue().toString()));

        final Map<String, Object> extensions = new HashMap<>();
        extensions.put(EXTENSION_QOS, qos);
        extensions.put(RETAIN, retain);
        extensions.put(USER_PROPERTIES, userProperties);

        return messageContextBuilder.channel(channel)
                                    .content(payload)
                                    .contentType(contentType)
                                    .contentEncoding(contentEncoding)
                                    .correlationId(correlationId)
                                    .extensions(extensions)
                                    .buildMessage();
        // @formatter:on
    }

    public static ServiceReferenceDTO serviceReferenceDTO(final ServiceReference<?> ref, final long bundleId) {
        final ServiceReferenceDTO dto = new ServiceReferenceDTO();

        dto.bundle = bundleId;
        dto.id = (Long) ref.getProperty(SERVICE_ID);
        dto.properties = new HashMap<>();

        for (final String key : ref.getPropertyKeys()) {
            final Object val = ref.getProperty(key);
            dto.properties.put(key, getDTOValue(val));
        }

        final Bundle[] usingBundles = ref.getUsingBundles();

        if (usingBundles == null) {
            dto.usingBundles = new long[0];
        } else {
            dto.usingBundles = new long[usingBundles.length];
            for (int j = 0; j < usingBundles.length; j++) {
                dto.usingBundles[j] = usingBundles[j].getBundleId();
            }
        }
        return dto;
    }

    public static Object getDTOValue(final Object value) {
        Class<?> c = value.getClass();
        if (c.isArray()) {
            c = c.getComponentType();
        }
        if ( // @formatter:off
                Number.class.isAssignableFrom(c)  ||
                Boolean.class.isAssignableFrom(c) ||
                String.class.isAssignableFrom(c)  ||
                DTO.class.isAssignableFrom(c)) {
            // @formatter:on
            return value;
        }
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            final String[] converted = new String[length];
            for (int i = 0; i < length; i++) {
                converted[i] = String.valueOf(Array.get(value, i));
            }
            return converted;
        }
        return String.valueOf(value);
    }

    public static ServiceReferenceDTO getDTOFromClass(final Class<?> clazz, final BundleContext bundleContext) {
        boolean isProtocolCompliant = false;

        final ServiceReferenceDTO[] services = bundleContext.getBundle().adapt(ServiceReferenceDTO[].class);
        for (final ServiceReferenceDTO serviceDTO : services) {
            final Map<String, Object> properties = serviceDTO.properties;
            final String[] serviceTypes = (String[]) properties.get(OBJECTCLASS);
            if (MESSAGING_PROTOCOL.equals(properties.get(KEY_PROTOCOL))) {
                isProtocolCompliant = true;
            }
            for (final String type : serviceTypes) {
                if (clazz.getName().equals(type) && isProtocolCompliant) {
                    return serviceDTO;
                }
            }
        }
        return null;
    }

    public static boolean acknowledgeMessage(final Message message, final MessageContextProvider ctx,
            final Consumer<Message> interimConsumer) {
        // first verify if the protocol specific handler is okay with the received message
        final AcknowledgeHandler protocolSpecificAcknowledgeHandler = ctx.protocolSpecificAcknowledgeHandler;
        if (protocolSpecificAcknowledgeHandler.acknowledge() || !protocolSpecificAcknowledgeHandler.reject()) {
            ctx.acknowledgeState = RECEIVED;
            if (ctx.acknowledgeFilter != null) {
                final boolean isAcknowledged = ctx.acknowledgeFilter.test(message);
                if (isAcknowledged) {
                    ctx.acknowledgeState = ACKNOWLEDGED;
                    if (ctx.acknowledgeHandler != null) {
                        ctx.acknowledgeHandler.accept(message);
                    }
                    interimConsumer.accept(message);
                } else {
                    ctx.acknowledgeState = REJECTED;
                }
            } else {
                interimConsumer.accept(message);
            }
            if (ctx.acknowledgeConsumer != null) {
                ctx.acknowledgeConsumer.accept(message);
            }
        } else {
            ctx.acknowledgeState = UNSUPPORTED;
        }
        return ctx.acknowledgeState == ACKNOWLEDGED;
    }

    public static Message prepareExceptionAsMessage(final Throwable t, final MessageContextBuilder mcb) {
        final Optional<String> message = Optional.ofNullable(t.getMessage());
        return mcb.content(ByteBuffer.wrap(message.orElse(stackTraceToString(t)).getBytes())).buildMessage();
    }

    public static String stackTraceToString(final Throwable t) {
        final StringBuilder result = new StringBuilder(t.toString()).append(lineSeparator());
        final StackTraceElement[] trace = t.getStackTrace();
        Stream.of(trace).forEach(e -> result.append(e.toString()).append(lineSeparator()));
        return result.toString();
    }

    public static int getQoS(final Map<String, Object> extensions) {
        // guaranteed deliver > guaranteed ordering > specified qos
        final boolean isGuaranteedDelivery = (boolean) extensions.getOrDefault(EXTENSION_GUARANTEED_DELIVERY, false);
        if (!isGuaranteedDelivery) {
            final boolean isGuranteedOrdering = (boolean) extensions.getOrDefault(EXTENSION_GUARANTEED_ORDERING, false);
            if (!isGuranteedOrdering) {
                return (int) extensions.getOrDefault(EXTENSION_QOS, DEFAULT_QOS.getCode());
            }
        }
        return EXACTLY_ONCE.getCode();
    }

    public static String asString(final MqttUtf8String string) {
        return asString(string.toByteBuffer());
    }

    public static String asString(final ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

}
