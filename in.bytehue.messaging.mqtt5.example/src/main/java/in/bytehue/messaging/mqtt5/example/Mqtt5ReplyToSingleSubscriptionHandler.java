package in.bytehue.messaging.mqtt5.example;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.propertytypes.ReplyToSubscription;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;

@Component
@ReplyToSubscription(target = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo)))", channel = "a/b", replyChannel = "c/d")
public final class Mqtt5ReplyToSingleSubscriptionHandler implements ReplyToSingleSubscriptionHandler {

    @Override
    public Message handleResponse(final Message requestMessage, final MessageContextBuilder responseBuilder) {
        final String content = StandardCharsets.UTF_8.decode(requestMessage.payload()).append("EXAMPLE").toString();
        return responseBuilder.content(ByteBuffer.wrap(content.getBytes())).buildMessage();
    }

}
