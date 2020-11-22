package org.osgi.service.messaging;

/**
 * Defines standard constants for the messaging specification.
 */
public final class MessagingConstants {

    private MessagingConstants() {
        // non-instantiable
    }

    /**
     * The name of the implementation capability for the Messaging specification
     *
     * @since 1.0
     */
    public static final String MESSAGING_IMPLEMENTATION = "osgi.messaging";

    /**
     * The name of the implementation capability for the Reply-To feature of the Messaging specification
     *
     * @since 1.0
     */
    public static final String REPLY_TO_IMPLEMENTATION = "osgi.messaging.replyto";

    /**
     * The name of the implementation capability for the Acknowledge feature of the Messaging specification
     *
     * @since 1.0
     */
    public static final String ACKNOWLEDGE_IMPLEMENTATION = "osgi.messaging.acknowledge";

    /**
     * The version of the implementation capability for the Messaging specification
     *
     * @since 1.0
     */
    public static final String MESSAGING_SPECIFICATION_VERSION = "1.0.0";

}
