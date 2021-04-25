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
package in.bytehue.messaging.mqtt5.remote.adapter;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.clientID;
import static in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.exceptionToString;
import static in.bytehue.messaging.mqtt5.remote.api.MqttApplication.APPLICATION_ID_PROPERTY;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.REMOTE_RESOURCE_MANAGEMENT_PID;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_BAD_REQUEST;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_ERROR;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_NOT_FOUND;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_OK;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_CODE_PROPERTY;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.RESPONSE_EXCEPTION_MESSAGE_PROPERTY;
import static java.util.stream.Collectors.joining;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_NAME_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.MethodType;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.MqttException;
import in.bytehue.messaging.mqtt5.remote.adapter.RemoteResourceHelper.RequestDTO;
import in.bytehue.messaging.mqtt5.remote.annotations.ProvideMQTTRemoteResourceManagement;
import in.bytehue.messaging.mqtt5.remote.api.MqttApplication;

@ProvideMQTTRemoteResourceManagement
@Component(configurationPid = REMOTE_RESOURCE_MANAGEMENT_PID)
public final class RemoteResourceManagement {

    //@formatter:off
    private static final String FILTER =
            "(&"
                + "(" + MESSAGING_PROTOCOL_PROPERTY + "=" + MESSAGING_PROTOCOL + ")"
                + "(" + MESSAGING_NAME_PROPERTY + "=" + MESSAGING_ID + "))";

    @ObjectClassDefinition(
            name = "MQTT Remote Resource Management",
            description = "This configuration is used to configure the remote resource management")
    @interface Config {
        @AttributeDefinition(name = "The control topic prefix for the remote resource management")
        String controlTopicPrefix() default "CTRL";

        @AttributeDefinition(name = "The control topic for the remote resource management")
        String controlTopic() default "in/bytehue";
    }
    //@formatter:on

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference(target = FILTER)
    private MessagePublisher publisher;

    @Reference(target = FILTER)
    private MessageSubscription subscriber;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @Reference(target = FILTER)
    private ComponentServiceObjects<MqttMessageContextBuilder> mcbFactory;

    @Reference
    private volatile Collection<Entry<Map<String, Object>, MqttApplication>> applications; // ascending order of ranking

    @Activate
    private Config config;

    @Activate
    private BundleContext bundleContext;

    @Activate
    void init() {
        final String topic = prepareSubscriptionTopic();
        subscribe(topic);
    }

    /**
     * The topic to be subscribed for remote resource management
     * <p>
     * {@code control-topic/client-id/REMOTE/application-id/method/resource-id}
     * <p>
     * For example:
     * <ul>
     * <li>{@code CTRL/com/company/project/DEVICE-ID-1234/CONF-V1/GET/configurations}</li>
     * <li>{@code CTRL/com/company/project/DEVICE-ID-1234/CONF-V1/GET/configurations/a.b.c}</li>
     * <li>{@code CTRL/com/company/project/DEVICE-ID-1234/DEPLOY-V2/EXEC/start/bundleA}</li>
     * <li>{@code CTRL/com/company/project/DEVICE-ID-4567/CONF-V3/GET/bundles}</li>
     * <li>{@code CTRL/com/company/project/DEVICE-ID-4567/APP-V1/EXEC/command}</li>
     * </ul>
     *
     * In the aforementioned examples,
     * <ul>
     * <li>{@code CTRL}</li> - Control Topic Prefix
     * <li>{@code com/company/project}</li> - Control Topic
     * <li>{@code DEVICE-ID-1234}</li> - Client ID
     * <li>{@code DEVICE-ID-4567}</li> - Client ID
     * <li>{@code CONF-V1}</li> - Application ID
     * <li>{@code DEPLOY-V2}</li> - Application ID
     * <li>{@code CONF-V3}</li> - Application ID
     * <li>{@code APP-V1}</li> - Application ID
     * <li>{@code GET}</li> - Method. Refer to {@link MethodType}
     * <li>{@code EXEC}</li> - Method. Refer to {@link MethodType}
     * <li>{@code configurations}</li> - Resource
     * <li>{@code configurations/a.b.c}</li> - Resource
     * <li>{@code start/bundleA}</li> - Resource
     * <li>{@code bundles}</li> - Resource
     * <li>{@code command}</li> - Resource
     * </ul>
     *
     * Therefore, we subscribe to {@code CTRL/com/company/project/DEVICE-ID-1234/#}
     * <p>
     * The subscription pattern: {@code control-topic-prefix/control-topic/client-id/#}
     *
     * @return the topic pattern to be subscribed
     */
    // @formatter:off
    private String prepareSubscriptionTopic() {
        return config.controlTopicPrefix()                  + "/" +
               config.controlTopic()                        + "/" +
               clientID(configurationAdmin, bundleContext)  +
               "/#";
    }
    // @formatter:on

    private void subscribe(final String topic) {
        subscriber.subscribe(topic).forEach(reqMessage -> {
            Message response;
            try {
                final RequestDTO request = prepareReqeust(reqMessage);
                response = execMqttApplication(request);
            } catch (final MqttException e) {
                final int code = e.code;
                response = prepareErrorMessage(e, code);
            } catch (final Exception e) {
                response = prepareErrorMessage(e, RESPONSE_CODE_ERROR);
            }
            final String replyToChannel = reqMessage.getContext().getReplyToChannel();
            if (replyToChannel == null) {
                logger.warn("The control topic {} request doesn't contain any reply to channel", topic);
                return;
            }
            publisher.publish(response, replyToChannel);
        }).onFailure(e -> logger.error("Error occurred while processing the request", topic, e));
    }

    private RequestDTO prepareReqeust(final Message requestMessage) {
        final String topic = requestMessage.getContext().getChannel();
        return initRequest(topic, requestMessage);
    }

    private RequestDTO initRequest(final String topic, final Message requestMessage) {
        final String clientID = clientID(configurationAdmin, bundleContext);
        final String subString = topic.substring(topic.indexOf(clientID) + clientID.length() + 1);
        final List<String> requestTokens = Arrays.asList(subString.split("/"));

        // the token should have at least following 3 elements:
        // APPLICATION-ID, METHOD and RESOURCE
        if (requestTokens.size() < 3) {
            throw new MqttException(RESPONSE_CODE_BAD_REQUEST,
                    "The request doesn't contain the following elements in order: APPLICATION-ID/METHOD/RESOURCE");
        }
        final RequestDTO dto = new RequestDTO();

        dto.applicationId = requestTokens.get(0);
        dto.method = MethodType.valueOf(requestTokens.get(1));
        dto.resource = requestTokens.subList(2, requestTokens.size()).stream().collect(joining("/"));
        dto.requestMessage = requestMessage;

        return dto;
    }

    private Message prepareErrorMessage(final Exception exception, final int code) {
        final MqttMessageContextBuilder mcb = mcbFactory.getService();
        try {
            final String exMessage = exception.getMessage();
            final String ex = exMessage == null ? exceptionToString(exception) : exMessage;

            final Map<String, Object> properties = new HashMap<>();

            properties.put(RESPONSE_CODE_PROPERTY, code);
            properties.put(RESPONSE_EXCEPTION_MESSAGE_PROPERTY, ex);
            return mcb.extensionEntry(USER_PROPERTIES, properties).buildMessage();
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }

    private Message execMqttApplication(final RequestDTO request) throws Exception {
        final MqttApplication application = findApp(request.applicationId);
        if (application == null) {
            throw new MqttException(RESPONSE_CODE_NOT_FOUND,
                    "MQTT Application " + request.applicationId + " doesn't exist");
        }
        Message message;
        final MqttMessageContextBuilder mcb = mcbFactory.getService();
        try {
            final Message requestMessage = request.requestMessage;
            final String resource = request.resource;
            final String correlationId = requestMessage.getContext().getCorrelationId();

            switch (request.method) {
                case GET:
                    message = application.doGET(resource, requestMessage, mcb);
                    return addResponseCodeAndCorrelationId(message, correlationId);
                case POST:
                    message = application.doPOST(resource, requestMessage, mcb);
                    return addResponseCodeAndCorrelationId(message, correlationId);
                case PUT:
                    message = application.doPUT(resource, requestMessage, mcb);
                    return addResponseCodeAndCorrelationId(message, correlationId);
                case DELETE:
                    message = application.doDELETE(resource, requestMessage, mcb);
                    return addResponseCodeAndCorrelationId(message, correlationId);
                case EXEC:
                    message = application.doEXEC(resource, requestMessage, mcb);
                    return addResponseCodeAndCorrelationId(message, correlationId);
                default:
                    throw new MqttException(RESPONSE_CODE_BAD_REQUEST,
                            "Unable to execute the specified method - " + request.method);
            }
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }

    private Message addResponseCodeAndCorrelationId(final Message message, final String correlationId) {
        final Message updatedMessage = addResponseCode(message);
        return addCorrelationId(updatedMessage, correlationId);
    }

    private Message addResponseCode(final Message message) {
        final Map<String, Object> extensions = message.getContext().getExtensions();
        final Object userProperties = extensions.computeIfAbsent(USER_PROPERTIES, e -> new HashMap<>());
        if (!(userProperties instanceof Map<?, ?>)) {
            throw new MqttException(RESPONSE_CODE_ERROR, "User Properties should be an instance of map");
        }
        @SuppressWarnings("unchecked")
        final Map<String, Object> properties = (Map<String, Object>) userProperties;
        properties.put(RESPONSE_CODE_PROPERTY, RESPONSE_CODE_OK);

        return message;
    }

    private Message addCorrelationId(final Message message, final String correlationId) {
        final MqttMessageContextBuilder mcb = mcbFactory.getService();
        try {
            final MessageContext context = message.getContext();
            // @formatter:off
            return mcb.channel(context.getChannel())
                      .content(message.payload())
                      .contentEncoding(context.getContentEncoding())
                      .contentType(context.getContentType())
                      .correlationId(correlationId)
                      .extensions(context.getExtensions())
                      .buildMessage();
            // @formatter:on
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }

    private MqttApplication findApp(final String applicationId) {
        for (final Entry<Map<String, Object>, MqttApplication> tuple : applications) {
            final Object appId = tuple.getKey().get(APPLICATION_ID_PROPERTY);
            if (appId.equals(applicationId)) {
                return tuple.getValue();
            }
        }
        return null;
    }

}
