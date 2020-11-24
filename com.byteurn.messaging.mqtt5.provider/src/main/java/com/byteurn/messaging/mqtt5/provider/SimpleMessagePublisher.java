package com.byteurn.messaging.mqtt5.provider;

import static com.byteurn.messaging.mqtt5.provider.SimpleMessagePublisher.PID;
import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static java.util.Collections.emptyMap;
import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;
import static org.osgi.service.messaging.Features.MESSAGE_EXPIRY_INTERVAL;
import static org.osgi.service.messaging.Features.QOS;
import static org.osgi.service.messaging.Features.RETAIN;
import static org.osgi.service.messaging.Features.USER_PROPERTIES;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.annotations.ProvideMessagingReplyToFeature;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import com.byteurn.messaging.mqtt5.provider.SimpleMessagePublisher.Config;
import com.byteurn.messaging.mqtt5.provider.helper.MessagingHelper;
import com.byteurn.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder.Send.Complete;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;

@Designate(ocd = Config.class)
@ProvideMessagingReplyToFeature
@Component(configurationPid = PID)
@MessagingFeature(name = "message-publisher", feature = "publish", protocol = "mqtt5")
public final class SimpleMessagePublisher implements MessagePublisher, ReplyToPublisher {

    private static final String THREAD_NAME_FORMAT = "-%d";
    private static final String THREAD_FACTORY_NAME = "message-publisher";

    public static final String PID = "com.byteurn.messaging.publisher";

    // @formatter:off
    private static final String FILTER_SUB_HANDLER = ""
            + "(osgi.messaging.replyToSubscription.target="
            + "(&(osgi.messaging.name=message-publisher)"
            + "(osgi.messaging.protocol=mqtt5)"
            + "(osgi.messaging.feature=replyTo)))";
    // @formatter:on

    private final PromiseFactory factory;
    private final BundleContext bundleContext;

    @ObjectClassDefinition( //
            name = "MQTT Messaging Publisher Executor Configuration", //
            description = "This configuration is used to configure the internal thread pool size")
    @interface Config {
        @AttributeDefinition(name = "Number of Threads for internal Thread Pool")
        int numThreads() default 10;
    }

    @Reference
    private MessagingClient messagingClient;

    @Reference(scope = PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<MessageContextBuilder> messageContextBuilder;

    @Reference(target = FILTER_SUB_HANDLER)
    private volatile Collection<ServiceReference<ReplyToSubscriptionHandler>> simpleHandlers;

    @Reference(target = FILTER_SUB_HANDLER)
    private volatile Collection<ServiceReference<ReplyToSingleSubscriptionHandler>> singleSubHandlers;

    @Activate
    public SimpleMessagePublisher(final BundleContext bundleContext, final Config config) {
        this.bundleContext = bundleContext;
        final ThreadFactory threadFactory = //
                new ThreadFactoryBuilder() //
                        .setThreadFactoryName(THREAD_FACTORY_NAME) //
                        .setThreadNameFormat(THREAD_NAME_FORMAT) //
                        .build();
        factory = new PromiseFactory(Executors.newFixedThreadPool(config.numThreads(), threadFactory));
    }

    @Override
    public void publish(final Message message) {
        publish(message, null, null, false);
    }

    @Override
    public void publish(final Message message, final String channel) {
        publish(message, null, channel, false);
    }

    @Override
    public void publish(final Message message, final MessageContext context) {
        publish(message, context, null, false);
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage) {
        return publish(requestMessage, null, null, true);
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage, final MessageContext replyToContext) {
        return publish(requestMessage, replyToContext, null, true);
    }

    private Promise<Message> publish(final Message message, MessageContext context, String channel,
            final boolean isReplyTo) {
        if (context == null) {
            context = message.getContext();
        }
        if (channel == null) {
            channel = context.getChannel();
        }
        final Deferred<Message> deferred = factory.deferred();
        final Map<String, Object> extensions = context.getExtensions();
        final String contentType = context.getContentType();
        final String correlationId = context.getCorrelationId();
        final ByteBuffer content = message.payload();
        final long messageExpiryInterval = (long) extensions.getOrDefault(MESSAGE_EXPIRY_INTERVAL, Long.MAX_VALUE);
        final int qos = (int) extensions.getOrDefault(QOS, DEFAULT_QOS.getCode());
        final boolean retain = (boolean) extensions.getOrDefault(RETAIN, false);
        @SuppressWarnings("unchecked")
        final Map<String, String> userProperties = (Map<String, String>) extensions.getOrDefault(USER_PROPERTIES,
                emptyMap());

        final Mqtt5UserPropertiesBuilder propsBuilder = Mqtt5UserProperties.builder();
        userProperties.forEach((k, v) -> propsBuilder.add(k, v));

        // @formatter:off
        final Complete<CompletableFuture<Mqtt5PublishResult>> publishRequest =
            messagingClient.client.toAsync()
                                  .publishWith()
                                  .topic(channel)
                                  .contentType(contentType)
                                  .messageExpiryInterval(messageExpiryInterval)
                                  .payload(content)
                                  .qos(MqttQos.fromCode(qos))
                                  .retain(retain)
                                  .userProperties(propsBuilder.build());
        if (isReplyTo) {
            final String replyToChannel = context.getReplyToChannel();
            publishRequest.responseTopic(replyToChannel).correlationData(correlationId.getBytes());
            messagingClient.client.toAsync()
                                  .subscribeWith()
                                  .topicFilter(replyToChannel)
                                  .callback(m -> {
                                      try {
                                          final MessageContextBuilder mcb = messageContextBuilder.getService();
                                          final Message msg = MessagingHelper.toMessage(m, mcb);
                                          deferred.resolve(msg);
                                          simpleHandlers.stream()
                                                        .filter(h -> filterHandler(h, replyToChannel))
                                                        .sorted(Comparator.reverseOrder())
                                                        .map(s -> bundleContext.getService(s))
                                                        .forEach(h -> h.handleResponse(msg));
                                          singleSubHandlers.stream()
                                                           .filter(h -> filterHandler(h, replyToChannel))
                                                           .sorted(Comparator.reverseOrder())
                                                           .map(s -> bundleContext.getService(s))
                                                           .forEach(h -> h.handleResponse(msg, mcb));
                                      } finally {
                                          bundleContext.ungetService(messageContextBuilder.getServiceReference());
                                      }
                                  })
                                  .send()
                                  .thenCompose(subAck -> publishRequest.send());
        } else {
            publishRequest.send();
        }
        // @formatter:on
        return deferred.getPromise();
    }

    private static boolean filterHandler(final ServiceReference<?> ref, final String channel) {
        final String property = (String) ref.getProperty("osgi.messaging.replyToSubscription.channel");
        if (property == null) {
            return false;
        }
        return true;
    }

}