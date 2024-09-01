package org.osgi.service.messaging;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Builder for creating a {@link Message} or {@link MessageContext} and configuring
 * publish or subscription properties.
 */
@ProviderType
public interface MessageContextBuilder extends MessageContextProvider {

	/**
	 * Sets the specified {@link MessageContext} instance. If this context is set,
	 * subsequent method calls on this builder will not override the values from
	 * the provided context.
	 *
	 * @param context an existing message context
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder withContext(MessageContext context);

	/**
	 * Adds the content to the message.
	 *
	 * @param byteBuffer the content as a ByteBuffer
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder content(ByteBuffer byteBuffer);

	/**
	 * Adds typed content to the message and maps it using the provided function.
	 *
	 * @param <T>           the type of the content
	 * @param object        the input object
	 * @param contentMapper a function to map the content type T into a {@link ByteBuffer}
	 * @return the current {@link MessageContextBuilder} instance
	 */
	<T> MessageContextBuilder content(T object, Function<T, ByteBuffer> contentMapper);

	/**
	 * Sets a reply-to address for the message, which indicates where replies 
	 * should be sent.
	 *
	 * @param replyToAddress the address to send replies to
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder replyTo(String replyToAddress);

	/**
	 * Sets a correlation ID, typically used for reply-to requests, to associate
	 * a response with its corresponding request.
	 *
	 * This can be useful when the underlying system does not automatically
	 * generate correlation IDs.
	 *
	 * @param correlationId the correlation identifier
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder correlationId(String correlationId);

	/**
	 * Specifies the content encoding to be used.
	 *
	 * @param contentEncoding the encoding of the content
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder contentEncoding(String contentEncoding);

	/**
	 * Specifies the content type, such as the MIME type of the content.
	 *
	 * @param contentType the type of the content
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder contentType(String contentType);

	/**
	 * Defines a channel name and a routing key for message delivery.
	 *
	 * @param channelName      the name of the channel
	 * @param channelExtension a key for routing the message
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder channel(String channelName, String channelExtension);

	/**
	 * Sets the channel name, which can be a topic or queue name.
	 *
	 * @param channelName the name of the channel
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder channel(String channelName);

	/**
	 * Adds an extension entry with the specified key and value to the message context.
	 *
	 * @param key   the key of the extension
	 * @param value the value of the extension
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder extensionEntry(String key, Object value);

	/**
	 * Appends the given map of extensions to the existing context options.
	 *
	 * @param extensions a map containing extension keys and their corresponding values
	 * @return the current {@link MessageContextBuilder} instance
	 */
	MessageContextBuilder extensions(Map<String, Object> extensions);

}