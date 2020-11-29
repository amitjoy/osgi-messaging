package org.osgi.service.messaging;

/**
 * Defines standard constants for the messaging specification.
 */
public final class MessageConstants {

    private MessageConstants() {
        throw new IllegalAccessError("Non-Instantiable");
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

    /**
     * The target filter service property for the reply-to subscription handler
     *
     * @since 1.0
     */
    public static final String REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY = "osgi.messaging.replyToSubscription.target";

    /**
     * The request channel service property for the reply-to subscription handler
     *
     * @since 1.0
     */
    public static final String REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY = "osgi.messaging.replyToSubscription.channel";

    /**
     * The response channel service property for the reply-to subscription handler
     *
     * @since 1.0
     */
    public static final String REPLY_TO_SUBSCRIPTION_RESPONSE_CHANNEL_PROPERTY = "osgi.messaging.replyToSubscription.replyChannel";

    /**
     * The messaging name service property for the Messaging specification
     *
     * @since 1.0
     */
    public static final String MESSAGING_NAME_PROPERTY = "osgi.messaging.name";

    /**
     * The messaging feature service property for the Messaging specification
     *
     * @since 1.0
     */
    public static final String MESSAGING_FEATURE_PROPERTY = "osgi.messaging.feature";

    /**
     * The messaging protocol service property for the Messaging specification
     *
     * @since 1.0
     */
    public static final String MESSAGING_PROTOCOL_PROPERTY = "osgi.messaging.protocol";
}
