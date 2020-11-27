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

import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.Component.MESSAGE_WHITEBOARD;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.prepareExceptionAsMessage;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.messaging.Features.REPLY_TO;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToWhiteboard;
import org.osgi.util.pushstream.PushStream;

@MessagingFeature(name = MESSAGE_WHITEBOARD, protocol = MQTT_PROTOCOL)
@Component(service = { ReplyToWhiteboard.class, MessageReplyToWhiteboardProvider.class })
public final class MessageReplyToWhiteboardProvider implements ReplyToWhiteboard {

    // @formatter:off
    private static final String KEY_PROTOCOL         = "osgi.messaging.protocol";
    private static final String KEY_SUB_CHANNEL      = "osgi.messaging.replyToSubscription.channel";
    private static final String KEY_PUB_CHANNEL      = "osgi.messaging.replyToSubscription.replyChannel";

    private static final String FILTER_MQTT          = "(" + KEY_PROTOCOL +"=" + MQTT_PROTOCOL + ")";
    private static final String FILTER_MESSAGING_ID  = "(" + KEY_PROTOCOL +"=" + MESSAGING_ID + ")";
    private static final String FILTER_REPLY_TO      = "(" + KEY_PROTOCOL +"=" + REPLY_TO + ")";

    private static final String FILTER_HANDLER =
            "(osgi.messaging.replyToSubscription.target=(&" +
                    FILTER_MQTT +
                    FILTER_MESSAGING_ID +
                    FILTER_REPLY_TO + "))";
    // @formatter:on

    @Reference
    private MessagePublisherProvider publisher;

    @Reference
    private MessageSubscriberProvider subscriber;

    @Reference
    private ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

    private final Map<ServiceReference<?>, PushStream<?>> streams = new ConcurrentHashMap<>();

    @Deactivate
    void stop() {
        streams.values().forEach(PushStream::close);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE, target = FILTER_HANDLER)
    synchronized void bindReplyToSingleSubscriptionHandler( //
            final ReplyToSingleSubscriptionHandler handler, //
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        // @formatter:off
        replyToSubscribe(replyToDTO.subChannel, replyToDTO.pubChannel, reference)
                  .map(m -> handleResponse(m, handler))
                  .forEach(m -> publisher.publish(m, replyToDTO.pubChannel));
        // @formatter:on
    }

    void unbindReplyToSingleSubscriptionHandler(final ServiceReference<?> reference) {
        streams.remove(reference).close();
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE, target = FILTER_HANDLER)
    synchronized void bindReplyToSubscriptionHandler( //
            final ReplyToSubscriptionHandler handler, //
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        // @formatter:off
        replyToSubscribe(replyToDTO.subChannel, replyToDTO.pubChannel, reference)
                  .forEach(handler::handleResponse);
        // @formatter:on
    }

    void unbindReplyToSubscriptionHandler(final ServiceReference<?> reference) {
        streams.remove(reference).close();
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE, target = FILTER_HANDLER)
    synchronized void bindReplyToManySubscriptionHandler( //
            final ReplyToManySubscriptionHandler handler, //
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        // @formatter:off
        replyToSubscribe(replyToDTO.subChannel, replyToDTO.pubChannel, reference)
                  .map(m -> handleResponses(m, handler))
                  .flatMap(m -> m)
                  .forEach(m -> publisher.publish(m, replyToDTO.subChannel));
        // @formatter:on
    }

    void unbindReplyToManySubscriptionHandler(final ServiceReference<?> reference) {
        streams.remove(reference).close();
    }

    private Message handleResponse(final Message request, final ReplyToSingleSubscriptionHandler handler) {
        final MessageContextBuilderProvider mcb = getResponse(request);
        try {
            return handler.handleResponse(request, mcb).getValue();
        } catch (final Exception e) {
            return prepareExceptionAsMessage(e, mcb);
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }

    private MessageContextBuilderProvider getResponse(final Message request) {
        final MessageContext context = request.getContext();
        final String channel = context.getReplyToChannel();
        final String correlation = context.getCorrelationId();
        final MessageContextBuilder builder = mcbFactory.getService().channel(channel).correlationId(correlation);
        return (MessageContextBuilderProvider) builder;
    }

    private PushStream<Message> handleResponses(final Message request, final ReplyToManySubscriptionHandler handler) {
        final MessageContextBuilder mcb = getResponse(request);
        return handler.handleResponses(request, mcb);
    }

    private PushStream<Message> replyToSubscribe( //
            final String subChannel, //
            final String pubChannel, //
            final ServiceReference<?> reference) {

        final PushStream<Message> stream = subscriber.replyToSubscribe(subChannel, pubChannel, reference);
        streams.put(reference, stream);
        return stream;
    }

    private static class ReplyToDTO {
        String pubChannel;
        String subChannel;

        ReplyToDTO(final ServiceReference<?> reference) {
            final Dictionary<String, Object> properties = reference.getProperties();

            pubChannel = (String) properties.get(KEY_PUB_CHANNEL);
            subChannel = (String) properties.get(KEY_SUB_CHANNEL);

            if (subChannel == null) {
                throw new IllegalArgumentException(
                        "The '" + reference + "' handler instance doesn't specify the reply-to subscription channel");
            }
            if (pubChannel == null) {
                boolean isMissingPubChannelAllowed = false;

                final String[] serviceTypes = (String[]) properties.get(OBJECTCLASS);
                for (final String type : serviceTypes) {
                    if (ReplyToSubscriptionHandler.class.getName().equals(type)) {
                        isMissingPubChannelAllowed = true;
                        break;
                    }
                }
                if (!isMissingPubChannelAllowed) {
                    throw new IllegalArgumentException(
                            "The '" + reference + "' handler instance doesn't specify the reply-to publish channel");
                }
            }
        }

    }

}
