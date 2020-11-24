package in.bytehue.messaging.mqtt5.api;

public interface ExtendedFeatures {

    /**
     * The name of the {@code Protocol} that conforms to the Messaging specification
     *
     * @since 1.0
     */
    String MQTT_5 = "mqtt5";

    /**
     * The name of the {@code Message Expiry Interval} extension of the Messaging specification
     *
     * @since 1.0
     */
    String MESSAGE_EXPIRY_INTERVAL = "messageExpiryInterval";

    /**
     * The name of the {@code Message Retain} extension of the Messaging specification
     *
     * @since 1.0
     */
    String RETAIN = "retain";

    /**
     * The name of the {@code User Properties} extension of the Messaging specification
     *
     * @since 1.0
     */
    String USER_PROPERTIES = "userProperties";

}
