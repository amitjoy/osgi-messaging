/*******************************************************************************
 * Copyright 2020 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.api;

import java.util.function.Predicate;

/**
 * Defines standard constants for the MQTT messaging
 */
public final class MqttMessageConstants {

    private static final String NON_INSTANTIABLE = "Non-Instantiable";

    private MqttMessageConstants() {
        throw new IllegalAccessError(NON_INSTANTIABLE);
    }

    /**
     * The name of the MQTT {@code Protocol} that conforms to the Messaging specification
     *
     * @since 1.0
     */
    public static final String MESSAGING_PROTOCOL = "mqtt5";

    /**
     * The identifier of the {@code Messaging} instance
     *
     * @since 1.0
     */
    public static final String MESSAGING_ID = "mqtt5-hivemq-adapter";

    /**
     * The name of the provider of the {@code Messaging} implementation
     *
     * @since 1.0
     */
    public static final String MESSAGING_PROVIDER = "bytehue";

    /**
     * The target {@link Predicate} filter for the reply-to many subscription handler
     *
     * @since 1.0
     */
    public static final String REPLY_TO_SUBSCRIPTION_END_CHANNEL_PROPERTY = "in.bytehue.replyToSubscription.predicate.filter";

    /**
     * Defines standard constants for the MQTT messaging extension features
     */
    public static final class Extension {

        private Extension() {
            throw new IllegalAccessError(NON_INSTANTIABLE);
        }

        /**
         * The name of the {@code Message Expiry Interval} extension of the MQTT v5 specification.
         * An integer value indicates the expiry interval of the messages.
         *
         * @since 1.0
         */
        public static final String MESSAGE_EXPIRY_INTERVAL = "messageExpiryInterval";

        /**
         * The name of the {@code Message Retain} extension of the MQTT v5 specification.
         * This indicates the message will be retained. A value of {code true} ensures
         * successful retention of messages.
         *
         * @since 1.0
         */
        public static final String RETAIN = "retain";

        /**
         * The name of the {@code User Properties} extension of the MQTT v5 specification.
         * A {@code Map&lt;String,String&gt; can be provided}.
         *
         * @since 1.0
         */
        public static final String USER_PROPERTIES = "userProperties";

        /**
         * The name of the {@code Local} extension of the MQTT v5 specification. This ensures
         * if we want to receive our own messages. A value of {code true} ascertains the receipt
         * of own messages.
         *
         * @since 1.0
         */
        public static final String RECEIVE_LOCAL = "receiveLocal";

        /**
         * The name of the {@code Reply-To Many End Predicate} extension of the messaging specification.
         * This ensures that when to close the reply-to many response connection.
         *
         * @since 1.0
         */
        public static final String REPLY_TO_MANY_PREDICATE = "replyToManyEndPredicate";

        /**
         * The name of the {@code Reply-To Many End Predicate} extension of the messaging specification.
         * This ensures that when to close the reply-to many response connection.
         *
         * @since 1.0
         */
        public static final String REPLY_TO_MANY_PREDICATE_FILTER = "replyToManyEndPredicateFilter";
    }

    /**
     * Defines standard constants for the MQTT messaging configuration PIDs
     */
    public static final class ConfigurationPid {

        private ConfigurationPid() {
            throw new IllegalAccessError(NON_INSTANTIABLE);
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
