/*******************************************************************************
 * Copyright 2020-2024 Amit Kumar Mondal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.api;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.messaging.MessageContext;

/**
 * The {@link MqttMessageCorrelationIdGenerator} interface is designed for
 * generating unique correlation identifiers required for reply-to channels in
 * MQTT messaging.
 *
 * <p>
 * Users can either provide their own correlation identifiers by setting them
 * directly through {@link MessageContext}, or allow them to be generated
 * automatically. Multiple implementations of this service can be provided, and
 * the {@link MqttMessageContextBuilder} can be configured to use a specific
 * implementation by setting the appropriate service filter.
 * </p>
 *
 * <p>
 * This functionality is particularly useful for managing multiple reply-to
 * channels, ensuring that each correlation identifier is uniquely generated
 * to properly identify the associated channels.
 * </p>
 *
 * @see MessageContext
 * @see MqttMessageContextBuilder
 * @since 1.0
 */
@ConsumerType
@FunctionalInterface
public interface MqttMessageCorrelationIdGenerator {

    /**
     * Generates and returns a unique identifier.
     *
     * @return the generated identifier, never {@code null}
     */
    String generate();
}
