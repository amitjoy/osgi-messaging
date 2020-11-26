package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MESSAGE_REPLY_TO_PUBLISHER_NAME;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.ExtendedMessagingConstants.PUBLISHER_PID;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.annotations.ProvideMessagingReplyToFeature;
import org.osgi.service.messaging.annotations.ProvideMessagingReplyToManySubscribeFeature;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;
import org.osgi.service.messaging.replyto.ReplyToPublisher;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.pushstream.PushStream;

import in.bytehue.messaging.mqtt5.provider.SimpleMessageReplyToPublisher.Config;
import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

@Designate(ocd = Config.class)
@ProvideMessagingReplyToFeature
@Component(configurationPid = PUBLISHER_PID)
@ProvideMessagingReplyToManySubscribeFeature
@MessagingFeature( //
        name = MESSAGE_REPLY_TO_PUBLISHER_NAME, //
        protocol = MQTT_PROTOCOL, //
        feature = { //
                REPLY_TO, //
                REPLY_TO_MANY_PUBLISH, //
                REPLY_TO_MANY_SUBSCRIBE, //
                GENERATE_CORRELATION_ID, //
                GENERATE_REPLY_CHANNEL })
public final class SimpleMessageReplyToPublisher implements ReplyToPublisher, ReplyToManyPublisher {

    @ObjectClassDefinition( //
            name = "MQTT Messaging Reply-To Publisher Executor Configuration", //
            description = "This configuration is used to configure the internal thread pool size")
    @interface Config {
        @AttributeDefinition(name = "Number of Threads for the internal thread pool")
        int numThreads() default 10;
    }

    private static final String THREAD_NAME_FORMAT = "-%d";
    private static final String THREAD_FACTORY_NAME = "mqtt-replyto-publisher";

    private final PromiseFactory factory;

    @Reference
    private SimpleMessagePublisher publisher;

    @Reference
    private SimpleMessageSubscriber subscriber;

    @Activate
    public SimpleMessageReplyToPublisher(final Config config) {
        final ThreadFactory threadFactory = //
                new ThreadFactoryBuilder() //
                        .setThreadFactoryName(THREAD_FACTORY_NAME) //
                        .setThreadNameFormat(THREAD_NAME_FORMAT) //
                        .build();
        factory = new PromiseFactory(Executors.newFixedThreadPool(config.numThreads(), threadFactory));
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage) {
        return publishWithReply(requestMessage, null);
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage, final MessageContext replyToContext) {
        autoGenerateMissingConfigs(requestMessage);

        final Deferred<Message> deferred = factory.deferred();
        final ReplyToDTO dto = new ReplyToDTO(requestMessage, replyToContext);

        // @formatter:off
        subscriber.subscribe(dto.subChannel)
                  .forEach(m -> {
                      publisher.publish(m, dto.pubChannel);
                      deferred.resolve(m);
                  });
        // @formatter:off
        return deferred.getPromise();
    }

    @Override
    public PushStream<Message> publishWithReplyMany(final Message requestMessage) {
        return publishWithReplyMany(requestMessage, null);
    }

    @Override
    public PushStream<Message> publishWithReplyMany(final Message requestMessage, final MessageContext replyToContext) {
        autoGenerateMissingConfigs(requestMessage);
        final ReplyToDTO dto = new ReplyToDTO(requestMessage, replyToContext);

        // @formatter:off
        return subscriber.subscribe(dto.subChannel)
                         .map(m -> {
                             publisher.publish(m, dto.pubChannel);
                             return m;
                          });
        // @formatter:off
    }

    private void autoGenerateMissingConfigs(final Message message) {
        final SimpleMessageContext context = (SimpleMessageContext) message.getContext();
        if (context.getCorrelationId() == null) {
            context.correlationId = UUID.randomUUID().toString();
        }
        if (context.getReplyToChannel() == null) {
            context.replyToChannel = UUID.randomUUID().toString();
        }
    }

    private class ReplyToDTO {
        String pubChannel;
        String subChannel;

        ReplyToDTO(final Message message, MessageContext context) {
            if (context == null) {
                context = message.getContext();
            }
            pubChannel = context.getReplyToChannel();
            subChannel = context.getChannel();
        }
    }

}
