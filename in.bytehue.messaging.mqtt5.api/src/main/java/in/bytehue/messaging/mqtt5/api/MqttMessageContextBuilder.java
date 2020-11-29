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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.REPLY_TO_MANY_PREDICATE;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;

import java.util.Map;
import java.util.function.Predicate;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;

/**
 * The {@link MqttMessageContextBuilder} service is the application access point to the
 * build a MQTT v5 message.
 *
 * <p>
 * <b>Note that<b>, access to this service requires the
 * {@code ServicePermission[Mqtt5MessageContextBuilder, GET]} permission. It is intended
 * that only administrative bundles should be granted this permission to limit
 * access to the potentially intrusive methods provided by this service.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by consumers.
 * @noextend This interface is not intended to be extended by consumers.
 *
 * @ThreadSafe
 * @since 1.0
 *
 * @see MessageContextBuilder
 */
public interface MqttMessageContextBuilder extends MessageContextBuilder {

    /**
     * Sets the retain flag for the MQTT communication.
     *
     * @param retain {@code true} to retain the messages, otherwise {@code false}
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withRetain(final boolean retain) {
        extensionEntry(RETAIN, retain);
        return this;
    }

    /**
     * Sets the message expiry interval for the MQTT communication.
     *
     * @param interval the interval to set
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withMessageExpiryInterval(final long interval) {
        extensionEntry(MESSAGE_EXPIRY_INTERVAL, interval);
        return this;
    }

    /**
     * Sets the user specified properties for the MQTT communication.
     *
     * @param userProperties the user properties
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withUserProperties(final Map<String, String> userProperties) {
        extensionEntry(USER_PROPERTIES, userProperties);
        return this;
    }

    /**
     * Sets the the flag to receive own messages
     *
     * @param receiveLocal {@code true} to receive own messages, otherwise {@code false}
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withReceiveLocal(final boolean receiveLocal) {
        extensionEntry(RECEIVE_LOCAL, receiveLocal);
        return this;
    }

    /**
     * Sets the the {@link Predicate} that is used to check when to end the Reply-To-Many request
     * connection
     *
     * @param predicate the {@link Predicate} instance
     * @return the {@link MqttMessageContextBuilder} instance
     */
    default MqttMessageContextBuilder withReplyToManyEndPredicate(final Predicate<Message> predicate) {
        extensionEntry(REPLY_TO_MANY_PREDICATE, predicate);
        return this;
    }

}
