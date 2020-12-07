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
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparingLong;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
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
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.MutablePair;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.log.Logger;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.acknowledge.AcknowledgeType;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.TypeReference;

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
            final Collection<ServiceReference<T>> references = context.getServiceReferences(clazz, filter);
            // @formatter:off
            final ToLongFunction<ServiceReference<?>> srFunc =
                    sr -> Optional.ofNullable(sr.getProperty(SERVICE_RANKING))
                                  .map(long.class::cast)
                                  .orElse(0L);
            // get the service with highest service ranking
            return references.stream()
                             .sorted(comparingLong(srFunc::applyAsLong).reversed())
                             .findFirst()
                             .map(context::getService)
                             .orElseThrow(() -> new RuntimeException("'" + clazz +"' service instance cannot be found"));
        } catch (final Exception e) {
            throw new RuntimeException("Service '" + clazz.getName() + "' cannot be retrieved", e);
        }
    }

    public static <T> Optional<T> getOptionalService(final Class<T> clazz, final String filter, final BundleContext context, final Logger logger) {
        try {
            final T service = getService(clazz, filter, context);
            return Optional.ofNullable(service);
        } catch (final Exception e) {
            logger.warn("Service '{}' cannot be retrieved", clazz.getName(), e);
            return Optional.empty();
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

    public static <T> ServiceReferenceDTO toServiceReferenceDTO(final Class<T> clazz, final BundleContext context) {
        final ServiceReference<T> ref = context.getServiceReference(clazz);
        return toServiceReferenceDTO(ref);
    }

    public static ServiceReferenceDTO toServiceReferenceDTO(final ServiceReference<?> ref) {
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

    // @formatter:off
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void acknowledgeMessage(
            final Message message,
            final MessageContextProvider ctx,
            final Consumer<Message> interimConsumer,
            final BundleContext context,
            final Logger logger) {

        // message is received but not yet acknowledged
        changeAcknowledgeState(message, RECEIVED);

        final MutablePair<String, Predicate<Message>> ackFilter = ctx.acknowledgeFilter;
        final String targetFilter = ackFilter.getLeft();
        final Predicate<Message> filter = ackFilter.getRight();

        Optional<Predicate> finalFilter = Optional.empty();

        // first check if the target service filter is set
        if (targetFilter != null) {
            finalFilter = getOptionalService(Predicate.class, targetFilter, context, logger);
        }
        // next check for the existence of filter as Predicate if no service is found that matches
        // the specified service filter
        if (!finalFilter.isPresent() && filter != null) {
            finalFilter = Optional.of(filter);
        }
        // next check if we have the final filter to execute
        if (finalFilter.isPresent()) {
            finalFilter.map(f -> f.test(message))
                       .ifPresent(isAcknowledged -> {
                           if (isAcknowledged) {
                               // acknowledge the message if the filter returns true
                               changeAcknowledgeState(message, ACKNOWLEDGED);
                               interimConsumer.accept(message);
                               // execute the post handler (if set) if the message is acknowledged
                               invokePostAcknowledgeHandler(message, ctx, context, logger);
                           } else {
                               // if the filter returns false, reject the message
                               changeAcknowledgeState(message, REJECTED);
                           }
                       });
        } else {
            // if we don't have any filter at all, execute the acknowledge handler if set
            invokeAcknowledgeHandler(message, ctx, context, logger);
            interimConsumer.accept(message);
            // then execute the post acknowledge handler if set
            invokePostAcknowledgeHandler(message, ctx, context, logger);
        }
    }

    private static void changeAcknowledgeState(final Message message, final AcknowledgeType type) {
        final MessageContextProvider context = (MessageContextProvider) message.getContext();
        context.acknowledgeState = type;
    }

    @SuppressWarnings("unchecked")
    private static void invokeAcknowledgeHandler(
            final Message message,
            final MessageContextProvider ctx,
            final BundleContext context,
            final Logger logger) {

        final MutablePair<String, Consumer<Message>> ackConsumer = ctx.acknowledgeHandler;
        final String targetFilter = ackConsumer.getLeft();
        final Consumer<Message> handler = ackConsumer.getRight();

        if (targetFilter != null) {
            // simply execute the handler since the acknowledge state should be handled by the user
            getOptionalService(Consumer.class, targetFilter, context, logger).ifPresent(a -> a.accept(message));
        } else if (handler != null) {
            // similarly here too
            handler.accept(message);
        } else {
            // if the handler is also not provided, we acknowledge it anyhow
            final MessageContextProvider ackContext = (MessageContextProvider) message.getContext();
            ackContext.acknowledgeState = ACKNOWLEDGED;
        }
    }

    @SuppressWarnings("unchecked")
    private static void invokePostAcknowledgeHandler(
            final Message message,
            final MessageContextProvider ctx,
            final BundleContext context,
            final Logger logger) {

        final MutablePair<String, Consumer<Message>> ackHandler = ctx.acknowledgeConsumer;
        final String targetFilter = ackHandler.getLeft();
        final Consumer<Message> handler = ackHandler.getRight();

        if (targetFilter != null) {
            getOptionalService(Consumer.class, targetFilter, context, logger).ifPresent(a -> a.accept(message));
        }
        if (handler != null) {
            handler.accept(message);
        }
    }
    // @formatter:on

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
        final Object dflt = extensions.getOrDefault(EXTENSION_QOS, DEFAULT_QOS.getCode());
        return adaptTo(dflt, int.class, converter);
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

    public static <T> T adapt(final Object value, final TypeReference<T> ref, final Converter converter) {
        return converter.convert(value).to(ref);
    }

    public static String asString(final MqttUtf8String string) {
        return string.toString();
    }

    public static String asString(final ByteBuffer buffer) {
        return new String(buffer.array(), UTF_8);
    }

}
