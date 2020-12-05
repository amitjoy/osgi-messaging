/*******************************************************************************
 * Copyright 2020 Amit Kumar Mondal
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
package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROVIDER;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RECEIVE_LOCAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.REPLY_TO_MANY_PREDICATE;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.REPLY_TO_MANY_PREDICATE_FILTER;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.toServiceReferenceDTO;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.EXTENSION_AUTO_ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.EXTENSION_GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.EXTENSION_LAST_WILL;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.MESSAGE_CONTEXT_BUILDER;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;

import in.bytehue.messaging.mqtt5.provider.helper.GogoCommand;

// @formatter:off
@Component
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                RETAIN,
                REPLY_TO,
                ACKNOWLEDGE,
                EXTENSION_QOS,
                RECEIVE_LOCAL,
                USER_PROPERTIES,
                EXTENSION_LAST_WILL,
                REPLY_TO_MANY_PUBLISH,
                GENERATE_REPLY_CHANNEL,
                MESSAGE_EXPIRY_INTERVAL,
                MESSAGE_CONTEXT_BUILDER,
                REPLY_TO_MANY_SUBSCRIBE,
                GENERATE_CORRELATION_ID,
                EXTENSION_AUTO_ACKNOWLEDGE,
                EXTENSION_GUARANTEED_DELIVERY,
                EXTENSION_GUARANTEED_ORDERING })
@GogoCommand(scope = "mqtt", function = "runtime")
//@formatter:on
public final class MessageServiceRuntimeProvider implements MessageServiceRuntime {

    @Activate
    private BundleContext bundleContext;

    @Activate
    private ComponentContext componentContext;

    @Reference
    private MessageSubscriptionRegistry subscriptionRegistry;

    @Reference
    private ComponentServiceObjects<MessageClientProvider> messagingClient;

    public MessagingRuntimeDTO runtime() {
        return getRuntimeDTO();
    }

    @Override
    public MessagingRuntimeDTO getRuntimeDTO() {
        final MessageClientProvider client = messagingClient.getService();
        try {
            final MessagingRuntimeDTO dto = new MessagingRuntimeDTO();

            dto.connectionURI = client.client.getConfig().getServerHost();
            dto.serviceDTO = toServiceReferenceDTO(componentContext.getServiceReference());
            // @formatter:off
            dto.features =
                    new String[] {
                            RETAIN,
                            REPLY_TO,
                            ACKNOWLEDGE,
                            RECEIVE_LOCAL,
                            EXTENSION_QOS,
                            USER_PROPERTIES,
                            EXTENSION_LAST_WILL,
                            REPLY_TO_MANY_PUBLISH,
                            GENERATE_REPLY_CHANNEL,
                            MESSAGE_EXPIRY_INTERVAL,
                            GENERATE_CORRELATION_ID,
                            REPLY_TO_MANY_SUBSCRIBE,
                            REPLY_TO_MANY_PREDICATE,
                            MESSAGE_CONTEXT_BUILDER,
                            EXTENSION_AUTO_ACKNOWLEDGE,
                            EXTENSION_GUARANTEED_ORDERING,
                            EXTENSION_GUARANTEED_DELIVERY,
                            REPLY_TO_MANY_PREDICATE_FILTER };
            // @formatter:on
            dto.instanceId = messagingClient.getServiceReference().getProperties().get(SERVICE_ID).toString();
            dto.protocols = new String[] { MESSAGING_PROTOCOL };
            dto.providerName = MESSAGING_PROVIDER;
            dto.subscriptions = subscriptionRegistry.getSubscriptionDTOs();
            dto.replyToSubscriptions = subscriptionRegistry.getReplyToSubscriptionDTOs();

            return dto;
        } finally {
            messagingClient.ungetService(client);
        }
    }

}
