package in.bytehue.messaging.mqtt5.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext;
import org.osgi.service.messaging.acknowledge.AcknowledgeType;

public final class SimpleMessageContext implements MessageContext, AcknowledgeMessageContext {

    public String channel;
    public String contentType;
    public String contentEncoding;
    public String correlationId;
    public String replyToChannel;

    public AcknowledgeType acknowledgeState;
    public Consumer<Message> acknowledgeHandler;
    public Predicate<Message> acknowledgeFilter;
    public Consumer<Message> acknowledgeConsumer;
    // AcknowledgeHandler acknowledgeHandler; TODO

    public Map<String, Object> extensions = new HashMap<>();

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getContentEncoding() {
        return contentEncoding;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String getReplyToChannel() {
        return replyToChannel;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public AcknowledgeType getAcknowledgeState() {
        return acknowledgeState;
    }

    @Override
    public AcknowledgeHandler getAcknowledgeHandler() {
        // TODO
        return null;
    }

}
