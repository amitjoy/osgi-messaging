package in.bytehue.messaging.mqtt5.provider;

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static in.bytehue.messaging.mqtt5.provider.helper.MessagingHelper.acknowledgeMessage;
import static in.bytehue.messaging.mqtt5.provider.helper.MessagingHelper.toMessage;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;
import static org.osgi.service.messaging.Features.QOS;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import com.hivemq.client.mqtt.datatypes.MqttQos;

@Component
@MessagingFeature(name = "message-subscriber", feature = "subscribe", protocol = "mqtt5")
public final class SimpleMessageSubscriber implements MessageSubscription {

    @Activate
    private BundleContext bundleContext;

    @Reference
    private MessagingClient messagingClient;

    @Reference(scope = PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

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
        if (context != null) {
            channel = context.getChannel();
        }
        requireNonNull(channel, "Channel cannot be null");
        final Map<String, Object> extensions = context.getExtensions();
        final int qos = (int) extensions.getOrDefault(QOS, DEFAULT_QOS);
        // @formatter:off
        messagingClient.client.toAsync()
                              .subscribeWith()
                              .topicFilter(channel)
                              .qos(MqttQos.fromCode(qos))
                              .callback(p -> {
                                  try {
                                      final MessageContextBuilder mcb = mcbFactory.getService();
                                      final Message message = toMessage(p, mcb);
                                      final SimpleMessageContext ctx = (SimpleMessageContext) context;
                                      acknowledgeMessage(message, ctx, m -> source.publish(m));
                                  } finally {
                                      bundleContext.ungetService(mcbFactory.getServiceReference());
                                  }
                              })
                              .send();
        // @formatter:on
        return provider.createStream(source);
    }

}
