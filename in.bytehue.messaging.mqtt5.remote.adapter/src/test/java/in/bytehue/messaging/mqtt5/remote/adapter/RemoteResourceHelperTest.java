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
package in.bytehue.messaging.mqtt5.remote.adapter;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.CLIENT_ID_FRAMEWORK_PROPERTY;
import static in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.clientID;
import static in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.exceptionToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.MethodType;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.MqttException;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.RequestDTO;

public class RemoteResourceHelperTest {

	@Test
	public void test_private_constructor_throws() throws Exception {
		final Constructor<RemoteResourceHelper> constructor = RemoteResourceHelper.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThatThrownBy(() -> constructor.newInstance())
				.isInstanceOf(InvocationTargetException.class)
				.hasCauseInstanceOf(IllegalAccessError.class);
	}

	@Test
	public void test_method_type_enum() {
		assertThat(MethodType.valueOf("GET")).isEqualTo(MethodType.GET);
		assertThat(MethodType.valueOf("POST")).isEqualTo(MethodType.POST);
		assertThat(MethodType.valueOf("PUT")).isEqualTo(MethodType.PUT);
		assertThat(MethodType.valueOf("DELETE")).isEqualTo(MethodType.DELETE);
		assertThat(MethodType.valueOf("EXEC")).isEqualTo(MethodType.EXEC);
		assertThat(MethodType.values()).hasSize(5);
	}

	@Test
	public void test_mqtt_exception() {
		final MqttException ex = new MqttException(404, "Not found");
		assertThat(ex.code).isEqualTo(404);
		assertThat(ex.getMessage()).isEqualTo("Not found");
	}

	@Test
	public void test_request_dto_fields() {
		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "app1";
		dto.method = MethodType.GET;
		dto.resource = "config/db";

		assertThat(dto.applicationId).isEqualTo("app1");
		assertThat(dto.method).isEqualTo(MethodType.GET);
		assertThat(dto.resource).isEqualTo("config/db");
	}

	@Test
	public void test_exception_to_string() {
		final Exception ex = new IllegalArgumentException("invalid argument");
		final String trace = exceptionToString(ex);

		assertThat(trace).contains("IllegalArgumentException");
		assertThat(trace).contains("invalid argument");
	}

	@Test
	public void test_client_id_from_config_admin() {
		final ConfigurationAdmin ca = (ConfigurationAdmin) Proxy.newProxyInstance(
				ConfigurationAdmin.class.getClassLoader(),
				new Class<?>[] { ConfigurationAdmin.class },
				(proxy, method, args) -> {
					if ("getConfiguration".equals(method.getName())) {
						return createConfigurationMock("client-1234");
					}
					return null;
				});

		final String id = clientID(ca, null);
		assertThat(id).isEqualTo("client-1234");
	}

	@Test
	public void test_client_id_from_bundle_context_property() {
		final ConfigurationAdmin ca = (ConfigurationAdmin) Proxy.newProxyInstance(
				ConfigurationAdmin.class.getClassLoader(),
				new Class<?>[] { ConfigurationAdmin.class },
				(proxy, method, args) -> {
					if ("getConfiguration".equals(method.getName())) {
						return createConfigurationMock(null);
					}
					return null;
				});

		final BundleContext bc = (BundleContext) Proxy.newProxyInstance(
				BundleContext.class.getClassLoader(),
				new Class<?>[] { BundleContext.class },
				(proxy, method, args) -> {
					if ("getProperty".equals(method.getName()) && CLIENT_ID_FRAMEWORK_PROPERTY.equals(args[0])) {
						return "fw-client-id";
					}
					return null;
				});

		final String id = clientID(ca, bc);
		assertThat(id).isEqualTo("fw-client-id");
	}

	@Test
	public void test_client_id_on_io_exception() {
		final ConfigurationAdmin ca = (ConfigurationAdmin) Proxy.newProxyInstance(
				ConfigurationAdmin.class.getClassLoader(),
				new Class<?>[] { ConfigurationAdmin.class },
				(proxy, method, args) -> {
					if ("getConfiguration".equals(method.getName())) {
						throw new IOException("storage error");
					}
					return null;
				});

		final String id = clientID(ca, null);
		assertThat(id).isEqualTo("+");
	}

	private Configuration createConfigurationMock(final String clientId) {
		return (Configuration) Proxy.newProxyInstance(
				Configuration.class.getClassLoader(),
				new Class<?>[] { Configuration.class },
				(proxy, method, args) -> {
					if ("getProperties".equals(method.getName())) {
						if (clientId == null) {
							return null;
						}
						final Dictionary<String, Object> props = new Hashtable<>();
						props.put("id", clientId);
						return props;
					}
					return null;
				});
	}

}
