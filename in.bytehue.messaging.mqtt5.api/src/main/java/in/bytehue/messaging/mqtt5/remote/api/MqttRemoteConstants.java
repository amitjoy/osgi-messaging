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

/**
 * Standard constants for the MQTT Remote Resource Management
 */
public final class MqttRemoteConstants {

	/**
	 * Non-instantiable
	 */
	private MqttRemoteConstants() {
		throw new IllegalAccessError("Non-Instantiable");
	}

	/**
	 * The configuration persistent identifier for the remote resource management
	 *
	 * @since 1.0
	 */
	public static final String REMOTE_RESOURCE_MANAGEMENT_PID = "in.bytehue.messaging.mqtt5.remote";

	/**
	 * The response code property
	 *
	 * @since 1.0
	 */
	public static final String RESPONSE_CODE_PROPERTY = "response.code";

	/**
	 * The response exception message property
	 *
	 * @since 1.0
	 */
	public static final String RESPONSE_EXCEPTION_MESSAGE_PROPERTY = "response.exception.message";

	/**
	 * The response code when everything goes well
	 *
	 * @since 1.0
	 */
	public static final int RESPONSE_CODE_OK = 200;

	/**
	 * The response code when the request is malformed
	 *
	 * @since 1.0
	 */
	public static final int RESPONSE_CODE_BAD_REQUEST = 400;

	/**
	 * The response code when the endpoint cannot be found
	 *
	 * @since 1.0
	 */
	public static final int RESPONSE_CODE_NOT_FOUND = 404;

	/**
	 * The response code when the MQTT application results in an error
	 *
	 * @since 1.0
	 */
	public static final int RESPONSE_CODE_ERROR = 500;

	/**
	 * The name of the {@code Remote Resource Management} implementation
	 *
	 * @since 1.0
	 */
	public static final String REMOTE_RESOURCE_MANAGEMENT_IMPLEMENTATION = "mqtt.remote";

	/**
	 * The version of the {@code Remote Resource Management} implementation
	 *
	 * @since 1.0
	 */
	public static final String REMOTE_RESOURCE_MANAGEMENT_VERSION = "1.0.0";

}
