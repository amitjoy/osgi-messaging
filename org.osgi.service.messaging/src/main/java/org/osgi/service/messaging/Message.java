package org.osgi.service.messaging;

import java.nio.ByteBuffer;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a message in a messaging system, containing a payload and associated
 * context information.
 * 
 * <p>
 * This interface provides methods to access the raw payload of the message and its
 * context, which includes metadata and other contextual information necessary for
 * message processing.
 * </p>
 */
@ProviderType
public interface Message {

    /**
     * Returns the payload of the message as a {@link ByteBuffer}.
     * 
     * <p>
     * The payload contains the raw data that the message is carrying. It is returned
     * as a {@link ByteBuffer}, which allows efficient manipulation of binary data.
     * The buffer's position, limit, and capacity properties should be used to handle
     * the data correctly.
     * </p>
     *
     * @return the payload of the message as a {@link ByteBuffer}. This should never
     *         be {@code null}; an empty message should return an empty buffer.
     */
    ByteBuffer payload();

    /**
     * Returns the context of the message, which provides additional metadata
     * and contextual information required for processing the message.
     * 
     * <p>
     * The message context contains various properties that describe the message,
     * such as headers, correlation identifiers, routing information, and other 
     * relevant details. The returned {@link MessageContext} instance should provide
     * all necessary information to properly handle and route the message in the 
     * messaging system.
     * </p>
     *
     * @return the {@link MessageContext} instance of this message. This method must
     *         not return {@code null}; a valid {@link MessageContext} should always
     *         be associated with a message.
     */
    MessageContext getContext();
}