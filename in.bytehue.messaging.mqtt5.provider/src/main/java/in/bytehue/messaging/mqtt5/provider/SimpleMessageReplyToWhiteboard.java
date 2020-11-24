package in.bytehue.messaging.mqtt5.provider;

import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;

import java.nio.ByteBuffer;
import java.util.Map;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToWhiteboard;
import org.osgi.util.pushstream.PushStream;

@Component
public final class SimpleMessageReplyToWhiteboard implements ReplyToWhiteboard {

    private static final String FILTER_MQTT_5 = "(osgi.messaging.protocol=mqtt5)";

    // @formatter:off
    private static final String FILTER_SUB_HANDLER = ""
            + "(osgi.messaging.replyToSubscription.target="
            + "(&(osgi.messaging.name=message-publisher)"
            + FILTER_MQTT_5
            + "(osgi.messaging.feature=replyTo)))";
    // @formatter:on

    @Reference(scope = PROTOTYPE_REQUIRED)
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    @Reference(target = FILTER_MQTT_5)
    private MessagePublisher publisher;

    @Reference(target = FILTER_MQTT_5)
    private MessageSubscription subscriber;

    @Reference(policy = DYNAMIC, target = FILTER_SUB_HANDLER)
    public synchronized void bindReplyToSingleSubscriptionHandler( //
            final ReplyToSingleSubscriptionHandler handler, //
            final Map<String, Object> properties) {

        final String pubChannel = (String) properties.get("osgi.messaging.replyToSubscription.replyChannel");
        final String subChannel = (String) properties.get("osgi.messaging.replyToSubscription.channel");

        // @formatter:off
        subscriber.subscribe(subChannel)
                  .map(m -> handleResponse(m, handler))
                  .forEach(m -> publisher.publish(m, pubChannel));
        // @formatter:on
    }

    void unbindReplyToSingleSubscriptionHandler(final ReplyToSingleSubscriptionHandler handler) {
        // nothing to do
    }

    @Reference(policy = DYNAMIC, target = FILTER_SUB_HANDLER)
    public synchronized void bindReplyToSubscriptionHandler( //
            final ReplyToSubscriptionHandler handler, //
            final Map<String, Object> properties) {
        final String subChannel = (String) properties.get("osgi.messaging.replyToSubscription.channel");
        subscriber.subscribe(subChannel).forEach(m -> handler.handleResponse(m));
    }

    void unbindReplyToSubscriptionHandler(final ReplyToSubscriptionHandler handler) {
        // nothing to do
    }

    @Reference(policy = DYNAMIC, target = FILTER_SUB_HANDLER)
    synchronized void bindReplyToManySubscriptionHandler( //
            final ReplyToManySubscriptionHandler handler, //
            final Map<String, Object> properties) {

        final String pubChannel = (String) properties.get("osgi.messaging.replyToSubscription.replyChannel");
        final String subChannel = (String) properties.get("osgi.messaging.replyToSubscription.channel");

        // @formatter:off
        subscriber.subscribe(subChannel)
                  .map(m -> handleResponses(m, handler))
                  .flatMap(m -> m)
                  .forEach(m -> publisher.publish(m, pubChannel));
        // @formatter:on
    }

    void unbindReplyToManySubscriptionHandler(final ReplyToManySubscriptionHandler handler) {
        // nothing to do
    }

    private MessageContextBuilder initResponse(final Message request) {
        final MessageContext requestCtx = request.getContext();
        final String channel = requestCtx.getReplyToChannel();
        final String correlation = requestCtx.getCorrelationId();
        final MessageContextBuilder mcb = mcbFactory.getService().channel(channel).correlationId(correlation);
        return mcb;
    }

    private Message handleResponse(final Message request, final ReplyToSingleSubscriptionHandler handler) {
        final MessageContextBuilder mcb = initResponse(request);
        try {
            return handler.handleResponse(request, mcb).getValue();
        } catch (final Exception e) {
            final Message error = mcb.content(ByteBuffer.wrap(e.getMessage().getBytes())).buildMessage();
            return error;
        }
    }

    private PushStream<Message> handleResponses(final Message request, final ReplyToManySubscriptionHandler handler) {
        final MessageContextBuilder mcb = initResponse(request);
        return handler.handleResponses(request, mcb);
    }

}
