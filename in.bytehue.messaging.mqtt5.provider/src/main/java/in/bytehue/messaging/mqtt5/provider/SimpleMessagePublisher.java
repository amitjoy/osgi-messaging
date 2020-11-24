package in.bytehue.messaging.mqtt5.provider;

import static com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish.DEFAULT_QOS;
import static in.bytehue.messaging.mqtt5.provider.SimpleMessagePublisher.PID;
import static in.bytehue.messaging.mqtt5.provider.SimpleMessagePublisher.CommunicationType.NO_REPLY_TO;
import static in.bytehue.messaging.mqtt5.provider.SimpleMessagePublisher.CommunicationType.REPLY_TO_MANY;
import static in.bytehue.messaging.mqtt5.provider.SimpleMessagePublisher.CommunicationType.REPLY_TO_SINGLE;
import static in.bytehue.messaging.mqtt5.provider.helper.MessagingHelper.acknowledgeMessage;
import static in.bytehue.messaging.mqtt5.provider.helper.MessagingHelper.toMessage;
import static java.lang.Long.MAX_VALUE;
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
import java.util.UUID;
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
import org.osgi.service.messaging.annotations.ProvideMessagingReplyToManySubscribeFeature;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder.Send.Complete;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;

import in.bytehue.messaging.mqtt5.provider.SimpleMessagePublisher.Config;
import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

@Designate(ocd = Config.class)
@ProvideMessagingReplyToFeature
@ProvideMessagingReplyToManySubscribeFeature
@Component(configurationPid = PID)
@MessagingFeature( //
        name = "message-publisher", //
        protocol = "mqtt5", //
        feature = { //
                "publish", //
                "generateCorrelationId", //
                "generateReplyChannel" })
public final class SimpleMessagePublisher implements MessagePublisher, ReplyToPublisher, ReplyToManyPublisher {

    private static final String THREAD_NAME_FORMAT = "-%d";
    private static final String THREAD_FACTORY_NAME = "message-publisher";

    public static final String PID = "in.bytehue.messaging.publisher";

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
        @AttributeDefinition(name = "Number of Threads for the internal thread pool")
        int numThreads() default 10;
    }

    @Reference
    private MessagingClient messagingClient;

    @Reference(scope = PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    @Reference(target = FILTER_SUB_HANDLER)
    private volatile Collection<ServiceReference<ReplyToSubscriptionHandler>> simpleHandlers;

    @Reference(target = FILTER_SUB_HANDLER)
    private volatile Collection<ServiceReference<ReplyToSingleSubscriptionHandler>> singleSubHandlers;

    @Reference(target = FILTER_SUB_HANDLER)
    private volatile Collection<ServiceReference<ReplyToManySubscriptionHandler>> manySubHandlers;

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
        publish(message, null, null, NO_REPLY_TO);
    }

    @Override
    public void publish(final Message message, final String channel) {
        publish(message, null, channel, NO_REPLY_TO);
    }

    @Override
    public void publish(final Message message, final MessageContext context) {
        publish(message, context, null, NO_REPLY_TO);
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage) {
        return publish(requestMessage, null, null, REPLY_TO_SINGLE).promise;
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage, final MessageContext replyToContext) {
        return publish(requestMessage, replyToContext, null, REPLY_TO_SINGLE).promise;
    }

    @Override
    public PushStream<Message> publishWithReplyMany(final Message requestMessage) {
        return publish(requestMessage, null, null, REPLY_TO_MANY).pushStream;
    }

    @Override
    public PushStream<Message> publishWithReplyMany(final Message requestMessage, final MessageContext replyToContext) {
        return publish(requestMessage, replyToContext, null, REPLY_TO_MANY).pushStream;
    }

    private ResponseDTO publish(final Message message, MessageContext context, String channel,
            final CommunicationType commType) {
        if (context == null) {
            context = message.getContext();
        }
        if (channel == null) {
            channel = context.getChannel();
        }

        final SimpleMessageContext cxt = (SimpleMessageContext) context;
        final ResponseDTO result = new ResponseDTO();
        final Deferred<Message> deferred = factory.deferred();
        final PushStreamProvider provider = new PushStreamProvider();
        final SimplePushEventSource<Message> source = provider.createSimpleEventSource(Message.class);

        final Map<String, Object> extensions = context.getExtensions();
        final String contentType = context.getContentType();
        final String ctxCorrelationId = context.getCorrelationId();
        final String correlationId = ctxCorrelationId != null ? ctxCorrelationId : UUID.randomUUID().toString();
        final ByteBuffer content = message.payload();
        final long messageExpiryInterval = (long) extensions.getOrDefault(MESSAGE_EXPIRY_INTERVAL, MAX_VALUE);
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
        if (commType == CommunicationType.REPLY_TO_SINGLE || commType == CommunicationType.REPLY_TO_MANY) {
            String replyChannel = context.getReplyToChannel();
            if (replyChannel == null) {
                replyChannel = UUID.randomUUID().toString();
            }
            final String replyToChannel = replyChannel;
            publishRequest.responseTopic(replyToChannel).correlationData(correlationId.getBytes());
            messagingClient.client.toAsync()
                                  .subscribeWith()
                                  .topicFilter(replyToChannel)
                                  .qos(MqttQos.fromCode(qos))
                                  .callback(m -> {
                                      try {
                                          final MessageContextBuilder mcb = mcbFactory.getService();
                                          final Message msg = toMessage(m, mcb);

                                          if (commType == REPLY_TO_SINGLE) {
                                              acknowledgeMessage(message, cxt, m1 -> deferred.resolve(m1));
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
                                          } else {
                                              acknowledgeMessage(message, cxt, m1 -> source.publish(m1));
                                              manySubHandlers.stream()
                                                             .filter(h -> filterHandler(h, replyToChannel))
                                                             .sorted(Comparator.reverseOrder())
                                                             .map(s -> bundleContext.getService(s))
                                                            . forEach(h -> h.handleResponses(message, mcb));
                                          }
                                      } finally {
                                          bundleContext.ungetService(mcbFactory.getServiceReference());
                                      }
                                  })
                                  .send()
                                  .thenCompose(subAck -> publishRequest.send());
            result.promise = deferred.getPromise();
            result.pushStream = provider.createStream(source);
        } else {
            publishRequest.send();
        }
        return result;
        // @formatter:on
    }

    private static boolean filterHandler(final ServiceReference<?> ref, final String channel) {
        final String property = (String) ref.getProperty("osgi.messaging.replyToSubscription.channel");
        return property != null;
    }

    enum CommunicationType {
        REPLY_TO_SINGLE,
        REPLY_TO_MANY,
        NO_REPLY_TO
    }

    static class ResponseDTO {
        Promise<Message> promise;
        PushStream<Message> pushStream;
    }

}