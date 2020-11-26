package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.asString;

import java.nio.ByteBuffer;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;

public final class SimpleMessage implements Message {

    public ByteBuffer byteBuffer;
    public MessageContext messageContext;

    SimpleMessage() {
        // used for internal purposes
    }

    SimpleMessage(final ByteBuffer byteBuffer, final MessageContext messageContext) {
        this.byteBuffer = byteBuffer;
        this.messageContext = messageContext;
    }

    @Override
    public ByteBuffer payload() {
        return byteBuffer;
    }

    @Override
    public MessageContext getContext() {
        return messageContext;
    }

    @Override
    public String toString() {
        return "Message [payload=" + asString(byteBuffer) + ", messageContext=" + messageContext + "]";
    }

}
