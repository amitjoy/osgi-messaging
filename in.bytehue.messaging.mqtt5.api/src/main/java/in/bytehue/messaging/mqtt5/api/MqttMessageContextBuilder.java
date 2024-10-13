/*******************************************************************************
 * 
 * © 2020-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.api;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;

/**
 * The {@link MqttMessageContextBuilder} service provides an interface for 
 * constructing MQTT 5.0 messages.
 *
 * <p>
 * <strong>Note:</strong> Access to this service requires the 
 * {@code ServicePermission[MqttMessageContextBuilder, GET]} permission.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by consumers.
 * @noextend This interface is not intended to be extended by consumers.
 *
 * @ThreadSafe
 * @since 1.0
 *
 * @see MessageContextBuilder
 */
@ProviderType
public interface MqttMessageContextBuilder extends MessageContextBuilder {

    /**
     * Sets the provided {@link MessageContext} instance. If this context is set,
     * calling message context builder functions on this builder will not override
     * the values from the given context.
     *
     * @param context an existing context
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder withContext(MessageContext context);

    /**
     * Adds content to the message.
     *
     * @param byteBuffer the content
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder content(ByteBuffer byteBuffer);

    /**
     * Adds typed content to the message and maps it using the provided mapping
     * function.
     *
     * @param <T> the content type
     * @param object the input object
     * @param contentMapper a mapping function to map T into the {@link ByteBuffer}
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    <T> MqttMessageContextBuilder content(T object, Function<T, ByteBuffer> contentMapper);

    /**
     * Defines a reply-to address for the message. The receiver will use this
     * address to send the reply.
     *
     * @param replyToAddress the reply address
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder replyTo(String replyToAddress);

    /**
     * Defines a correlation ID used for reply-to requests. The correlation ID
     * is an identifier to match a response with its corresponding request.
     *
     * @param correlationId the correlation ID
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder correlationId(String correlationId);

    /**
     * Defines a service filter used to generate a correlation ID. The correlation ID
     * is an identifier to match a response with its corresponding request.
     *
     * @param filter the correlation ID generator filter
     * @return the {@link MqttMessageContextBuilder} instance
     */
    MqttMessageContextBuilder correlationIdGenerator(String filter);

    /**
     * Defines content encoding.
     *
     * @param contentEncoding the content encoding
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder contentEncoding(String contentEncoding);

    /**
     * Defines the content type, such as the content mime type.
     *
     * @param contentType the content type
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder contentType(String contentType);

    /**
     * Defines a channel name and a routing key.
     *
     * @param channelName the channel name
     * @param channelExtension the special key for routing a message
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder channel(String channelName, String channelExtension);

    /**
     * Defines a channel name that can be a topic or queue name.
     *
     * @param channelName the channel name
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder channel(String channelName);

    /**
     * Adds an options entry with the given key and value.
     *
     * @param key the option/property key
     * @param value the option value
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder extensionEntry(String key, Object value);

    /**
     * Appends the given options to the context options.
     *
     * @param options the options map to be added to the existing options
     * @return the {@link MqttMessageContextBuilder} instance
     */
    @Override
    MqttMessageContextBuilder extensions(Map<String, Object> extension);

    /**
     * Sets the quality of service (QoS).
     *
     * @param qos the QoS value
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withQoS(final int qos) {
        extensionEntry(EXTENSION_QOS, qos);
        return this;
    }

    /**
     * Sets the retain flag for MQTT communication.
     *
     * @param retain {@code true} to retain the messages, otherwise {@code false}
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withRetain(final boolean retain) {
        extensionEntry(RETAIN, retain);
        return this;
    }

    /**
     * Sets the message expiry interval for MQTT communication.
     *
     * @param interval the expiry interval in seconds
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withMessageExpiryInterval(final long interval) {
        extensionEntry(MESSAGE_EXPIRY_INTERVAL, interval);
        return this;
    }

    /**
     * Sets user-specified properties for MQTT communication.
     *
     * @param userProperties the user properties
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withUserProperties(final Map<String, String> userProperties) {
        extensionEntry(USER_PROPERTIES, userProperties);
        return this;
    }

    /**
     * Sets the flag to receive own messages.
     *
     * @param receiveLocal {@code true} to receive own messages, otherwise {@code false}
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withReceiveLocal(final boolean receiveLocal) {
        extensionEntry(RECEIVE_LOCAL, receiveLocal);
        return this;
    }
}
