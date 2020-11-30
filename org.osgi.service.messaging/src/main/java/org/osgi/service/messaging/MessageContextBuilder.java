package org.osgi.service.messaging;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Builder for building a {@link Message} or {@link MessageContext} to configure publish or subscription properties
 */
@ProviderType
public interface MessageContextBuilder extends MessageContextProvider {

    /**
     * Sets the provided {@link MessageContext} instance. If this context is set,
     * calling message context builder functions on this builder will no override
     * the values from the given context.
     *
     * @param context an existing context
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder withContext(MessageContext context);

    /**
     * Adds the content to the message
     *
     * @param byteBuffer the content
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder content(ByteBuffer byteBuffer);

    /**
     * Adds typed content to the message and maps it using the provided mapping function
     *
     * @param <T> the content type
     * @param object the input object
     * @param contentMapper a mapping function to map T into the {@link ByteBuffer}
     * @return the {@link MessageContextBuilder} instance
     */
    <T> MessageContextBuilder content(T object, Function<T, ByteBuffer> contentMapper);

    /**
     * Defines a reply to address when submitting a reply-to request. So the receiver will
     * knows, where to send the reply.
     *
     * @param replyToAddress the reply address
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder replyTo(String replyToAddress);

    /**
     * Defines a correlation id that is usually used for reply-to requests.
     *
     * The correlation id is an identifier to assign a response to its corresponding request.
     *
     * This options can be used when the underlying system doesn't provide the generation of these
     * correlation ids
     *
     * @param correlationId the correlationId
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder correlationId(String correlationId);

    /**
     * Defines a content encoding
     *
     * @param content the content encoding
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder contentEncoding(String contentEncoding);

    /**
     * Defines a content-type like the content mime-type.
     *
     * @param contentType the content type
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder contentType(String contentType);

    /**
     * Defines a channel name and a routing key
     *
     * @param channelName the channel name
     * @param channelExtension the special key for routing a message
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder channel(String channelName, String channelExtension);

    /**
     * Defines a channel name that can be a topic or queue name
     *
     * @param channelName the channel name
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder channel(String channelName);

    /**
     * Adds an options entry with the given key and the given value
     *
     * @param key the option/property key
     * @param value the option value
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder extensionEntry(String key, Object value);

    /**
     * Appends the given options to the context options
     *
     * @param options the options map to be added to the options
     * @return the {@link MessageContextBuilder} instance
     */
    MessageContextBuilder extensions(Map<String, Object> extension);

}