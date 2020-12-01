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
package in.bytehue.messaging.mqtt5.provider.command;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.provider.command.MessagePubSubGogoCommand.PID;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;

import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.provider.helper.GogoCommand;

// @formatter:off
@GogoCommand(scope = "mqtt", function = { "pub", "sub" })
@Descriptor("Gogo commands to publish/subscribe to topics")
@Component(
        service = MessagePubSubGogoCommand.class,
        immediate = true,
        configurationPid = PID,
        configurationPolicy = REQUIRE)
public final class MessagePubSubGogoCommand {

    public static final String PID = "in.bytehue.messaging.mqtt.command";

    private static final String FILTER = "(" + MESSAGING_PROTOCOL_PROPERTY + "=" + MESSAGING_PROTOCOL + ")";

    @Reference(target = FILTER)
    private MessagePublisher publisher;

    @Reference(target = FILTER)
    private MessageSubscription subscriber;

    @Reference(target = FILTER)
    private ComponentServiceObjects<MqttMessageContextBuilder> mcbFactory;

    @Descriptor("Subscribes to specific topic/filter with the input context")
    public String sub(

            @Descriptor("Topic/Filter")
            @Parameter(names = { "-t", "-topic" }, absentValue = "")
            final String topic,

            @Descriptor("Quality of Service")
            @Parameter(names = { "-q", "-qos" }, absentValue = "0")
            final int qos,

            @Descriptor("Receive Local")
            @Parameter(names = { "-l", "-local" }, absentValue = "false")
            final boolean receiveLocal,

            @Descriptor("Retain as published")
            @Parameter(names = { "-r", "-retain" }, absentValue = "false")
            final boolean retainAsPublished) {

        if (topic.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }
        final MqttMessageContextBuilder mcb = mcbFactory.getService();
        try {
            final MessageContext context = mcb.channel(topic)
                                              .withQoS(qos)
                                              .withReceiveLocal(receiveLocal)
                                              .withRetain(retainAsPublished)
                                              .buildContext();
            subscriber.subscribe(context).forEach(m -> {
                System.out.println("Message Received");
                System.out.println(new String(m.payload().array(), StandardCharsets.UTF_8));
            });
            return "Subscribed to " + topic;
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            mcbFactory.ungetService(mcb);
        }
        return "Subscribed to "+ topic;
    }

    @Descriptor("Publishes to specific topic/filter with the input context")
    public String pub(

            @Descriptor("Topic/Filter")
            @Parameter(names = { "-t", "-topic" }, absentValue = "")
            final String topic,

            @Descriptor("Quality of Service")
            @Parameter(names = { "-q", "-qos" }, absentValue = "0")
            final int qos,

            @Descriptor("Receive Local")
            @Parameter(names = { "-l", "-local" }, absentValue = "false")
            final boolean receiveLocal,

            @Descriptor("Retain")
            @Parameter(names = { "-r", "-retain" }, absentValue = "false")
            final boolean retain,

            @Descriptor("Content Type")
            @Parameter(names = { "-ct", "-contentType" }, absentValue = "text/plain")
            final String contentType,

            @Descriptor("Content")
            @Parameter(names = { "-c", "-content" }, absentValue = "")
            final String content,

            @Descriptor("Message Expiry Interval")
            @Parameter(names = { "-e", "-expiry" }, absentValue = "0")
            final long messageExpiryInterval) {

        if (topic.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }
        final MqttMessageContextBuilder mcb = mcbFactory.getService();
        try {
            final Message message = mcb.channel(topic)
                                       .withQoS(qos)
                                       .withRetain(retain)
                                       .contentType(contentType)
                                       .content(ByteBuffer.wrap(content.getBytes()))
                                       .withMessageExpiryInterval(messageExpiryInterval)
                                       .buildMessage();
            publisher.publish(message);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            mcbFactory.ungetService(mcb);
        }
        return "Published to " + topic;
    }

}
