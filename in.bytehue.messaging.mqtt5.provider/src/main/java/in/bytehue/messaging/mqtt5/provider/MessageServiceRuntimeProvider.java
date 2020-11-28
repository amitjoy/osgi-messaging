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
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.RETAIN;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.USER_PROPERTIES;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getDTOFromClass;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.AUTO_ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.GUARANTEED_DELIVERY;
import static org.osgi.service.messaging.Features.GUARANTEED_ORDERING;
import static org.osgi.service.messaging.Features.LAST_WILL;
import static org.osgi.service.messaging.Features.MESSAGE_CONTEXT_BUILDER;
import static org.osgi.service.messaging.Features.QOS;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;

// @formatter:off
@Component
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                QOS,
                RETAIN,
                REPLY_TO,
                LAST_WILL,
                ACKNOWLEDGE,
                RECEIVE_LOCAL,
                USER_PROPERTIES,
                AUTO_ACKNOWLEDGE,
                GUARANTEED_DELIVERY,
                GUARANTEED_ORDERING,
                REPLY_TO_MANY_PUBLISH,
                GENERATE_REPLY_CHANNEL,
                MESSAGE_EXPIRY_INTERVAL,
                REPLY_TO_MANY_SUBSCRIBE,
                GENERATE_CORRELATION_ID,
                MESSAGE_CONTEXT_BUILDER })
//@formatter:on
public final class MessageServiceRuntimeProvider implements MessageServiceRuntime {

    @Activate
    private BundleContext bundleContext;

    @Reference
    private MessageSubscriptionRegistry subscriptionRegistry;

    @Reference
    private ComponentServiceObjects<MessageClientProvider> messagingClient;

    @Override
    public MessagingRuntimeDTO getRuntimeDTO() {
        final MessageClientProvider client = messagingClient.getService();
        try {
            final MessagingRuntimeDTO dto = new MessagingRuntimeDTO();

            dto.connectionURI = client.client.getConfig().getServerHost();
            dto.serviceDTO = getDTOFromClass(MessageServiceRuntime.class, bundleContext);
            // @formatter:off
            dto.features =
                    new String[] {
                            ACKNOWLEDGE,
                            AUTO_ACKNOWLEDGE,
                            GENERATE_CORRELATION_ID,
                            GENERATE_REPLY_CHANNEL,
                            GUARANTEED_DELIVERY,
                            GUARANTEED_ORDERING,
                            LAST_WILL,
                            MESSAGE_CONTEXT_BUILDER,
                            QOS,
                            REPLY_TO,
                            REPLY_TO_MANY_PUBLISH,
                            REPLY_TO_MANY_SUBSCRIBE,
                            MESSAGE_EXPIRY_INTERVAL,
                            RECEIVE_LOCAL,
                            RETAIN,
                            USER_PROPERTIES };
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
