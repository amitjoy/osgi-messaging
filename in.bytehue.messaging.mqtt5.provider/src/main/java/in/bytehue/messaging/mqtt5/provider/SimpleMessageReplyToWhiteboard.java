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
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.findServiceRefAsDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getServiceReferenceDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.initChannelDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.prepareExceptionAsMessage;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;
import static org.osgi.service.messaging.Features.REPLY_TO;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToWhiteboard;
import org.osgi.util.pushstream.PushStream;

@MessagingFeature(name = MESSAGE_WHITEBOARD, protocol = MQTT_PROTOCOL)
@Component(service = { ReplyToWhiteboard.class, SimpleMessageReplyToWhiteboard.class })
public final class SimpleMessageReplyToWhiteboard implements ReplyToWhiteboard {

    // @formatter:off
    private static final String KEY_PROTOCOL       = "osgi.messaging.protocol";
    private static final String KEY_SUB_CHANNEL    = "osgi.messaging.replyToSubscription.channel";
    private static final String KEY_PUB_CHANNEL    = "osgi.messaging.replyToSubscription.replyChannel";

    public static final String FILTER_MQTT         = "(" + KEY_PROTOCOL +"=" + MQTT_PROTOCOL + ")";
    public static final String FILTER_MESSAGING_ID = "(" + KEY_PROTOCOL +"=" + MESSAGING_ID + ")";
    public static final String FILTER_REPLY_TO     = "(" + KEY_PROTOCOL +"=" + REPLY_TO + ")";

    private static final String FILTER_HANDLER =
            "(osgi.messaging.replyToSubscription.target=(&" +
                    FILTER_MQTT +
                    FILTER_MESSAGING_ID +
                    FILTER_REPLY_TO + "))";
    // @formatter:on

    @Activate
    private BundleContext bundleContext;

    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Reference
    private SimpleMessagePublisher publisher;

    @Reference
    private SimpleMessageSubscriber subscriber;

    @Reference(scope = PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<SimpleMessageContextBuilder> mcbFactory;

    private final Map<ServiceReference<?>, ReplyToSubscriptionDTO> subscriptions = new ConcurrentHashMap<>();

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE, target = FILTER_HANDLER)
    synchronized void bindReplyToSingleSubscriptionHandler( //
            final ReplyToSingleSubscriptionHandler handler, //
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        // @formatter:off
        subscriber.subscribe(replyToDTO.subChannel)
                  .map(m -> handleResponse(m, handler))
                  .forEach(m -> publisher.publish(m, replyToDTO.pubChannel))
                  .onResolve(replyToDTO::close);
        // @formatter:on
    }

    void unbindReplyToSingleSubscriptionHandler(final ServiceReference<?> reference) {
        subscriptions.remove(reference);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE, target = FILTER_HANDLER)
    synchronized void bindReplyToSubscriptionHandler( //
            final ReplyToSubscriptionHandler handler, //
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        // @formatter:off
        subscriber.subscribe(replyToDTO.subChannel)
                  .forEach(m -> handler.handleResponse(m))
                  .onResolve(replyToDTO::close);
        // @formatter:on
    }

    void unbindReplyToSubscriptionHandler(final ServiceReference<?> reference) {
        subscriptions.remove(reference);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE, target = FILTER_HANDLER)
    synchronized void bindReplyToManySubscriptionHandler( //
            final ReplyToManySubscriptionHandler handler, //
            final ServiceReference<?> reference) {

        final ReplyToDTO replyToDTO = new ReplyToDTO(reference);

        // @formatter:off
        subscriber.subscribe(replyToDTO.subChannel)
                  .map(m -> handleResponses(m, handler))
                  .flatMap(m -> m)
                  .onClose(replyToDTO::close)
                  .forEach(m -> publisher.publish(m, replyToDTO.subChannel));
        // @formatter:on
    }

    void unbindReplyToManySubscriptionHandler(final ServiceReference<?> reference) {
        subscriptions.remove(reference);
    }

    public ReplyToSubscriptionDTO[] getReplyToSubscriptionDTOs() {
        final ReplyToSubscriptionDTO dto = new ReplyToSubscriptionDTO();

        dto.generateCorrelationId = true;
        dto.generateReplyChannel = true;
        dto.serviceDTO = findServiceRefAsDTO(MessageSubscription.class, bundleContext);

        return new ReplyToSubscriptionDTO[] { dto };
    }

    private Message handleResponse(final Message request, final ReplyToSingleSubscriptionHandler handler) {
        final SimpleMessageContextBuilder mcb = initResponse(request);
        try {
            return handler.handleResponse(request, mcb).getValue();
        } catch (final Exception e) {
            return prepareExceptionAsMessage(e, mcb);
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }

    private SimpleMessageContextBuilder initResponse(final Message request) {
        final MessageContext context = request.getContext();
        final String channel = context.getReplyToChannel();
        final String correlation = context.getCorrelationId();
        final MessageContextBuilder builder = mcbFactory.getService().channel(channel).correlationId(correlation);
        return (SimpleMessageContextBuilder) builder;
    }

    private PushStream<Message> handleResponses(final Message request, final ReplyToManySubscriptionHandler handler) {
        final MessageContextBuilder mcb = initResponse(request);
        return handler.handleResponses(request, mcb);
    }

    private ReplyToSubscriptionDTO initReplyToSubscriptionDTO( //
            final ChannelDTO pub, //
            final ChannelDTO sub, //
            final ServiceReference<?> reference) {

        final ReplyToSubscriptionDTO subscriptionDTO = new ReplyToSubscriptionDTO();

        subscriptionDTO.requestChannel = sub;
        subscriptionDTO.responseChannel = pub;
        subscriptionDTO.handlerService = getServiceReferenceDTO(reference, bundleContext.getBundle().getBundleId());
        subscriptionDTO.serviceDTO = findServiceRefAsDTO(MessageSubscription.class, bundleContext);
        subscriptionDTO.generateCorrelationId = true;
        subscriptionDTO.generateReplyChannel = true;

        return subscriptionDTO;
    }

    private class ReplyToDTO {
        String pubChannel;
        String subChannel;
        ServiceReference<?> reference;

        ReplyToDTO(final ServiceReference<?> reference) {
            this.reference = reference;
            final Dictionary<String, Object> properties = reference.getProperties();

            pubChannel = (String) properties.get(KEY_PUB_CHANNEL);
            subChannel = (String) properties.get(KEY_SUB_CHANNEL);

            if (subChannel == null) {
                throw new RuntimeException(
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
                    throw new RuntimeException(
                            "The '" + reference + "' handler instance doesn't specify the reply-to publish channel");
                }
            }
            final String rountingKey = null; // no routing key for MQTT
            final ChannelDTO pubDTO = initChannelDTO(pubChannel, rountingKey, true);
            final ChannelDTO subDTO = initChannelDTO(subChannel, rountingKey, true);

            final ReplyToSubscriptionDTO subscriptionDTO = initReplyToSubscriptionDTO(pubDTO, subDTO, reference);
            subscriptions.put(reference, subscriptionDTO);
        }

        void close() {
            final ReplyToSubscriptionDTO dto = subscriptions.get(reference);
            dto.requestChannel.connected = false;
            dto.responseChannel.connected = false;
        }
    }

}
