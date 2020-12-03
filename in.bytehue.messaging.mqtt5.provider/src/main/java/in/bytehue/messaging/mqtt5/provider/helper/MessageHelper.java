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
import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.ACKNOWLEDGED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.RECEIVED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.REJECTED;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;
import org.osgi.util.converter.Converter;

import com.hivemq.client.internal.mqtt.datatypes.MqttTopicImpl;
import com.hivemq.client.internal.mqtt.datatypes.MqttUserPropertiesImpl;
import com.hivemq.client.internal.mqtt.datatypes.MqttUtf8StringImpl;
import com.hivemq.client.internal.mqtt.message.publish.MqttPublish;
import com.hivemq.client.internal.mqtt.message.publish.MqttWillPublish;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import in.bytehue.messaging.mqtt5.provider.MessageContextProvider;

public final class MessageHelper {

    private MessageHelper() {
        throw new IllegalAccessError("Non-instantiable");
    }

    public static <T> T getService(final Class<T> clazz, final String filter, final BundleContext context) {
        try {
            final Collection<ServiceReference<T>> serviceReferences = context.getServiceReferences(clazz, filter);
            // @formatter:off
            final ToLongFunction<ServiceReference<?>> srFunc =
                    sr -> Optional.ofNullable(sr.getProperty(SERVICE_RANKING))
                                  .map(long.class::cast)
                                  .orElse(0L);
            // get the service with highest service ranking
            return serviceReferences.stream()
                    .sorted(
                            (sr1, sr2) -> Long.compare(
                                                srFunc.applyAsLong(sr1),
                                                srFunc.applyAsLong(sr2)))
                    .findFirst()
                    .map(context::getService)
                    .orElseThrow(() -> new RuntimeException("'" + clazz +"' service instance cannot be found"));
        } catch (final Exception e) {
            throw new RuntimeException("Service '" + clazz.getName() + "' cannot be retrieved", e);
        }
    }

    public static Message toMessage(
            final Mqtt5Publish publish,
            final MessageContext context,
            final MessageContextBuilder messageContextBuilder) {

        final MqttPublish pub = (MqttPublish) publish;
        final ByteBuffer payload = pub.getRawPayload();
        final String contentEncoding = publish
                                            .getPayloadFormatIndicator()
                                            .map(e -> e.name().toLowerCase())
                                            .orElse(null);

        final String contentType = publish.getContentType().map(MessageHelper::asString).orElse(null);
        final String channel = publish.getTopic().toString();
        final String correlationId = asString(pub.getRawCorrelationData());
        final int qos = publish.getQos().getCode();
        final boolean retain = publish.isRetain();
        final Mqtt5UserProperties properties = publish.getUserProperties();

        final Map<String, String> userProperties =  properties
                                                        .asList()
                                                        .stream()
                                                        .collect(toMap(
                                                                    e -> e.getName().toString(),
                                                                    e -> e.getValue().toString()));

        final Map<String, Object> extensions = new HashMap<>(context.getExtensions());
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

    public static ServiceReferenceDTO serviceReferenceDTO(final ServiceReference<?> ref) {
        final ServiceReferenceDTO dto = new ServiceReferenceDTO();

        dto.bundle = ref.getBundle().getBundleId();
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
            final Object property = properties.get(MESSAGING_PROTOCOL_PROPERTY);
            if (property != null && MESSAGING_PROTOCOL.equals(((String[]) property)[0])) {
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

    // @formatter:off
    public static boolean acknowledgeMessage(
            final Message message,
            final MessageContextProvider ctx,
            final Consumer<Message> interimConsumer) {
    // @formatter:on
        final AcknowledgeHandler protocolSpecificAcknowledgeHandler = ctx.protocolSpecificAcknowledgeHandler;

        // first verify if the protocol specific handler is okay with the received message
        if (protocolSpecificAcknowledgeHandler.acknowledge() || !protocolSpecificAcknowledgeHandler.reject()) {
            // message is received but not yet acknowledged
            ctx.acknowledgeState = RECEIVED;

            // check for the existence of filter
            if (ctx.acknowledgeFilter != null) {
                final boolean isAcknowledged = ctx.acknowledgeFilter.test(message);
                if (isAcknowledged) {
                    ctx.acknowledgeState = ACKNOWLEDGED;
                } else {
                    ctx.acknowledgeState = REJECTED;
                }
            } else {
                // if the filter is not set, automatically acknowledge the message
                ctx.acknowledgeState = ACKNOWLEDGED;
            }
            // invoke the pre- and post-handlers if the message is acknowledged
            if (ctx.acknowledgeState == ACKNOWLEDGED) {
                invokePreHandler(ctx, message);
                interimConsumer.accept(message);
                invokePostHandler(ctx, message);
            }
        } else {
            ctx.acknowledgeState = REJECTED;
        }
        return ctx.acknowledgeState == ACKNOWLEDGED;
    }

    private static void invokePreHandler(final MessageContextProvider ctx, final Message message) {
        if (ctx.acknowledgeHandler != null) {
            ctx.acknowledgeHandler.accept(message);
        }
    }

    private static void invokePostHandler(final MessageContextProvider ctx, final Message message) {
        if (ctx.acknowledgeConsumer != null) {
            ctx.acknowledgeConsumer.accept(message);
        }
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

    public static int getQoS(final Map<String, Object> extensions, final Converter converter) {
        final Object isGuaranteedDeliveryProp = extensions.getOrDefault(EXTENSION_GUARANTEED_DELIVERY, false);
        final Object isGuranteedOrderingProp = extensions.getOrDefault(EXTENSION_GUARANTEED_ORDERING, false);

        final boolean isGuaranteedDelivery = adaptTo(isGuranteedOrderingProp, boolean.class, converter);
        final boolean isGuranteedOrdering = adaptTo(isGuaranteedDeliveryProp, boolean.class, converter);

        if (isGuaranteedDelivery || isGuranteedOrdering) {
            return EXACTLY_ONCE.getCode();
        }
        return (int) extensions.getOrDefault(EXTENSION_QOS, DEFAULT_QOS.getCode());
    }

    // @formatter:off
    public static MqttWillPublish toLastWill(
            final String channel,
            final ByteBuffer payload,
            final int qos,
            final boolean isRetain,
            final long messageExpiryInterval,
            final String contentEncoding,
            final String contentType,
            final String responseChannel,
            final String correlationId,
            final Map<String, String> userProperties,
            final long delayInterval) {

        requireNonNull(channel, "Last will topic cannot be null");
        requireNonNull(userProperties, "User Properties cannot be null");

        final MqttTopicImpl topic = MqttTopicImpl.of(channel);
        final MqttQos qosInstance = MqttQos.fromCode(qos);

        Mqtt5PayloadFormatIndicator encoding = null;
        if (contentEncoding != null) {
            encoding = Mqtt5PayloadFormatIndicator.valueOf(contentEncoding);
        }

        MqttUtf8StringImpl cType = null;
        if (contentType != null) {
            cType = MqttUtf8StringImpl.of(contentType);
        }

        MqttTopicImpl replyTopic = null;
        if (responseChannel != null) {
            replyTopic = MqttTopicImpl.of(responseChannel);
        }

        ByteBuffer correlationData = null;
        if (correlationId != null) {
            correlationData = ByteBuffer.wrap(correlationId.getBytes());
        }

        final Mqtt5UserPropertiesBuilder propsBuilder = Mqtt5UserProperties.builder();
        userProperties.forEach(propsBuilder::add);

        final MqttUserPropertiesImpl props = (MqttUserPropertiesImpl) propsBuilder.build();

        return new MqttWillPublish(
                topic,
                payload,
                qosInstance,
                isRetain,
                messageExpiryInterval,
                encoding,
                cType,
                replyTopic,
                correlationData,
                props,
                delayInterval);
    }
    // @formatter:on

    public static <T> T adaptTo(final Object value, final Class<T> to, final Converter converter) {
        return converter.convert(value).to(to);
    }

    public static String asString(final MqttUtf8String string) {
        return string.toString();
    }

    public static String asString(final ByteBuffer buffer) {
        return new String(buffer.array(), UTF_8);
    }

}
