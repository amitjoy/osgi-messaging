package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.MessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MessageConstants.MQTT_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MessageConstants.Component.MESSAGE_WHITEBOARD;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.findServiceRefAsDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getServiceReferenceDTO;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.initChannelDTO;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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

    public static final String FILTER_MQTT = "(osgi.messaging.protocol=" + MQTT_PROTOCOL + ")";

    // @formatter:off
    // TODO Does this filter really make sense?
    public static final String FILTER_HANDLER = ""
            + "(osgi.messaging.replyToSubscription.target="
            + "(&(osgi.messaging.name=" + MESSAGING_ID + ")"
            + FILTER_MQTT
            + "(osgi.messaging.feature=replyTo)))";
    // @formatter:on

    @Activate
    private BundleContext bundleContext;

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
                  .onClose(() -> {
                      final ReplyToSubscriptionDTO dto = subscriptions.get(reference);
                      dto.requestChannel.connected = false;
                      dto.responseChannel.connected = false;
                  })
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

    private SimpleMessageContextBuilder initResponse(final Message request) {
        final MessageContext context = request.getContext();
        final String channel = context.getReplyToChannel();
        final String correlation = context.getCorrelationId();
        final MessageContextBuilder builder = mcbFactory.getService().channel(channel).correlationId(correlation);
        return (SimpleMessageContextBuilder) builder;
    }

    private Message handleResponse(final Message request, final ReplyToSingleSubscriptionHandler handler) {
        final SimpleMessageContextBuilder mcb = initResponse(request);
        try {
            return handler.handleResponse(request, mcb).getValue();
        } catch (final Exception e) {
            final Message error = mcb.content(ByteBuffer.wrap(e.getMessage().getBytes())).buildMessage();
            return error;
        } finally {
            mcbFactory.ungetService(mcb);
        }
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

            pubChannel = (String) properties.get("osgi.messaging.replyToSubscription.replyChannel");
            subChannel = (String) properties.get("osgi.messaging.replyToSubscription.channel");

            final ChannelDTO pubDTO = initChannelDTO(pubChannel, null, true);
            final ChannelDTO subDTO = initChannelDTO(subChannel, null, true);

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
