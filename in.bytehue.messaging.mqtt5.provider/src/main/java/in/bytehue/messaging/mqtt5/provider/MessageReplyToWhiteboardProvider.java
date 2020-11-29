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
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.prepareExceptionAsMessage;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.messaging.Features.REPLY_TO;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
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

import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;

// TODO remove immediate = true
// @formatter:off
@MessagingFeature(name = MESSAGING_ID, protocol = MESSAGING_PROTOCOL)
@Component(service = { ReplyToWhiteboard.class, MessageReplyToWhiteboardProvider.class }, immediate = true)
public final class MessageReplyToWhiteboardProvider implements ReplyToWhiteboard {

    public static final String KEY_NAME         = "osgi.messaging.name";
    public static final String KEY_TARGET       = "osgi.messaging.replyToSubscription.target";
    public static final String KEY_FEATURE      = "osgi.messaging.feature";
    public static final String KEY_PROTOCOL     = "osgi.messaging.protocol";
    public static final String KEY_SUB_CHANNEL  = "osgi.messaging.replyToSubscription.channel";
    public static final String KEY_PUB_CHANNEL  = "osgi.messaging.replyToSubscription.replyChannel";

    private final MessagePublisherProvider publisher;
    private final MessageSubscriptionProvider subscriber;
    private final Map<ServiceReference<?>, List<PushStream<?>>> streams;
    private final ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

    @Activate
    public MessageReplyToWhiteboardProvider(
            @Reference
            final MessagePublisherProvider publisher,
            @Reference
            final MessageSubscriptionProvider subscriber,
            @Reference
            final ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory) {

        this.publisher = publisher;
        this.subscriber = subscriber;
        this.mcbFactory = mcbFactory;

        streams = new ConcurrentHashMap<>();
    }

    @Deactivate
    void stop() {
        streams.values().forEach(list -> list.forEach(PushStream::close));
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    synchronized void bindReplyToSingleSubscriptionHandler( //
            final ReplyToSingleSubscriptionHandler handler, //
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);
        if (!replyToDTO.isConform) {
            return;
        }
        Stream.of(replyToDTO.subChannels)
              .forEach(c -> replyToSubscribe(c, replyToDTO.pubChannel, reference)
                                  .map(m -> handleResponse(m, handler))
                                  .forEach(m -> publisher.publish(m, replyToDTO.pubChannel)));
    }

    void unbindReplyToSingleSubscriptionHandler(final ServiceReference<?> reference) {
        closeConnectedPushstreams(reference);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    synchronized void bindReplyToSubscriptionHandler(
            final ReplyToSubscriptionHandler handler,
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);
        if (!replyToDTO.isConform) {
            return;
        }
        Stream.of(replyToDTO.subChannels)
              .forEach(c -> replyToSubscribe(c, replyToDTO.pubChannel, reference)
                                  .forEach(handler::handleResponse));
    }

    void unbindReplyToSubscriptionHandler(final ServiceReference<?> reference) {
        closeConnectedPushstreams(reference);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    synchronized void bindReplyToManySubscriptionHandler(
            final ReplyToManySubscriptionHandler handler,
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);
        if (!replyToDTO.isConform) {
            return;
        }
        Stream.of(replyToDTO.subChannels)
              .forEach(c -> replyToSubscribe(c, replyToDTO.pubChannel, reference)
                                  .map(m -> handleResponses(m, handler))
                                  .flatMap(m -> m)
                                  .forEach(m -> publisher.publish(m, replyToDTO.pubChannel)));
    }

    void unbindReplyToManySubscriptionHandler(final ServiceReference<?> reference) {
        closeConnectedPushstreams(reference);
    }

    private Message handleResponse(final Message request, final ReplyToSingleSubscriptionHandler handler) {
        final MessageContextBuilderProvider mcb = getResponse(request);
        try {
            return handler.handleResponse(request, mcb);
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

        return (MessageContextBuilderProvider) mcbFactory.getService().channel(channel).correlationId(correlation);
    }

    private PushStream<Message> handleResponses(final Message request, final ReplyToManySubscriptionHandler handler) {
        final MessageContextBuilder mcb = getResponse(request);
        return handler.handleResponses(request, mcb);
    }

    private PushStream<Message> replyToSubscribe(
            final String subChannel,
            final String pubChannel,
            final ServiceReference<?> reference) {

        final PushStream<Message> stream = subscriber.replyToSubscribe(subChannel, pubChannel, reference);
        streams.computeIfAbsent(reference, s -> new ArrayList<>()).add(stream);
        return stream;
    }

    private static class ReplyToDTO {

        boolean isConform;
        String pubChannel;
        String[] subChannels;

        ReplyToDTO(final ServiceReference<?> reference) {
            final Dictionary<String, ?> properties = reference.getProperties();

            pubChannel = (String) properties.get(KEY_PUB_CHANNEL);
            subChannels = (String[]) properties.get(KEY_SUB_CHANNEL);

            if (subChannels == null) {
                throw new IllegalArgumentException("The '" + reference
                        + "' handler instance doesn't specify the reply-to subscription channel(s)");
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
            final String replyToSubTarget = (String) properties.get(KEY_TARGET);
            final FilterParser fp = new FilterParser();
            final Expression exp = fp.parse(replyToSubTarget);

            final Map<String, String> requiredValues = new HashMap<>();

            requiredValues.put(KEY_NAME, MESSAGING_ID);
            requiredValues.put(KEY_FEATURE, REPLY_TO);
            requiredValues.put(KEY_PROTOCOL, MESSAGING_PROTOCOL);

            isConform = exp.eval(requiredValues);
        }
    }

    private void closeConnectedPushstreams(final ServiceReference<?> reference) {
        Optional.ofNullable(streams.remove(reference)).ifPresent(s -> s.forEach(PushStream::close));
    }

}
