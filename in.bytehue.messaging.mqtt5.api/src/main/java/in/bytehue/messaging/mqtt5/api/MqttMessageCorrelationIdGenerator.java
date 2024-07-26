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
 * The {@link MqttMessageCorrelationIdGenerator} interface defines a strategy for
 * generating unique correlation identifiers that are used to manage reply-to channels 
 * in MQTT messaging scenarios.
 *
 * <p>
 * Users can implement this interface to provide custom correlation identifier generation
 * logic, or rely on existing implementations that adhere to this contract.
 * Implementations can be explicitly specified via the {@link MqttMessageContextBuilder} 
 * using an appropriate service filter.
 * </p>
 *
 * <p>
 * This is particularly useful when multiple reply-to channels are managed, requiring each 
 * correlation identifier to be unique to properly map responses to their originating requests.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * {@code
 * MqttMessageCorrelationIdGenerator generator = () -> UUID.randomUUID().toString();
 * String correlationId = generator.generate();
 * }
 * </pre>
 *
 * @see MessageContext
 * @see MqttMessageContextBuilder
 * @since 1.0
 */
@ConsumerType
@FunctionalInterface
public interface MqttMessageCorrelationIdGenerator {

    /**
     * Generates a unique correlation identifier for use in MQTT messaging.
     * 
     * @return a unique identifier, never {@code null}
     */
    String generate();
}