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

import static in.bytehue.messaging.mqtt5.remote.api.MqttApplication.APPLICATION_ID_PROPERTY;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_BAD_REQUEST;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_ERROR;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_NOT_FOUND;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_OK;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_PROPERTY;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_EXCEPTION_MESSAGE_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;

import in.bytehue.messaging.mqtt5.api.MqttMessageConstants;
import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.MethodType;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.MqttException;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.RequestDTO;
import in.bytehue.messaging.mqtt5.remote.api.MqttApplication;

public class RemoteResourceManagementTest {

	private RemoteResourceManagement rrm;
	private final List<Map.Entry<Map<String, Object>, MqttApplication>> applications = new ArrayList<>();

	@Before
	public void setup() throws Exception {
		rrm = new RemoteResourceManagement();

		// Configure ConfigurationAdmin mock with client id
		final ConfigurationAdmin ca = (ConfigurationAdmin) Proxy.newProxyInstance(
				ConfigurationAdmin.class.getClassLoader(),
				new Class<?>[] { ConfigurationAdmin.class },
				(proxy, method, args) -> {
					if ("getConfiguration".equals(method.getName())) {
						return (Configuration) Proxy.newProxyInstance(
								Configuration.class.getClassLoader(),
								new Class<?>[] { Configuration.class },
								(p, m, a) -> {
									if ("getProperties".equals(m.getName())) {
										final Dictionary<String, Object> props = new Hashtable<>();
										props.put("id", "device-123");
										return props;
									}
									return null;
								});
					}
					return null;
				});

		setField(rrm, "configurationAdmin", ca);
		setField(rrm, "applications", applications);
		setField(rrm, "mcbFactory", createMockMcbFactory());
	}

	@Test
	public void test_init_request_parsing_get() throws Exception {
		final String topic = "CTRL/in/bytehue/device-123/APP-V1/GET/configurations";
		final Message reqMessage = createMockMessage(topic, "req-cor-id", null);

		final Method initRequest = RemoteResourceManagement.class.getDeclaredMethod("initRequest", String.class, Message.class);
		initRequest.setAccessible(true);

		final RequestDTO dto = (RequestDTO) initRequest.invoke(rrm, topic, reqMessage);

		assertThat(dto.applicationId).isEqualTo("APP-V1");
		assertThat(dto.method).isEqualTo(MethodType.GET);
		assertThat(dto.resource).isEqualTo("configurations");
		assertThat(dto.requestMessage).isSameAs(reqMessage);
	}

	@Test
	public void test_init_request_parsing_post_with_nested_resource() throws Exception {
		final String topic = "CTRL/in/bytehue/device-123/TELEMETRY/POST/sensors/subsystem/cpu";
		final Message reqMessage = createMockMessage(topic, "req-cor-id", null);

		final Method initRequest = RemoteResourceManagement.class.getDeclaredMethod("initRequest", String.class, Message.class);
		initRequest.setAccessible(true);

		final RequestDTO dto = (RequestDTO) initRequest.invoke(rrm, topic, reqMessage);

		assertThat(dto.applicationId).isEqualTo("TELEMETRY");
		assertThat(dto.method).isEqualTo(MethodType.POST);
		assertThat(dto.resource).isEqualTo("sensors/subsystem/cpu");
	}

	@Test
	public void test_find_application() throws Exception {
		final MqttApplication app = new MqttApplication() {};
		final Map<String, Object> props = new HashMap<>();
		props.put(APPLICATION_ID_PROPERTY, "APP-SAMPLE");
		applications.add(new AbstractMap.SimpleEntry<>(props, app));

		final Method findApp = RemoteResourceManagement.class.getDeclaredMethod("findApp", String.class);
		findApp.setAccessible(true);

		final MqttApplication found = (MqttApplication) findApp.invoke(rrm, "APP-SAMPLE");
		assertThat(found).isSameAs(app);

		final MqttApplication notFound = (MqttApplication) findApp.invoke(rrm, "NON_EXISTENT");
		assertThat(notFound).isNull();
	}

	@Test
	public void test_dispatch_get_request() throws Exception {
		final AtomicBoolean getInvoked = new AtomicBoolean(false);
		final MqttApplication app = new MqttApplication() {
			@Override
			public Message doGET(final String resource, final Message requestMessage, final MqttMessageContextBuilder builder) {
				getInvoked.set(true);
				assertThat(resource).isEqualTo("status");
				return createMockMessage("resp-channel", null, new HashMap<>());
			}
		};

		final Map<String, Object> props = new HashMap<>();
		props.put(APPLICATION_ID_PROPERTY, "MY-APP");
		applications.add(new AbstractMap.SimpleEntry<>(props, app));

		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "MY-APP";
		dto.method = MethodType.GET;
		dto.resource = "status";
		dto.requestMessage = createMockMessage("req-channel", "cor-456", new HashMap<>());

		final Method execApp = RemoteResourceManagement.class.getDeclaredMethod("execMqttApplication", RequestDTO.class);
		execApp.setAccessible(true);

		final Message response = (Message) execApp.invoke(rrm, dto);

		assertThat(getInvoked.get()).isTrue();
		assertThat(response).isNotNull();

		final Map<String, Object> extensions = response.getContext().getExtensions();
		@SuppressWarnings("unchecked")
		final Map<String, Object> userProps = (Map<String, Object>) extensions.get(MqttMessageConstants.Extension.USER_PROPERTIES);
		assertThat(userProps.get(RESPONSE_CODE_PROPERTY)).isEqualTo(RESPONSE_CODE_OK);
	}

	@Test
	public void test_dispatch_post_request() throws Exception {
		final AtomicBoolean postInvoked = new AtomicBoolean(false);
		final MqttApplication app = new MqttApplication() {
			@Override
			public Message doPOST(final String resource, final Message requestMessage, final MqttMessageContextBuilder builder) {
				postInvoked.set(true);
				assertThat(resource).isEqualTo("data/update");
				return createMockMessage("resp-channel", null, new HashMap<>());
			}
		};

		final Map<String, Object> props = new HashMap<>();
		props.put(APPLICATION_ID_PROPERTY, "POST-APP");
		applications.add(new AbstractMap.SimpleEntry<>(props, app));

		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "POST-APP";
		dto.method = MethodType.POST;
		dto.resource = "data/update";
		dto.requestMessage = createMockMessage("req-channel", "cor-789", new HashMap<>());

		final Method execApp = RemoteResourceManagement.class.getDeclaredMethod("execMqttApplication", RequestDTO.class);
		execApp.setAccessible(true);

		final Message response = (Message) execApp.invoke(rrm, dto);

		assertThat(postInvoked.get()).isTrue();
		assertThat(response).isNotNull();

		final Map<String, Object> extensions = response.getContext().getExtensions();
		@SuppressWarnings("unchecked")
		final Map<String, Object> userProps = (Map<String, Object>) extensions.get(MqttMessageConstants.Extension.USER_PROPERTIES);
		assertThat(userProps.get(RESPONSE_CODE_PROPERTY)).isEqualTo(RESPONSE_CODE_OK);
	}

	@Test
	public void test_dispatch_put_request() throws Exception {
		final AtomicBoolean putInvoked = new AtomicBoolean(false);
		final MqttApplication app = new MqttApplication() {
			@Override
			public Message doPUT(final String resource, final Message requestMessage, final MqttMessageContextBuilder builder) {
				putInvoked.set(true);
				assertThat(resource).isEqualTo("config/new");
				return createMockMessage("resp-channel", null, new HashMap<>());
			}
		};

		final Map<String, Object> props = new HashMap<>();
		props.put(APPLICATION_ID_PROPERTY, "PUT-APP");
		applications.add(new AbstractMap.SimpleEntry<>(props, app));

		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "PUT-APP";
		dto.method = MethodType.PUT;
		dto.resource = "config/new";
		dto.requestMessage = createMockMessage("req-channel", "cor-put", new HashMap<>());

		final Method execApp = RemoteResourceManagement.class.getDeclaredMethod("execMqttApplication", RequestDTO.class);
		execApp.setAccessible(true);

		final Message response = (Message) execApp.invoke(rrm, dto);

		assertThat(putInvoked.get()).isTrue();
		assertThat(response).isNotNull();

		final Map<String, Object> extensions = response.getContext().getExtensions();
		@SuppressWarnings("unchecked")
		final Map<String, Object> userProps = (Map<String, Object>) extensions.get(MqttMessageConstants.Extension.USER_PROPERTIES);
		assertThat(userProps.get(RESPONSE_CODE_PROPERTY)).isEqualTo(RESPONSE_CODE_OK);
	}

	@Test
	public void test_dispatch_delete_request() throws Exception {
		final AtomicBoolean deleteInvoked = new AtomicBoolean(false);
		final MqttApplication app = new MqttApplication() {
			@Override
			public Message doDELETE(final String resource, final Message requestMessage, final MqttMessageContextBuilder builder) {
				deleteInvoked.set(true);
				assertThat(resource).isEqualTo("items/42");
				return createMockMessage("resp-channel", null, new HashMap<>());
			}
		};

		final Map<String, Object> props = new HashMap<>();
		props.put(APPLICATION_ID_PROPERTY, "DELETE-APP");
		applications.add(new AbstractMap.SimpleEntry<>(props, app));

		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "DELETE-APP";
		dto.method = MethodType.DELETE;
		dto.resource = "items/42";
		dto.requestMessage = createMockMessage("req-channel", "cor-del", new HashMap<>());

		final Method execApp = RemoteResourceManagement.class.getDeclaredMethod("execMqttApplication", RequestDTO.class);
		execApp.setAccessible(true);

		final Message response = (Message) execApp.invoke(rrm, dto);

		assertThat(deleteInvoked.get()).isTrue();
		assertThat(response).isNotNull();

		final Map<String, Object> extensions = response.getContext().getExtensions();
		@SuppressWarnings("unchecked")
		final Map<String, Object> userProps = (Map<String, Object>) extensions.get(MqttMessageConstants.Extension.USER_PROPERTIES);
		assertThat(userProps.get(RESPONSE_CODE_PROPERTY)).isEqualTo(RESPONSE_CODE_OK);
	}

	@Test
	public void test_dispatch_exec_request() throws Exception {
		final AtomicBoolean execInvoked = new AtomicBoolean(false);
		final MqttApplication app = new MqttApplication() {
			@Override
			public Message doEXEC(final String resource, final Message requestMessage, final MqttMessageContextBuilder builder) {
				execInvoked.set(true);
				assertThat(resource).isEqualTo("restart/node");
				return createMockMessage("resp-channel", null, new HashMap<>());
			}
		};

		final Map<String, Object> props = new HashMap<>();
		props.put(APPLICATION_ID_PROPERTY, "EXEC-APP");
		applications.add(new AbstractMap.SimpleEntry<>(props, app));

		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "EXEC-APP";
		dto.method = MethodType.EXEC;
		dto.resource = "restart/node";
		dto.requestMessage = createMockMessage("req-channel", "cor-exec", new HashMap<>());

		final Method execApp = RemoteResourceManagement.class.getDeclaredMethod("execMqttApplication", RequestDTO.class);
		execApp.setAccessible(true);

		final Message response = (Message) execApp.invoke(rrm, dto);

		assertThat(execInvoked.get()).isTrue();
		assertThat(response).isNotNull();

		final Map<String, Object> extensions = response.getContext().getExtensions();
		@SuppressWarnings("unchecked")
		final Map<String, Object> userProps = (Map<String, Object>) extensions.get(MqttMessageConstants.Extension.USER_PROPERTIES);
		assertThat(userProps.get(RESPONSE_CODE_PROPERTY)).isEqualTo(RESPONSE_CODE_OK);
	}

	@Test
	public void test_correlation_id_forwarded_to_response() throws Exception {
		final MqttApplication app = new MqttApplication() {
			@Override
			public Message doGET(final String resource, final Message requestMessage, final MqttMessageContextBuilder builder) {
				return createMockMessage("resp-channel", null, new HashMap<>());
			}
		};

		final Map<String, Object> props = new HashMap<>();
		props.put(APPLICATION_ID_PROPERTY, "CID-APP");
		applications.add(new AbstractMap.SimpleEntry<>(props, app));

		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "CID-APP";
		dto.method = MethodType.GET;
		dto.resource = "data";
		dto.requestMessage = createMockMessage("req-channel", "unique-correlation-id-999", new HashMap<>());

		final Method execApp = RemoteResourceManagement.class.getDeclaredMethod("execMqttApplication", RequestDTO.class);
		execApp.setAccessible(true);

		final Message response = (Message) execApp.invoke(rrm, dto);

		assertThat(response.getContext().getCorrelationId()).isEqualTo("unique-correlation-id-999");
	}

	@Test
	public void test_bad_request_when_less_than_three_tokens() throws Exception {
		final String topic = "CTRL/in/bytehue/device-123/APP1/GET";
		final Method initReq = RemoteResourceManagement.class.getDeclaredMethod("initRequest", String.class, Message.class);
		initReq.setAccessible(true);

		final Message reqMessage = createMockMessage(topic, "cid-bad", new HashMap<>());

		assertThatThrownBy(() -> initReq.invoke(rrm, topic, reqMessage))
				.isInstanceOf(InvocationTargetException.class)
				.hasCauseInstanceOf(MqttException.class)
				.satisfies(t -> {
					final MqttException ex = (MqttException) t.getCause();
					assertThat(ex.code).isEqualTo(RESPONSE_CODE_BAD_REQUEST);
					assertThat(ex.getMessage()).contains("APPLICATION-ID/METHOD/RESOURCE");
				});
	}

	@Test
	public void test_application_not_found() throws Exception {
		final RequestDTO dto = new RequestDTO();
		dto.applicationId = "NON_EXISTENT";
		dto.method = MethodType.GET;
		dto.resource = "res";
		dto.requestMessage = createMockMessage("req-channel", "cid", new HashMap<>());

		final Method execApp = RemoteResourceManagement.class.getDeclaredMethod("execMqttApplication", RequestDTO.class);
		execApp.setAccessible(true);

		assertThatThrownBy(() -> execApp.invoke(rrm, dto))
				.isInstanceOf(InvocationTargetException.class)
				.hasCauseInstanceOf(MqttException.class)
				.satisfies(t -> {
					final MqttException ex = (MqttException) t.getCause();
					assertThat(ex.code).isEqualTo(RESPONSE_CODE_NOT_FOUND);
					assertThat(ex.getMessage()).contains("doesn't exist");
				});
	}

	@Test
	public void test_prepare_error_message_500() throws Exception {
		final Method prepError = RemoteResourceManagement.class.getDeclaredMethod("prepareErrorMessage", Exception.class, int.class);
		prepError.setAccessible(true);

		final Exception error = new RuntimeException("Severe internal failure");
		final Message errorMsg = (Message) prepError.invoke(rrm, error, RESPONSE_CODE_ERROR);

		assertThat(errorMsg).isNotNull();
		final Map<String, Object> extensions = errorMsg.getContext().getExtensions();
		@SuppressWarnings("unchecked")
		final Map<String, Object> userProps = (Map<String, Object>) extensions.get(MqttMessageConstants.Extension.USER_PROPERTIES);
		assertThat(userProps.get(RESPONSE_CODE_PROPERTY)).isEqualTo(RESPONSE_CODE_ERROR);
		assertThat(userProps.get(RESPONSE_EXCEPTION_MESSAGE_PROPERTY)).isEqualTo("Severe internal failure");
	}

	@Test
	public void test_prepare_error_message_400() throws Exception {
		final Method prepError = RemoteResourceManagement.class.getDeclaredMethod("prepareErrorMessage", Exception.class, int.class);
		prepError.setAccessible(true);

		final MqttException error = new MqttException(RESPONSE_CODE_BAD_REQUEST, "Malformed command payload");
		final Message errorMsg = (Message) prepError.invoke(rrm, error, RESPONSE_CODE_BAD_REQUEST);

		assertThat(errorMsg).isNotNull();
		final Map<String, Object> extensions = errorMsg.getContext().getExtensions();
		@SuppressWarnings("unchecked")
		final Map<String, Object> userProps = (Map<String, Object>) extensions.get(MqttMessageConstants.Extension.USER_PROPERTIES);
		assertThat(userProps.get(RESPONSE_CODE_PROPERTY)).isEqualTo(RESPONSE_CODE_BAD_REQUEST);
		assertThat(userProps.get(RESPONSE_EXCEPTION_MESSAGE_PROPERTY)).isEqualTo("Malformed command payload");
	}

	private static void setField(final Object target, final String fieldName, final Object value) throws Exception {
		final Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Message createMockMessage(final String channel, final String correlationId, final Map<String, Object> extensions) {
		final Map<String, Object> ext = extensions != null ? extensions : new HashMap<>();
		final MessageContext context = (MessageContext) Proxy.newProxyInstance(
				MessageContext.class.getClassLoader(),
				new Class<?>[] { MessageContext.class },
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "getChannel":
							return channel;
						case "getCorrelationId":
							return correlationId;
						case "getReplyToChannel":
							return "reply-to";
						case "getExtensions":
							return ext;
						default:
							return null;
					}
				});

		return (Message) Proxy.newProxyInstance(
				Message.class.getClassLoader(),
				new Class<?>[] { Message.class },
				(proxy, method, args) -> {
					if ("getContext".equals(method.getName())) {
						return context;
					}
					if ("payload".equals(method.getName())) {
						return ByteBuffer.wrap(new byte[0]);
					}
					return null;
				});
	}

	@SuppressWarnings("unchecked")
	private static ComponentServiceObjects<MqttMessageContextBuilder> createMockMcbFactory() {
		return (ComponentServiceObjects<MqttMessageContextBuilder>) Proxy.newProxyInstance(
				ComponentServiceObjects.class.getClassLoader(),
				new Class<?>[] { ComponentServiceObjects.class },
				(proxy, method, args) -> {
					if ("getService".equals(method.getName())) {
						return createMockMcb();
					}
					return null;
				});
	}

	private static MqttMessageContextBuilder createMockMcb() {
		final Map<String, Object> ext = new HashMap<>();
		final Map<String, Object> userProps = new HashMap<>();
		ext.put(MqttMessageConstants.Extension.USER_PROPERTIES, userProps);
		final String[] cidHolder = new String[] { "built-cid" };

		return (MqttMessageContextBuilder) Proxy.newProxyInstance(
				MqttMessageContextBuilder.class.getClassLoader(),
				new Class<?>[] { MqttMessageContextBuilder.class },
				(proxy, method, args) -> {
					final String name = method.getName();
					if ("buildMessage".equals(name)) {
						return createMockMessage("built-channel", cidHolder[0], new HashMap<>(ext));
					}
					if ("correlationId".equals(name) && args != null && args.length == 1) {
						cidHolder[0] = (String) args[0];
						return proxy;
					}
					if ("extensions".equals(name) && args != null && args.length == 1 && args[0] instanceof Map) {
						@SuppressWarnings("unchecked")
						final Map<String, Object> passedExt = (Map<String, Object>) args[0];
						ext.putAll(passedExt);
						return proxy;
					}
					if ("extensionEntry".equals(name) && args != null && args.length == 2) {
						ext.put((String) args[0], args[1]);
						return proxy;
					}
					if (method.getReturnType().isAssignableFrom(MqttMessageContextBuilder.class)) {
						return proxy;
					}
					return null;
				});
	}

}
