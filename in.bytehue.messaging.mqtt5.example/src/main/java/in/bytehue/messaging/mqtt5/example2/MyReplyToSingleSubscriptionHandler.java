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
package in.bytehue.messaging.mqtt5.example2;

import java.nio.ByteBuffer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.propertytypes.ReplyToSubscription;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;

// @formatter:off
@Component
@ReplyToSubscription(target =
    "(&(osgi.messaging.protocol=mqtt5)"
    + "(osgi.messaging.name=mqtt5-hivemq-adapter)"
    + "(osgi.messaging.feature=replyTo)))",
    channel = "channel_bar",
    replyChannel = "channel_foo")
// @formatter:on
public final class MyReplyToSingleSubscriptionHandler implements ReplyToSingleSubscriptionHandler {

    @Override
    public Message handleResponse(final Message requestMessage, final MessageContextBuilder responseBuilder) {
        return responseBuilder.content(ByteBuffer.wrap("Message from Handler".getBytes())).buildMessage();
    }

}
