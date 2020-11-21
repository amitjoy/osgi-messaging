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

    public static class Features {

        private Features() {
            // non-instantiable
        }

        /**
         * The name of the the {@code Message Context Builder} feature of the Messaging specification
         *
         * @since 1.0
         */
        public static final String MESSAGE_CONTEXT_BUILDER = "messageContextBuilder";

        /**
         * The name of the the {@code Reply-To} feature of the Messaging specification
         *
         * @since 1.0
         */
        public static final String REPLY_TO = "replyTo";

        /**
         * The name of the the {@code Reply-To-May-Publish} feature of the Messaging specification
         *
         * @since 1.0
         */
        public static final String REPLY_TO_MANY_PUBLISH = "replyToManyPublish";

        /**
         * The name of the the {@code Reply-To-May-Subscribe} feature of the Messaging specification
         *
         * @since 1.0
         */
        public static final String REPLY_TO_MANY_SUBSCRIBE = "replyToManySubscribe";

        /**
         * The name of the the {@code Correlation ID Generation} feature of the Messaging specification
         *
         * @since 1.0
         */
        public static final String GENERATE_CORRELATION_ID = "generateCorrelationId";

        /**
         * The name of the the {@code Reply Channel Generation} feature of the Messaging specification
         *
         * @since 1.0
         */
        public static final String GENERATE_REPLY_CHANNEL = "generateReplyChannel";

        /**
         * The name of the the {@code Acknowledge} feature of the Messaging specification
         *
         * @since 1.0
         */
        public static final String ACKNOWLEDGE = "acknowledge";
    }

    public static class Extensions {

        private Extensions() {
            // non-instantiable
        }

        /**
         * The name of the the {@code Guaranteed Ordering} extension of the Messaging specification
         *
         * @since 1.0
         */
        public static final String GUARANTEED_ORDERING = "guaranteedOrdering";

        /**
         * The name of the the {@code Guaranteed Delivery} extension of the Messaging specification
         *
         * @since 1.0
         */
        public static final String GUARANTEED_DELIVERY = "guaranteedDelivery";

        /**
         * The name of the the {@code Automatic Acknowledgement} extension of the Messaging specification
         *
         * @since 1.0
         */
        public static final String AUTO_ACKNOWLEDGE = "autoAcknowledge";

        /**
         * The name of the the {@code Last Will} extension of the Messaging specification
         *
         * @since 1.0
         */
        public static final String LAST_WILL = "lastWill";

        /**
         * The name of the the {@code Quality of Service} extension of the Messaging specification
         *
         * @since 1.0
         */
        public static final String QOS = "qos";
    }

}
