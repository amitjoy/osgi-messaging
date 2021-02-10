/*******************************************************************************
 * Copyright 2021 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.provider.command.MessagePubSubGogoCommand.PID;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.stackTraceToString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;

import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.provider.MessagePublisherProvider;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionProvider;
import in.bytehue.messaging.mqtt5.provider.helper.GogoCommand;
import in.bytehue.messaging.mqtt5.provider.helper.Table;

// @formatter:off
@GogoCommand(scope = "mqtt", function = { "pub", "sub" })
@Descriptor("Gogo commands to publish/subscribe to topics")
@Component(
        immediate = true,
        configurationPid = PID,
        configurationPolicy = REQUIRE,
        service = MessagePubSubGogoCommand.class)
public final class MessagePubSubGogoCommand {

    public static final String PID = "in.bytehue.messaging.mqtt.command";

    @Reference
    private MessagePublisherProvider publisher;

    @Reference
    private MessageSubscriptionProvider subscriber;

    @Reference
    private ComponentServiceObjects<MqttMessageContextBuilder> mcbFactory;

    @Descriptor("Subscribes to specific topic/filter with the input context")
    public String sub(

            @Descriptor("Topic/Filter")
            @Parameter(names = { "-t", "--topic" }, absentValue = "foo/bar")
            final String topic,

            @Descriptor("Quality of Service")
            @Parameter(names = { "-q", "--qos" }, absentValue = "0")
            final int qos,

            @Descriptor("Receive Local")
            @Parameter(names = { "-l", "--local" }, absentValue = "false")
            final boolean receiveLocal,

            @Descriptor("Retain as Published")
            @Parameter(names = { "-r", "--retain" }, absentValue = "false")
            final boolean retainAsPublished) {

        final MqttMessageContextBuilder mcb = mcbFactory.getService();
        try {
            // display the configuration
            final Table st = new Table();
            st.setShowVerticalLines(true);
            st.setHeaders("Configuration", "Value");
            st.addRow("Channel", topic);
            st.addRow("QoS", String.valueOf(qos));
            st.addRow("Receive Local", String.valueOf(receiveLocal));
            st.addRow("Retain as Published", String.valueOf(retainAsPublished));
            st.print();

            final MessageContext context = mcb.channel(topic)
                                              .withQoS(qos)
                                              .withReceiveLocal(receiveLocal)
                                              .withRetain(retainAsPublished)
                                              .buildContext();
            subscriber.subscribe(context).forEach(m -> {
                System.out.println("Message Received");
                System.out.println(new String(m.payload().array(), UTF_8));
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
            @Parameter(names = { "-t", "--topic" }, absentValue = "foo/bar")
            final String topic,

            @Descriptor("Quality of Service")
            @Parameter(names = { "-q", "--qos" }, absentValue = "0")
            final int qos,

            @Descriptor("Receive Local")
            @Parameter(names = { "-l", "--local" }, absentValue = "false")
            final boolean receiveLocal,

            @Descriptor("Retain")
            @Parameter(names = { "-r", "--retain" }, absentValue = "false")
            final boolean retain,

            @Descriptor("Content Type")
            @Parameter(names = { "-ct", "--contentType" }, absentValue = "text/plain")
            final String contentType,

            @Descriptor("Content")
            @Parameter(names = { "-c", "--content" }, absentValue = "foobar")
            final String content,

            @Descriptor("Message Expiry Interval")
            @Parameter(names = { "-e", "--expiry" }, absentValue = "0")
            final long messageExpiryInterval,

            @Descriptor("User Properties, such as 'A=B#C=D#E=F'")
            @Parameter(names = { "-u", "--userProperties" }, absentValue = "")
            final String userProperties) {

        final MqttMessageContextBuilder mcb = mcbFactory.getService();
        try {
            final Map<String, String> properties = initUserProperties(userProperties);

            // display the configuration
            final Table st = new Table();
            st.setShowVerticalLines(true);
            st.setHeaders("Configuration", "Value");
            st.addRow("Channel", topic);
            st.addRow("QoS", String.valueOf(qos));
            st.addRow("Retain", String.valueOf(retain));
            st.addRow("Content Type", contentType);
            st.addRow("Content", content);
            st.addRow("Message Expiry Interval", String.valueOf(messageExpiryInterval));
            st.addRow("User Properties", String.valueOf(properties));
            st.print();

            final Message message = mcb.channel(topic)
                                       .withQoS(qos)
                                       .withRetain(retain)
                                       .contentType(contentType)
                                       .content(ByteBuffer.wrap(content.getBytes()))
                                       .withMessageExpiryInterval(messageExpiryInterval)
                                       .withUserProperties(properties)
                                       .buildMessage();
            publisher.publish(message);
        } catch (final Exception e) {
            return stackTraceToString(e);
        } finally {
            mcbFactory.ungetService(mcb);
        }
        return "Published to " + topic;
    }

    private Map<String, String> initUserProperties(final String userProperties) {
        final Map<String, String> map = new HashMap<>();
        if (userProperties.indexOf('#') == -1) {
            return map;
        }
        for (final String pair : userProperties.split("#")) {
            final String[] kv = pair.split("=");
            map.put(kv[0], kv[1]);
        }
        return map;
    }

}
