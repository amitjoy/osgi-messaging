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

import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.MQTT_PROTOCOL;
import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.MESSAGE_CONTEXT_BUILDER;

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

import in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants;
import in.bytehue.messaging.mqtt5.provider.helper.MessageHelper;

// @formatter:off
@Component(
        scope = PROTOTYPE,
        service = {
                MessageContextBuilder.class,
                SimpleMessageContextBuilder.class,
                AcknowledgeMessageContextBuilder.class
        })
@ProvideMessagingAcknowledgeFeature
@MessagingFeature(
        protocol = MQTT_PROTOCOL,
        name = Mqtt5MessageConstants.Component.MESSAGE_CONTEXT_BUILDER,
        feature = {
                MESSAGE_CONTEXT_BUILDER,
                ACKNOWLEDGE })
// @formatter:on
public final class SimpleMessageContextBuilder implements MessageContextBuilder, AcknowledgeMessageContextBuilder {

    private final Logger logger;
    private final SimpleMessage message;
    private final BundleContext bundleContext;
    private final SimpleMessageContext messageContext;

    @Activate
    public SimpleMessageContextBuilder( //
            final BundleContext bundleContext, //
            @Reference final SimpleMessageAcknowledgeHandler acknowledgeHandler, //
            @Reference(service = LoggerFactory.class) final Logger logger) {
        this.logger = logger;
        this.bundleContext = bundleContext;
        message = new SimpleMessage();
        messageContext = new SimpleMessageContext();

        message.messageContext = messageContext;
        messageContext.protocolSpecificAcknowledgeHandler = acknowledgeHandler;
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
            message.messageContext = context;
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
        // routing key ('channelExtension') is not required by MQTT
        channel(channelName);
        logger.debug("Channel extension will be ignored");
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
            messageContext.acknowledgeHandler = getAcknowledgeHandler(Consumer.class, acknowledgeHandlerTarget);
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
            messageContext.acknowledgeFilter = getAcknowledgeHandler(Predicate.class, acknowledgeFilterTarget);
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
            messageContext.acknowledgeConsumer = getAcknowledgeHandler(Consumer.class, ackowledgeConsumerTarget);
        }
        return this;
    }

    @Override
    public MessageContextBuilder messageContextBuilder() {
        return this;
    }

    private <T> T getAcknowledgeHandler(final Class<T> clazz, final String filter) {
        try {
            return MessageHelper.getService(clazz, filter, bundleContext);
        } catch (final Exception e) {
            logger.error("Acknowledge Handler not found in the service registry", e);
        }
        return null;
    }

}
