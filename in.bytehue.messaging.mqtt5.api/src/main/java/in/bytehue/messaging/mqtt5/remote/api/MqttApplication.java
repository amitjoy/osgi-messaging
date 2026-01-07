/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
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
package in.bytehue.messaging.mqtt5.remote.api;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.messaging.Message;
import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;

/**
 * The {@link MqttApplication} interface should be implemented by consumers to
 * manage remote resources effectively.
 *
 * <p>
 * Every application must provide the following service property:
 * <b>mqtt.application.id</b> - This property identifies the application.
 * </p>
 *
 * <ul>
 * <li>{@link MqttApplication#doPUT(String, Message, MqttMessageContextBuilder)}
 * implements a CREATE request for a resource identified in the supplied {@link Message}.</li>
 * <li>{@link MqttApplication#doGET(String, Message, MqttMessageContextBuilder)}
 * implements a READ request for a resource identified in the supplied {@link Message}.</li>
 * <li>{@link MqttApplication#doPOST(String, Message, MqttMessageContextBuilder)}
 * implements an UPDATE request for a resource identified in the supplied {@link Message}.</li>
 * <li>{@link MqttApplication#doDELETE(String, Message, MqttMessageContextBuilder)}
 * implements a DELETE request for a resource identified in the supplied {@link Message}.</li>
 * <li>{@link MqttApplication#doEXEC(String, Message, MqttMessageContextBuilder)}
 * performs application operations not necessarily tied to a specific resource.</li>
 * </ul>
 *
 * @since 1.0
 */
@ConsumerType
public interface MqttApplication {

    /**
     * The service property denoting the application identifier.
     */
    String APPLICATION_ID_PROPERTY = "mqtt.application.id";

    /**
     * Handles a READ request for a resource.
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to construct the response message
     * @return the response message as {@link Message}
     * @throws Exception if the request cannot be fulfilled due to invalid parameters or processing errors
     */
    default Message doGET(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Handles a CREATE request for a resource.
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to construct the response message
     * @return the response message as {@link Message}
     * @throws Exception if the request cannot be fulfilled due to invalid parameters or processing errors
     */
    default Message doPUT(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Handles an UPDATE request for a resource.
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to construct the response message
     * @return the response message as {@link Message}
     * @throws Exception if the request cannot be fulfilled due to invalid parameters or processing errors
     */
    default Message doPOST(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Handles a DELETE request for a resource.
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to construct the response message
     * @return the response message as {@link Message}
     * @throws Exception if the request cannot be fulfilled due to invalid parameters or processing errors
     */
    default Message doDELETE(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Performs application operations not necessarily tied to a specific resource.
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to construct the response message
     * @return the response message as {@link Message}
     * @throws Exception if the request cannot be fulfilled due to invalid parameters or processing errors
     */
    default Message doEXEC(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

}
