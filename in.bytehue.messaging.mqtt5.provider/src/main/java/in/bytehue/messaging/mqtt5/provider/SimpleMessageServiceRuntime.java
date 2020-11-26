package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MESSAGE_RUNTIME_NAME;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.PROVIDER_NAME;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.AUTO_ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
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

import in.bytehue.messaging.mqtt5.provider.helper.MessageHelper;

@Component
@MessagingFeature(name = MESSAGE_RUNTIME_NAME, protocol = MQTT_PROTOCOL)
public final class SimpleMessageServiceRuntime implements MessageServiceRuntime {

    @Activate
    private BundleContext bundleContext;

    @Reference
    private SimpleMessagePublisher publisher;

    @Reference
    private SimpleMessageSubscriber subscriber;

    @Reference
    private SimpleMessageReplyToWhiteboard whiteboard;

    @Reference
    private ComponentServiceObjects<SimpleMessageClient> messagingClient;

    @Override
    public MessagingRuntimeDTO getRuntimeDTO() {
        final SimpleMessageClient client = messagingClient.getService();
        try {
            final MessagingRuntimeDTO dto = new MessagingRuntimeDTO();

            dto.connectionURI = client.client.getConfig().getServerHost();
            dto.serviceDTO = MessageHelper.findServiceRefAsDTO(MessageServiceRuntime.class, bundleContext);

            // @formatter:off
            dto.features = new String[] {
                    ACKNOWLEDGE,
                    AUTO_ACKNOWLEDGE,
                    GENERATE_CORRELATION_ID,
                    GENERATE_REPLY_CHANNEL,
                    LAST_WILL,
                    MESSAGE_CONTEXT_BUILDER,
                    QOS,
                    REPLY_TO,
                    REPLY_TO_MANY_PUBLISH,
                    REPLY_TO_MANY_SUBSCRIBE};
            // @formatter:on

            dto.instanceId = messagingClient.getServiceReference().getProperties().get(SERVICE_ID).toString();
            dto.protocols = new String[] { MQTT_PROTOCOL };
            dto.providerName = PROVIDER_NAME;
            dto.subscriptions = subscriber.getSubscriptionDTOs();
            dto.replyToSubscriptions = whiteboard.getReplyToSubscriptionDTOs();

            return dto;
        } finally {
            messagingClient.ungetService(client);
        }
    }

}