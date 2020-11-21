package org.osgi.service.messaging;

import java.nio.ByteBuffer;

/** A message object */
public interface Message {

    /**
     * Returns the payload of the message as {@link ByteBuffer}
     *
     * @return the payload of the message as {@link ByteBuffer}
     */
    ByteBuffer payload();

    /**
     * Returns the message context. This must not be <code>null</code>.
     *
     * @return the {@link MessageContext} instance of this message
     */
    MessageContext getContext();
}