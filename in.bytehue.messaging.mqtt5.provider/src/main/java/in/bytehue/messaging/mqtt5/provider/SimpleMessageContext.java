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
    public AcknowledgeHandler protocolSpecificAcknowledgeHandler;

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
        return protocolSpecificAcknowledgeHandler;
    }

    @Override
    public String toString() {
        return "MessageContext [channel=" + channel + ", contentType=" + contentType + ", contentEncoding="
                + contentEncoding + ", correlationId=" + correlationId + ", replyToChannel=" + replyToChannel
                + ", extensions=" + extensions + "]";
    }

}
