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
import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;
import static org.osgi.service.messaging.Features.ACKNOWLEDGE;
import static org.osgi.service.messaging.Features.MESSAGE_CONTEXT_BUILDER;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;

// @formatter:off
@Component(
        scope = PROTOTYPE,
        service = {
                    MessageContextBuilder.class,
                    MqttMessageContextBuilder.class,
                    MessageContextBuilderProvider.class,
                    AcknowledgeMessageContextBuilder.class
                  }
)
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                    MESSAGE_CONTEXT_BUILDER,
                    ACKNOWLEDGE
                  }
)
@ProvideMessagingAcknowledgeFeature
public final class MessageContextBuilderProvider
        implements
            MqttMessageContextBuilder,
            MessageContextBuilder,
            AcknowledgeMessageContextBuilder {

    private final Logger logger;
    private final MessageProvider message;
    private final MessageContextProvider messageContext;

    @Activate
    public MessageContextBuilderProvider(
            @Reference(service = LoggerFactory.class)
            final Logger logger) {

        this.logger = logger;

        message = new MessageProvider();
        messageContext = new MessageContextProvider();
        message.messageContext = messageContext;
    }
    //@formatter:on

    @Override
    public MessageContext buildContext() {
        return messageContext;
    }

    @Override
    public Message buildMessage() {
        return message;
    }

    @Override
    public MqttMessageContextBuilder withContext(final MessageContext context) {
        if (context instanceof MessageContextProvider) {
            message.messageContext = context;
        }
        return this;
    }

    @Override
    public MqttMessageContextBuilder content(final ByteBuffer byteBuffer) {
        message.byteBuffer = byteBuffer;
        return this;
    }

    @Override
    public <T> MqttMessageContextBuilder content(final T object, final Function<T, ByteBuffer> contentMapper) {
        message.byteBuffer = Optional.ofNullable(contentMapper).map(c -> c.apply(object)).orElse(null);
        return this;
    }

    @Override
    public MqttMessageContextBuilder replyTo(final String replyToAddress) {
        messageContext.replyToChannel = replyToAddress;
        return this;
    }

    @Override
    public MqttMessageContextBuilder correlationId(final String correlationId) {
        messageContext.correlationId = correlationId;
        return this;
    }

    @Override
    public MqttMessageContextBuilder correlationIdGenerator(final String filter) {
        messageContext.correlationIdGenerator = filter;
        return this;
    }

    @Override
    public MqttMessageContextBuilder contentEncoding(final String contentEncoding) {
        messageContext.contentEncoding = contentEncoding;
        return this;
    }

    @Override
    public MqttMessageContextBuilder contentType(final String contentType) {
        messageContext.contentType = contentType;
        return this;
    }

    @Override
    public MqttMessageContextBuilder channel(final String channelName, final String channelExtension) {
        // routing key ('channelExtension') is not required by MQTT
        channel(channelName);
        logger.debug("Channel extension will be ignored");
        return this;
    }

    @Override
    public MqttMessageContextBuilder channel(final String channelName) {
        messageContext.channel = channelName;
        return this;
    }

    @Override
    public MqttMessageContextBuilder extensionEntry(final String key, final Object value) {
        if (key != null && value != null) {
            messageContext.extensions.put(key, value);
        }
        return this;
    }

    @Override
    public MqttMessageContextBuilder extensions(final Map<String, Object> extensions) {
        if (extensions != null) {
            messageContext.extensions.putAll(extensions);
        }
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder handleAcknowledge(final Consumer<Message> acknowledgeHandler) {
        messageContext.acknowledgeHandler.setConcrete(acknowledgeHandler);
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder handleAcknowledge(final String acknowledgeHandlerTarget) {
        messageContext.acknowledgeHandler.setServiceFilter(acknowledgeHandlerTarget);
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder filterAcknowledge(final Predicate<Message> acknowledgeFilter) {
        messageContext.acknowledgeFilter.setConcrete(acknowledgeFilter);
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder filterAcknowledge(final String acknowledgeFilterTarget) {
        messageContext.acknowledgeFilter.setServiceFilter(acknowledgeFilterTarget);
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder postAcknowledge(final Consumer<Message> acknowledgeConsumer) {
        messageContext.acknowledgeConsumer.setConcrete(acknowledgeConsumer);
        return this;
    }

    @Override
    public AcknowledgeMessageContextBuilder postAcknowledge(final String ackowledgeConsumerTarget) {
        messageContext.acknowledgeConsumer.setServiceFilter(ackowledgeConsumerTarget);
        return this;
    }

    @Override
    public MessageContextBuilder messageContextBuilder() {
        return this;
    }

}
