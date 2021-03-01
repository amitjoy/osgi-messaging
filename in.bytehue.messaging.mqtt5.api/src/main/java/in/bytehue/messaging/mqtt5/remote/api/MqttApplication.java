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
package in.bytehue.messaging.mqtt5.remote.api;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.messaging.Message;

import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;

/**
 * This interface {@link MqttApplication} should be implemented by consumers to leverage remote resource management
 *
 * <p>
 * Every application must provide the following service property:
 * <p>
 * <b>mqtt.application.id</b> - that will be used to identify this application
 *
 * <ul>
 * <li>{@link MqttApplication#doGet} is used to implement a READ request for a resource identified in the supplied
 * {@link Message)}</li>
 * <li>{@link MqttApplication#doPut} is used to implement a CREATE or UPDATE request for a resource identified in the
 * supplied {@link Message}</li>
 * <li>{@link MqttApplication#doDelete} is used to implement a DELETE request for a resource identified in the supplied
 * {@link Message}</li>
 * <li>{@link MqttApplication#doPost} is used to implement other operations on a resource identified in the supplied
 * {@link Message}</li>
 * <li>{@link MqttApplication#doExec} is used to perform application operation not necessary tied to a given
 * resource.</li>
 * </ul>
 *
 * @since 1.0
 */
@ConsumerType
public interface MqttApplication {

    /**
     * The service property to be set
     */
    String APPLICATION_ID_PROPERTY = "mqtt.application.id";

    /**
     * Used to implement a READ request for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doGET( //
            final String resource, //
            final Message requestMessage, //
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to implement a CREATE or UPDATE request for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doPUT( //
            final String resource, //
            final Message requestMessage, //
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to implement other operations for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doPOST( //
            final String resource, //
            final Message requestMessage, //
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to implement a DELETE request for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doDELETE( //
            final String resource, //
            final Message requestMessage, //
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to perform application operation not necessary tied to a given resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doEXEC( //
            final String resource, //
            final Message requestMessage, //
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

}
