package org.osgi.service.messaging;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Context object that can be used to provide additional properties that can
 * be put to the underlying driver / connection.
 *
 * The context holds meta-information for a message to be send or received
 */
@ProviderType
public interface MessageContext {

    /**
     * Returns a channel definition
     *
     * @return a channel definition
     */
    String getChannel();

    /**
     * Returns the content type like a mime-type
     *
     * @return the content type
     */
    String getContentType();

    /**
     * Returns the content encoding
     *
     * @return the content encoding
     */
    String getContentEncoding();

    /**
     * Returns the correlation id
     *
     * @return the correlation id
     */
    String getCorrelationId();

    /**
     * Returns the reply to channel
     *
     * @return the reply to channel
     */
    String getReplyToChannel();

    /**
     * Returns the options map for additional configurations. The returning map
     * can not be modified anymore
     *
     * @return the options map, must no be <code>null</code>
     */
    Map<String, Object> getExtensions();

}