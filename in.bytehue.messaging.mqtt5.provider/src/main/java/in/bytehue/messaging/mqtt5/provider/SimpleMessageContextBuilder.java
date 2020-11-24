package in.bytehue.messaging.mqtt5.provider;

import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder;
import org.osgi.service.messaging.annotations.ProvideMessagingAcknowledgeFeature;
import org.osgi.service.messaging.propertytypes.MessagingFeature;

import in.bytehue.messaging.mqtt5.provider.helper.MessagingHelper;

@Component(scope = PROTOTYPE)
@ProvideMessagingAcknowledgeFeature
@MessagingFeature( //
        name = "message-context-builder", //
        protocol = "mqtt5", //
        feature = { //
                "messageContextBuilder", //
                "acknowledge" })
public final class SimpleMessageContextBuilder implements MessageContextBuilder, AcknowledgeMessageContextBuilder {

    private final Logger logger;
    private final SimpleMessage message;
    private final BundleContext bundleContext;
    private SimpleMessageContext messageContext;

    @Activate
    public SimpleMessageContextBuilder( //
            final BundleContext bundleContext, //
            @Reference(service = LoggerFactory.class) final Logger logger) {
        this.logger = logger;
        this.bundleContext = bundleContext;
        message = new SimpleMessage();
        messageContext = new SimpleMessageContext();
    }

    @Override
    public MessageContext buildContext() {
        return messageContext;
    }

    @Override
    public Message buildMessage() {
        return message;
    }

    @Override
    public MessageContextBuilder withContext(final MessageContext context) {
        if (context instanceof SimpleMessageContext) {
            messageContext = (SimpleMessageContext) context;
        }
        return this;
    }

    @Override
    public MessageContextBuilder content(final ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            message.byteBuffer = byteBuffer;
        }
        return this;
    }

    @Override
    public <T> MessageContextBuilder content(final T object, final Function<T, ByteBuffer> contentMapper) {
        if (object != null && contentMapper != null) {
            message.byteBuffer = contentMapper.apply(object);
        }
        return this;
    }

    @Override
    public MessageContextBuilder replyTo(final String replyToAddress) {
        if (replyToAddress != null) {
            messageContext.replyToChannel = replyToAddress;
        }
        return this;
    }

    @Override
    public MessageContextBuilder correlationId(final String correlationId) {
        if (correlationId != null) {
            messageContext.correlationId = correlationId;
        }
        return this;
    }

    @Override
    public MessageContextBuilder contentEncoding(final String contentEncoding) {
        if (contentEncoding != null) {
            messageContext.contentEncoding = contentEncoding;
        }
        return this;
    }

    @Override
    public MessageContextBuilder contentType(final String contentType) {
        if (contentType != null) {
            messageContext.contentType = contentType;
        }
        return this;
    }

    @Override
    public MessageContextBuilder channel(final String channelName, final String channelExtension) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public MessageContextBuilder channel(final String channelName) {
        if (channelName != null) {
            messageContext.channel = channelName;
        }
        return this;
    }

    @Override
    public MessageContextBuilder extensionEntry(final String key, final Object value) {
        if (key != null && value != null) {
            messageContext.extensions.put(key, value);
        }
        return this;
    }

    @Override
    public MessageContextBuilder extensions(final Map<String, Object> extension) {
        if (extension != null) {
            messageContext.extensions.putAll(extension);
        }
        return this;
    }

    // ACKNOWLEDGES

    @Override
    public AcknowledgeMessageContextBuilder handleAcknowledge(final Consumer<Message> acknowledgeHandler) {
        if (acknowledgeHandler != null) {
            messageContext.acknowledgeHandler = acknowledgeHandler;
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AcknowledgeMessageContextBuilder handleAcknowledge(final String acknowledgeHandlerTarget) {
        if (acknowledgeHandlerTarget != null) {
            messageContext.acknowledgeHandler = getAcknowledgeService(Consumer.class, acknowledgeHandlerTarget);
        }
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder filterAcknowledge(final Predicate<Message> acknowledgeFilter) {
        if (acknowledgeFilter != null) {
            messageContext.acknowledgeFilter = acknowledgeFilter;
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AcknowledgeMessageContextBuilder filterAcknowledge(final String acknowledgeFilterTarget) {
        if (acknowledgeFilterTarget != null) {
            messageContext.acknowledgeFilter = getAcknowledgeService(Predicate.class, acknowledgeFilterTarget);
        }
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder postAcknowledge(final Consumer<Message> acknowledgeConsumer) {
        if (acknowledgeConsumer != null) {
            messageContext.acknowledgeConsumer = acknowledgeConsumer;
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AcknowledgeMessageContextBuilder postAcknowledge(final String ackowledgeConsumerTarget) {
        if (ackowledgeConsumerTarget != null) {
            messageContext.acknowledgeConsumer = getAcknowledgeService(Consumer.class, ackowledgeConsumerTarget);
        }
        return this;
    }

    @Override
    public MessageContextBuilder messageContextBuilder() {
        return this;
    }

    private <T> T getAcknowledgeService(final Class<T> clazz, final String filter) {
        try {
            return MessagingHelper.getService(clazz, filter, bundleContext);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

}
