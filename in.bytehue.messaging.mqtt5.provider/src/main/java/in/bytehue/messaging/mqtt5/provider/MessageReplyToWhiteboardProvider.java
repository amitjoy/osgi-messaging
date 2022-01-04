/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.adaptTo;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.prepareExceptionAsMessage;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_FEATURE_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_NAME_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_RESPONSE_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;

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
import org.osgi.util.converter.Converter;
import org.osgi.util.pushstream.PushStream;

import in.bytehue.messaging.mqtt5.provider.helper.FilterParser;
import in.bytehue.messaging.mqtt5.provider.helper.FilterParser.Expression;

@Component
@MessagingFeature(name = MESSAGING_ID, protocol = MESSAGING_PROTOCOL)
// @formatter:off
public final class MessageReplyToWhiteboardProvider {

    private final Converter converter;
    private final MessagePublisherProvider publisher;
    private final MessageSubscriptionProvider subscriber;
    private final Map<ServiceReference<?>, List<PushStream<?>>> streams;
    private final ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

    @Activate
    public MessageReplyToWhiteboardProvider(
            @Reference
            final ConverterAdapter converter,
            @Reference
            final MessagePublisherProvider publisher,
            @Reference
            final MessageSubscriptionProvider subscriber,
            @Reference
            final ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory) {

        this.converter = converter;
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
    synchronized void bindReplyToSingleSubscriptionHandler(
            final ReplyToSingleSubscriptionHandler handler,
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        Stream.of(replyToDTO.subChannels)
              .forEach(c -> replyToSubscribe(c, replyToDTO.pubChannel, reference)
                                  .map(m -> handleResponse(m, handler))
                                  .forEach(m -> publisher.publish(m, replyToDTO.pubChannel)));
    }

    void unbindReplyToSingleSubscriptionHandler(final ServiceReference<?> reference) {
        closeConnectedPushStreams(reference);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    synchronized void bindReplyToSubscriptionHandler(
            final ReplyToSubscriptionHandler handler,
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        Stream.of(replyToDTO.subChannels)
              .forEach(c -> replyToSubscribe(c, replyToDTO.pubChannel, reference)
                                  .forEach(handler::handleResponse));
    }

    void unbindReplyToSubscriptionHandler(final ServiceReference<?> reference) {
        closeConnectedPushStreams(reference);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    synchronized void bindReplyToManySubscriptionHandler(
            final ReplyToManySubscriptionHandler handler,
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        Stream.of(replyToDTO.subChannels)
              .forEach(c -> replyToSubscribe(c, replyToDTO.pubChannel, reference)
                                  .forEach(m ->
                                      handleResponses(m, handler)
                                          .forEach(msg -> publisher.publish(msg, replyToDTO.pubChannel))));
    }

    void unbindReplyToManySubscriptionHandler(final ServiceReference<?> reference) {
        closeConnectedPushStreams(reference);
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

        return (MessageContextBuilderProvider)
                    mcbFactory.getService()
                              .channel(channel)
                              .correlationId(correlation)
                              .content(request.payload());
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

    private class ReplyToDTO {

        boolean isConform;
        String pubChannel;
        String[] subChannels;

        ReplyToDTO(final ServiceReference<?> reference) {
            final Dictionary<String, ?> properties = reference.getProperties();

            final Object replyToSubResponse = properties.get(REPLY_TO_SUBSCRIPTION_RESPONSE_CHANNEL_PROPERTY);
            final Object replyToSubRequest = properties.get(REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY);

            pubChannel = adaptTo(replyToSubResponse, String.class, converter);
            subChannels = adaptTo(replyToSubRequest, String[].class, converter);

            if (subChannels == null) {
                throw new IllegalStateException("The '" + reference
                        + "' handler instance doesn't specify the reply-to subscription channel(s)");
            }
            if (pubChannel == null && !isReplyToSubscriptionHandler(reference)) {
                throw new IllegalStateException(
                        "The '" + reference + "' handler instance doesn't specify the reply-to publish channel");
            }

            final Object replyToSubTgt = properties.get(REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY);
            final String replyToSubTarget = adaptTo(replyToSubTgt, String.class, converter);

            final FilterParser fp = new FilterParser();
            final Expression exp = fp.parse(replyToSubTarget);

            final Map<String, String> requiredValues = new HashMap<>();

            requiredValues.put(MESSAGING_FEATURE_PROPERTY, REPLY_TO);
            requiredValues.put(MESSAGING_NAME_PROPERTY, MESSAGING_ID);
            requiredValues.put(MESSAGING_PROTOCOL_PROPERTY, MESSAGING_PROTOCOL);

            isConform = exp.eval(requiredValues);

            if (!isConform) {
                throw new IllegalStateException(
                        "The '" + reference + "' handler service doesn't specify the reply-to target filter");
            }
        }

        private boolean isReplyToSubscriptionHandler(final ServiceReference<?> ref) {
            final String[] serviceTypes = (String[]) ref.getProperty(OBJECTCLASS);
            return Stream.of(serviceTypes).anyMatch(ReplyToSubscriptionHandler.class.getName()::equals);
        }
    }

    private void closeConnectedPushStreams(final ServiceReference<?> reference) {
        Optional.ofNullable(streams.remove(reference)).ifPresent(s -> s.forEach(PushStream::close));
    }

}
