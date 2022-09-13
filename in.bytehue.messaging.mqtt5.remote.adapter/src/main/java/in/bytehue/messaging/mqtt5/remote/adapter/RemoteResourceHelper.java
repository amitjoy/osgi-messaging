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
package in.bytehue.messaging.mqtt5.remote.adapter;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.CLIENT_ID_FRAMEWORK_PROPERTY;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CLIENT;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.messaging.Message;

public final class RemoteResourceHelper {

	private RemoteResourceHelper() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public enum MethodType {
		GET, POST, PUT, DELETE, EXEC
	}

	public static class RequestDTO extends DTO {
		String resource;
		MethodType method;
		String applicationId;
		Message requestMessage;
	}

	public static class MqttException extends RuntimeException {

		private static final long serialVersionUID = 4877572873981748364L;

		public final int code;

		public MqttException(final int code, final String message) {
			super(message);
			this.code = code;
		}
	}

	public static String clientID(final ConfigurationAdmin configurationAdmin, final BundleContext bundleContext) {
		try {
			final Configuration configuration = configurationAdmin.getConfiguration(CLIENT, "?");
			final Dictionary<String, Object> properties = configuration.getProperties();
			if (properties == null || properties.get("id") == null) {
				// check for framework property if available
				final String id = bundleContext.getProperty(CLIENT_ID_FRAMEWORK_PROPERTY);
				requireNonNull(id, "No MQTT Client ID has been assigned");
				return id;
			} else {
				return String.valueOf(properties.get("id"));
			}
		} catch (final IOException e) {
			// not gonna happen at all
		}
		return "+";
	}

	public static String exceptionToString(final Exception exception) {
		final StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

}
