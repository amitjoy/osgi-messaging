/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.messaging.MessageContext;

/**
 * {@link MqttMessageCorrelationIdGenerator} is primarily used to provide the functionality
 * for generating correlation identifiers required for reply-to channels. User can either
 * provide the identifiers by setting them directly through {@link MessageContext} or let
 * them be generated automatically. Users can provide multiple implementations of this service
 * and configure the {@link MessageContext} using {@link MqttMessageContextBuilder} where one
 * set the service filter.
 *
 * <p>
 * This is actually useful where you can introduce multiple reply-to channels and the correlation
 * identifiers can be generated in a unique way to identify them properly.
 *
 * @see MessageContext
 * @See MqttMessageContextBuilder
 *
 * @since 1.0
 */
@ConsumerType
@FunctionalInterface
public interface MqttMessageCorrelationIdGenerator {

    /**
     * Returns the generated identifier (cannot be (@code null))
     *
     * @return the generated identifier
     */
    String generate();

}
