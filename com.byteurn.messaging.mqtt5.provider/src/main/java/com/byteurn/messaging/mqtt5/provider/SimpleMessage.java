package com.byteurn.messaging.mqtt5.provider;

import java.nio.ByteBuffer;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;

public final class SimpleMessage implements Message {

    ByteBuffer byteBuffer;
    MessageContext messageContext;

    boolean isAcknowledged;

    SimpleMessage() {
        // used for testing
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

}
