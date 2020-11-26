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

import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.asString;

import java.nio.ByteBuffer;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;

public final class SimpleMessage implements Message {

    // TODO add gogo command to pub/sub

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
