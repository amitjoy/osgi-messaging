/*******************************************************************************
 * Copyright 2021 Amit Kumar Mondal
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

/**
 * Standard constants for the {@code MQTT Messaging}
 */
public final class MqttMessageConstants {

    private static final String NON_INSTANTIABLE = "Non-Instantiable";

    /**
     * Non-instantiable
     */
    private MqttMessageConstants() {
        throw new IllegalAccessError(NON_INSTANTIABLE);
    }

    /**
     * The name of the MQTT {@code Protocol} that conforms to the {@code Messaging} specification
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
     * The name of the provider of the {@code MQTT Messaging} implementation
     *
     * @since 1.0
     */
    public static final String MESSAGING_PROVIDER = "Byte Hue";

    /**
     * The name of framework property to be queried for client identifier if there exists no
     * specified configuration in {@link ConfigurationPid#CLIENT}.
     *
     * @since 1.0
     */
    public static final String CLIENT_ID_FRAMEWORK_PROPERTY = "in.bytehue.client.id";

    /**
     * The name of property which is available to the service that gets registered
     * when there exists a valid connection to the MQTT broker.
     *
     * @since 1.0
     */
    public static final String MQTT_CONNECTION_READY_SERVICE_PROPERTY = "mqtt.connection.ready";

    /**
     * The service filter to track the OSGi service which gets registered
     * when there exists a valid connection to the MQTT broker.
     *
     * @since 1.0
     */
    public static final String MQTT_CONNECTION_READY_SERVICE_PROPERTY_FILTER = "("
            + MQTT_CONNECTION_READY_SERVICE_PROPERTY + "=true)";

    /**
     * Standard constants for the {@code MQTT Messaging} extension features
     */
    public static final class Extension {

        /**
         * Non-instantiable
         */
        private Extension() {
            throw new IllegalAccessError(NON_INSTANTIABLE);
        }

        /**
         * The name of the {@code Message Expiry Interval} extension of the {@code MQTT 5.0 specification}.
         * An integer value indicates the expiry interval of the messages.
         *
         * @since 1.0
         */
        public static final String MESSAGE_EXPIRY_INTERVAL = "messageExpiryInterval";

        /**
         * The name of the {@code Message Retain} extension of the {@code MQTT 5.0 specification}.
         * This indicates the message will be retained. A value of {code true} ensures
         * successful retention of messages.
         *
         * @since 1.0
         */
        public static final String RETAIN = "retain";

        /**
         * The name of the {@code User Properties} extension of the {@code MQTT 5.0 specification}.
         * A {@code Map<String, String>} can be provided.
         *
         * @since 1.0
         */
        public static final String USER_PROPERTIES = "userProperties";

        /**
         * The name of the {@code Local} extension of the {@code MQTT 5.0 specification}. This ensures
         * if we want to receive our own messages. A value of {code true} ascertains the receipt
         * of own messages.
         *
         * @since 1.0
         */
        public static final String RECEIVE_LOCAL = "receiveLocal";

        /**
         * The name of the {@code last will delay interval} extension of the {@code MQTT 5.0 specification}.
         * A value of {@code Long} can be provided.
         *
         * @since 1.0
         */
        public static final String LAST_WILL_DELAY_INTERVAL = "lastWillDelayInterval";
    }

    /**
     * Defines standard constants for the MQTT messaging configuration PIDs
     */
    public static final class ConfigurationPid {

        /**
         * Non-instantiable
         */
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
