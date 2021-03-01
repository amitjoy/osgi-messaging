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
package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.PUBLISHER;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getCorrelationId;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.osgi.service.messaging.Features.GENERATE_CORRELATION_ID;
import static org.osgi.service.messaging.Features.GENERATE_REPLY_CHANNEL;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.annotations.ProvideMessagingReplyToManyFeature;
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

import in.bytehue.messaging.mqtt5.provider.MessageReplyToPublisherProvider.Config;
import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

//@formatter:off
@Designate(ocd = Config.class)
@ProvideMessagingReplyToManyFeature
@Component(configurationPid = PUBLISHER)
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                REPLY_TO,
                REPLY_TO_MANY_PUBLISH,
                REPLY_TO_MANY_SUBSCRIBE,
                GENERATE_CORRELATION_ID,
                GENERATE_REPLY_CHANNEL })
public final class MessageReplyToPublisherProvider implements ReplyToPublisher, ReplyToManyPublisher {

    @ObjectClassDefinition(
            name = "MQTT Messaging Reply-To Publisher Executor Configuration",
            description = "This configuration is used to configure the internal thread pool")
    @interface Config {
        @AttributeDefinition(name = "Number of threads for the internal thread pool")
        int numThreads() default 20;

        @AttributeDefinition(name = "Prefix of the thread name")
        String threadNamePrefix() default "mqtt-replyto-publisher";

        @AttributeDefinition(name = "Suffix of the thread name (supports only {@code %d} format specifier)")
        String threadNameSuffix() default "-%d";

        @AttributeDefinition(name = "Flag to set if the threads will be daemon threads")
        boolean isDaemon() default true;
    }
    //@formatter:on

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private MessagePublisherProvider publisher;

    @Reference
    private MessageSubscriptionProvider subscriber;

    @Activate
    private BundleContext bundleContext;

    private final PromiseFactory promiseFactory;

    @Activate
    public MessageReplyToPublisherProvider(final Config config) {
        //@formatter:off
        final ThreadFactory threadFactory =
                new ThreadFactoryBuilder()
                        .setThreadFactoryName(config.threadNamePrefix())
                        .setThreadNameFormat(config.threadNameSuffix())
                        .setDaemon(config.isDaemon())
                        .build();
        promiseFactory = new PromiseFactory(newFixedThreadPool(config.numThreads(), threadFactory));
        //@formatter:on
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage) {
        return publishWithReply(requestMessage, requestMessage.getContext());
    }

    @Override
    public Promise<Message> publishWithReply(final Message requestMessage, final MessageContext replyToContext) {
        final Deferred<Message> deferred = promiseFactory.deferred();
        final ReplyToDTO dto = new ReplyToDTO(requestMessage, replyToContext);

        publisher.publish(requestMessage, dto.pubChannel);
        // resolve the promise on first response
        subscriber.subscribe(dto.subChannel).forEach(deferred::resolve);
        return deferred.getPromise();
    }

    @Override
    public PushStream<Message> publishWithReplyMany(final Message requestMessage) {
        return publishWithReplyMany(requestMessage, requestMessage.getContext());
    }

    @Override
    public PushStream<Message> publishWithReplyMany(final Message requestMessage, final MessageContext replyToContext) {
        final ReplyToDTO dto = new ReplyToDTO(requestMessage, replyToContext);

        publisher.publish(requestMessage, dto.pubChannel);
        return subscriber.subscribe(dto.subChannel);
    }

    private class ReplyToDTO {
        String pubChannel;
        String subChannel;

        ReplyToDTO(final Message message, final MessageContext context) {
            autoGenerateCorrelationIdIfAbsent(message);
            autoGenerateReplyToChannelIfAbsent(message);

            pubChannel = context.getChannel();
            subChannel = context.getReplyToChannel();
        }

        private void autoGenerateCorrelationIdIfAbsent(final Message message) {
            final MessageContextProvider context = (MessageContextProvider) message.getContext();
            context.correlationId = getCorrelationId(context, bundleContext, logger);
        }

        private void autoGenerateReplyToChannelIfAbsent(final Message message) {
            final MessageContextProvider context = (MessageContextProvider) message.getContext();

            if (context.getReplyToChannel() == null) {
                context.replyToChannel = UUID.randomUUID().toString();
                logger.info("Auto-generated reply-to channel '{}' as it is missing in the request",
                        context.replyToChannel);
            }
        }
    }

}
