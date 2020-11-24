package com.byteurn.messaging.mqtt5.provider;

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;
import static org.osgi.service.messaging.Features.QOS;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.ACKOWLEDGED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.REJECTED;

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

import com.byteurn.messaging.mqtt5.provider.helper.MessagingHelper;
import com.hivemq.client.mqtt.datatypes.MqttQos;

@Component
@MessagingFeature(name = "message-subscriber", feature = "subscribe", protocol = "mqtt5")
public final class SimpleMessageSubscriber implements MessageSubscription {

    @Activate
    private BundleContext bundleContext;

    @Reference
    private MessagingClient messagingClient;

    @Reference(scope = PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<MessageContextBuilder> messageContextBuilder;

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
                                      final MessageContextBuilder mcb = messageContextBuilder.getService();
                                      final Message message = MessagingHelper.toMessage(p, mcb);
                                      final SimpleMessageContext ctx = (SimpleMessageContext) context;
                                      boolean isAcknowledged = false;
                                      if (ctx.acknowledgeFilter != null) {
                                          isAcknowledged = ctx.acknowledgeFilter.test(message);
                                          if (isAcknowledged) {
                                              ctx.acknowledgeState = ACKOWLEDGED;
                                              if (ctx.acknowledgeHandler != null) {
                                                  ctx.acknowledgeHandler.accept(message);
                                              }
                                              source.publish(message);
                                          } else {
                                              ctx.acknowledgeState = REJECTED;
                                          }
                                      }
                                      if (ctx.acknowledgeConsumer != null) {
                                          ctx.acknowledgeConsumer.accept(message);
                                      }
                                  } finally {
                                      bundleContext.ungetService(messageContextBuilder.getServiceReference());
                                  }
                              })
                              .send();
        // @formatter:on
        return provider.createStream(source);
    }

}
