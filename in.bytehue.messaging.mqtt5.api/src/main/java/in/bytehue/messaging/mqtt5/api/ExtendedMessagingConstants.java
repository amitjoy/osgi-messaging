package in.bytehue.messaging.mqtt5.api;

public final class ExtendedMessagingConstants {

    private ExtendedMessagingConstants() {
        throw new IllegalAccessError("Non-Instantiable");
    }

    /**
     * The name of the {@code Protocol} that conforms to the Messaging specification
     *
     * @since 1.0
     */
    public static final String MQTT_PROTOCOL = "mqtt5";

    /**
     * The name of the {@code Messaging}
     *
     * @since 1.0
     */
    public static final String MQTT_MESSAGING_NAME = "mqtt5-hivemq-adapter";

    /**
     * The name of the {@code Message Context Builder}
     *
     * @since 1.0
     */
    public static final String MESSAGE_CONTEXT_BUILDER_NAME = "mqtt5-message-context-builder";

    /**
     * The name of the {@code Message Whiteboard}
     *
     * @since 1.0
     */
    public static final String MESSAGE_WHITEBOARD_NAME = "mqtt5-message-whiteboard";

    /**
     * The name of the {@code Message Runtime}
     *
     * @since 1.0
     */
    public static final String MESSAGE_RUNTIME_NAME = "mqtt5-message-runtime";

    /**
     * The name of the {@code Message Publisher}
     *
     * @since 1.0
     */
    public static final String MESSAGE_PUBLISHER_NAME = "mqtt5-message-publisher";

    /**
     * The name of the {@code Message Subscriber}
     *
     * @since 1.0
     */
    public static final String MESSAGE_SUBSCRIBER_NAME = "mqtt5-message-subscriber";

    /**
     * The name of the {@code Message Reply-To Publisher}
     *
     * @since 1.0
     */
    public static final String MESSAGE_REPLY_TO_PUBLISHER_NAME = "mqtt5-message-replyto-publisher";

    /**
     * The name of the {@code Message Expiry Interval} extension of the Messaging specification
     *
     * @since 1.0
     */
    public static final String MESSAGE_EXPIRY_INTERVAL = "messageExpiryInterval";

    /**
     * The name of the {@code Message Retain} extension of the Messaging specification
     *
     * @since 1.0
     */
    public static final String RETAIN = "retain";

    /**
     * The name of the {@code User Properties} extension of the Messaging specification
     *
     * @since 1.0
     */
    public static final String USER_PROPERTIES = "userProperties";

    /**
     * The name of the {@code Implementation Provider} of the Messaging specification
     *
     * @since 1.0
     */
    public static final String PROVIDER_NAME = "MQTT-5-Provider-ByteHue";

    /**
     * The configuration PID to configure the internal executor in Reply-To publisher
     *
     * @since 1.0
     */
    public static final String PUBLISHER_PID = "in.bytehue.messaging.publisher";

    /**
     * The configuration PID to configure the internal executor in Reply-To publisher
     *
     * @since 1.0
     */
    public static final String CLIENT_PID = "in.bytehue.messaging.client";

}
