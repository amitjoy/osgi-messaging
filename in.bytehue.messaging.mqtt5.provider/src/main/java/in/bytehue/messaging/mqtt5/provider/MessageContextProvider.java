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

import static org.osgi.service.messaging.acknowledge.AcknowledgeType.ACKNOWLEDGED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.REJECTED;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext;
import org.osgi.service.messaging.acknowledge.AcknowledgeType;

import in.bytehue.messaging.mqtt5.provider.helper.AcknowledgePair;

public final class MessageContextProvider implements MessageContext, AcknowledgeMessageContext {

    public String channel;
    public String contentType;
    public String contentEncoding;
    public String correlationId;
    public String replyToChannel;

    public volatile AcknowledgeType acknowledgeState;
    public Map<String, Object> extensions = new HashMap<>();

    public final AcknowledgePair<Predicate<Message>> acknowledgeFilter = AcknowledgePair.emptyOf(Predicate.class);
    public final AcknowledgePair<Consumer<Message>> acknowledgeHandler = AcknowledgePair.emptyOf(Consumer.class);
    public final AcknowledgePair<Consumer<Message>> acknowledgeConsumer = AcknowledgePair.emptyOf(Consumer.class);

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
        return new AcknowledgeHandler() {

            @Override
            public synchronized boolean reject() {
                if (acknowledgeState == ACKNOWLEDGED) {
                    return false;
                }
                acknowledgeState = REJECTED;
                return true;
            }

            @Override
            public synchronized boolean acknowledge() {
                if (acknowledgeState == REJECTED) {
                    return false;
                }
                acknowledgeState = ACKNOWLEDGED;
                return true;
            }
        };
    }

    // @formatter:off
    @Override
    public String toString() {
        return new StringBuilder().append("MessageContextProvider [channel=")
                                  .append(channel)
                                  .append(", contentType=")
                                  .append(contentType)
                                  .append(", contentEncoding=")
                                  .append(contentEncoding)
                                  .append(", correlationId=")
                                  .append(correlationId)
                                  .append(", replyToChannel=")
                                  .append(replyToChannel)
                                  .append(", acknowledgeState=")
                                  .append(acknowledgeState)
                                  .append(", extensions=")
                                  .append(extensions)
                                  .append("]")
                                  .toString();
    }

}
