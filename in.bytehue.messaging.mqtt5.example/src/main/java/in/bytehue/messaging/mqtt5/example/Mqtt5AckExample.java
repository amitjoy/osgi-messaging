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
package in.bytehue.messaging.mqtt5.example;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder;
import org.osgi.util.pushstream.PushStream;

// @formatter:off
@Component(service = Mqtt5AckExample.class, immediate = true)
public final class Mqtt5AckExample {

    @Reference(target = "(osgi.messaging.feature=acknowledge)")
    private MessageSubscription mqttSubscription;

    @Reference
    private ComponentServiceObjects<AcknowledgeMessageContextBuilder> amcbFactory;

    @Reference
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    volatile boolean good = true;

    public void subscribeMessage1() {
        final AcknowledgeMessageContextBuilder ackBuilder = amcbFactory.getService();
        final MessageContext context = ackBuilder
                .filterAcknowledge(m -> m.getContext().getContentType().equals("plain/text"))
                .messageContextBuilder()
                .channel("/demo").buildContext();
        mqttSubscription.subscribe(context);
    }

    public void subscribeMessage2() {
        final AcknowledgeMessageContextBuilder ackBuilder = amcbFactory.getService();
        final MessageContext context = ackBuilder.handleAcknowledge(m -> {
            final AcknowledgeMessageContext ctx = (AcknowledgeMessageContext) m.getContext();
            final AcknowledgeHandler handler = ctx.getAcknowledgeHandler();
            if (good) {
                handler.acknowledge();
            } else {
                handler.reject();
            }
        }).postAcknowledge(m -> {
            final AcknowledgeMessageContext ctx = (AcknowledgeMessageContext) m.getContext();
            System.out.println("Acknowledge state is: " + ctx.getAcknowledgeState());
        }).messageContextBuilder()
          .channel("/demo")
          .buildContext();
        mqttSubscription.subscribe(context);
    }

    public void subscribeMessage3() {
        final AcknowledgeMessageContextBuilder ackBuilder = amcbFactory.getService();
        final MessageContext context = ackBuilder
                .handleAcknowledge("(foo=bar)")
                .postAcknowledge(m -> {
                        final AcknowledgeMessageContext ctx = (AcknowledgeMessageContext) m.getContext();
                        System.out.println("Acknowledge state is: " + ctx.getAcknowledgeState());
                        })
                .messageContextBuilder()
                .channel("/demo")
                .buildContext();
        mqttSubscription.subscribe(context);
    }

    public void subscribeMessage4() {
        final MessageContext context = mcbFactory.getService().channel("/demo").buildContext();
        final PushStream<Message> messageStream = mqttSubscription.subscribe(context);
        messageStream.forEach(m -> {
            final AcknowledgeMessageContext ctx = (AcknowledgeMessageContext) m.getContext();
            final AcknowledgeHandler handler = ctx.getAcknowledgeHandler();
            if (good) {
                handler.acknowledge();
            } else {
                handler.reject();
            }
        });
    }

}
