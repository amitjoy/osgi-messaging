package in.bytehue.messaging.mqtt5.provider;

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MESSAGE_SUBSCRIBER_NAME;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.acknowledgeMessage;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.findServiceRefAsDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.initChannelDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.toMessage;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;
import static org.osgi.service.messaging.Features.QOS;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import com.hivemq.client.mqtt.datatypes.MqttQos;

@MessagingFeature(name = MESSAGE_SUBSCRIBER_NAME, protocol = MQTT_PROTOCOL)
@Component(service = { MessageSubscription.class, SimpleMessageSubscriber.class })
public final class SimpleMessageSubscriber implements MessageSubscription {

    @Activate
    private BundleContext bundleContext;

    @Reference
    private SimpleMessageClient messagingClient;

    @Reference(scope = PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<SimpleMessageContextBuilder> mcbFactory;

    private final Map<PushStream<Message>, ChannelDTO> subscriptions = new HashMap<>();

    @Override
    public PushStream<Message> subscribe(final String channel) {
        return subscribe(null, channel);
    }

    @Override
    public PushStream<Message> subscribe(final MessageContext context) {
        return subscribe(context, null);
    }

    private PushStream<Message> subscribe(final MessageContext context, String channel) {
        final PushStreamProvider provider = new PushStreamProvider();
        final SimplePushEventSource<Message> source = provider.createSimpleEventSource(Message.class);
        final PushStream<Message> stream = provider.createStream(source);

        if (context != null) {
            channel = context.getChannel();
        }
        requireNonNull(channel, "Channel cannot be null");
        final Map<String, Object> extensions = context.getExtensions();
        final int qos = (int) extensions.getOrDefault(QOS, DEFAULT_QOS);

        subscriptions.put(stream, initChannelDTO(channel, null, true));

        // @formatter:off
        messagingClient.client.toAsync()
                              .subscribeWith()
                              .topicFilter(channel)
                              .qos(MqttQos.fromCode(qos))
                              .callback(p -> {
                                  final SimpleMessageContextBuilder mcb = mcbFactory.getService();
                                  try {
                                      final Message message = toMessage(p, mcb);
                                      final SimpleMessageContext ctx = (SimpleMessageContext) context;
                                      acknowledgeMessage(message, ctx, m -> source.publish(m));
                                  } finally {
                                      mcbFactory.ungetService(mcb);
                                  }
                              })
                              .send();
        // @formatter:on

        stream.onClose(() -> {
            final ChannelDTO dto = subscriptions.get(stream);
            dto.connected = false;
        });
        return stream;
    }

    public SubscriptionDTO[] getSubscriptionDTOs() {
        // @formatter:off
        return subscriptions.values()
                            .stream()
                            .map(this::initSubscriptionDTO)
                            .toArray(SubscriptionDTO[]::new);
        // @formatter:on
    }

    private SubscriptionDTO initSubscriptionDTO(final ChannelDTO channelDTO) {
        final SubscriptionDTO subscriptionDTO = new SubscriptionDTO();

        subscriptionDTO.serviceDTO = findServiceRefAsDTO(MessageSubscription.class, bundleContext);
        subscriptionDTO.channel = channelDTO;

        return subscriptionDTO;
    }
}
