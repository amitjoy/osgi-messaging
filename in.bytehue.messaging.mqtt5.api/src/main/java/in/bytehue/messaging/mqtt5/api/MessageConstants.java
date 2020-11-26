package in.bytehue.messaging.mqtt5.api;

/**
 * Defines standard constants for the MQTT messaging
 */
public final class MessageConstants {

    private MessageConstants() {
        throw new IllegalAccessError("Non-Instantiable");
    }

    /**
     * The name of the MQTT {@code Protocol} that conforms to the Messaging specification
     *
     * @since 1.0
     */
    public static final String MQTT_PROTOCOL = "mqtt5";

    /**
     * The identifier of the {@code Messaging} instance
     *
     * @since 1.0
     */
    public static final String MESSAGING_ID = "mqtt5-hivemq-adapter";

    /**
     * Defines standard constants for the MQTT messaging component names
     */
    public static final class Component {

        private Component() {
            throw new IllegalAccessError("Non-Instantiable");
        }

        /**
         * The name of the {@code Message Context Builder}
         *
         * @since 1.0
         */
        public static final String MESSAGE_CONTEXT_BUILDER = "mqtt5-message-context-builder";

        /**
         * The name of the {@code Message Whiteboard}
         *
         * @since 1.0
         */
        public static final String MESSAGE_WHITEBOARD = "mqtt5-message-whiteboard";

        /**
         * The name of the {@code Message Runtime}
         *
         * @since 1.0
         */
        public static final String MESSAGE_RUNTIME = "mqtt5-message-runtime";

        /**
         * The name of the {@code Message Publisher}
         *
         * @since 1.0
         */
        public static final String MESSAGE_PUBLISHER = "mqtt5-message-publisher";

        /**
         * The name of the {@code Message Subscriber}
         *
         * @since 1.0
         */
        public static final String MESSAGE_SUBSCRIBER = "mqtt5-message-subscriber";

        /**
         * The name of the {@code Message Reply-To Publisher}
         *
         * @since 1.0
         */
        public static final String MESSAGE_REPLY_TO_PUBLISHER = "mqtt5-message-replyto-publisher";

        /**
         * The name of the {@code Implementation Provider} of the Messaging specification
         *
         * @since 1.0
         */
        public static final String PROVIDER = "MQTT-5-Provider-ByteHue";

    }

    /**
     * Defines standard constants for the MQTT messaging extension features
     */
    public static final class Extension {

        private Extension() {
            throw new IllegalAccessError("Non-Instantiable");
        }

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
    }

    /**
     * Defines standard constants for the MQTT messaging configuration PIDs
     */
    public static final class PID {

        private PID() {
            throw new IllegalAccessError("Non-Instantiable");
        }

        /**
         * The configuration PID to configure the internal executor in Reply-To publisher
         *
         * @since 1.0
         */
        public static final String PUBLISHER = "in.bytehue.messaging.publisher";

        /**
         * The configuration PID to configure the internal executor in Reply-To publisher
         *
         * @since 1.0
         */
        public static final String CLIENT = "in.bytehue.messaging.client";

    }

}