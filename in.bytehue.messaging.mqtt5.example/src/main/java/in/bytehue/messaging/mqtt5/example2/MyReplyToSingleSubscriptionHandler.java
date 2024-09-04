/*******************************************************************************
 * Copyright 2020-2024 Amit Kumar Mondal
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
package in.bytehue.messaging.mqtt5.example2;

import java.nio.ByteBuffer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.propertytypes.ReplyToSubscription;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;

/**
 * An OSGi component that handles single reply-to subscriptions for MQTT5 protocol messages.
 * It listens on the specified channels and sends responses using the defined configuration.
 */
@Component
@ReplyToSubscription(
    target = "(&(osgi.messaging.protocol=mqtt5)" +
             "(osgi.messaging.name=mqtt5-hivemq-adapter)" +
             "(osgi.messaging.feature=replyTo))",
    channel = "channel_bar",
    replyChannel = "channel_foo"
)
public final class MyReplyToSingleSubscriptionHandler implements ReplyToSingleSubscriptionHandler {

    /**
     * Handles incoming messages and constructs a response message.
     *
     * @param requestMessage  The received message that needs a reply.
     * @param responseBuilder A builder for creating the response message context.
     * @return The constructed response message.
     */
    @Override
    public Message handleResponse(final Message requestMessage, final MessageContextBuilder responseBuilder) {
        // Construct and return a response message with the specified content.
        return responseBuilder
                .content(ByteBuffer.wrap("Message from Handler".getBytes()))
                .buildMessage();
    }
}