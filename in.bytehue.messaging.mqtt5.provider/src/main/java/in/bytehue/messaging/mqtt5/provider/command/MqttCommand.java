/*******************************************************************************
 * Copyright 2020-2023 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.provider.command.MqttCommand.PID;
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
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.provider.MessageClientProvider;
import in.bytehue.messaging.mqtt5.provider.MessageClientProvider.Config;
import in.bytehue.messaging.mqtt5.provider.MessagePublisherProvider;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionProvider;
import in.bytehue.messaging.mqtt5.provider.helper.FelixGogoCommand;
import in.bytehue.messaging.mqtt5.provider.helper.Table;

// @formatter:off
@Descriptor("MQTT 5 Messaging")
@FelixGogoCommand(scope = "mqtt", function = { "pub", "sub", "runtime" })
@Component(
        immediate = true,
        configurationPid = PID,
        configurationPolicy = REQUIRE,
        service = MqttCommand.class)
public final class MqttCommand {

    public static final String PID = "in.bytehue.messaging.mqtt.command";

    @Reference
    private MessageClientProvider client;

    @Reference
    private MessageServiceRuntime runtime;

    @Reference
    private MessagePublisherProvider publisher;

    @Reference
    private MessageSubscriptionProvider subscriber;

    @Reference
    private ComponentServiceObjects<MqttMessageContextBuilder> mcbFactory;

    @Descriptor("Returns the current runtime information of the MQTT client")
    public String runtime(
            @Descriptor("Shows full MQTT configuration ")
            @Parameter(absentValue = "false", presentValue = "true", names = { "-showconfig" })
            final boolean showconfig) {

        final Converter converter = Converters.standardConverter();
        final MessagingRuntimeDTO runtimeInfo = runtime.getRuntimeDTO();

        final StringBuilder output = new StringBuilder();

        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Name", "Value");

        table.addRow("Connection URI", runtimeInfo.connectionURI);
        table.addRow("Connection Port", String.valueOf(client.config().port()));
        table.addRow("Connection SSL",  String.valueOf(client.config().useSSL()));
        table.addRow("Connection State", client.client.getState().toString());
        table.addRow("Provider", runtimeInfo.providerName);
        table.addRow("Supported Protocols", converter.convert(runtimeInfo.protocols).to(String.class));
        table.addRow("Instance ID", runtimeInfo.instanceId);

        final String subscriptions = prepareSubscriptions(runtimeInfo.subscriptions);
        final String replyToSubscriptions = prepareReplyToSubscriptions(runtimeInfo.replyToSubscriptions);


        output.append(table.print())
              .append(System.lineSeparator())
              .append(System.lineSeparator())
              .append("Subscriptions: ")
              .append(System.lineSeparator())
              .append(subscriptions)
              .append(System.lineSeparator())
              .append(System.lineSeparator())
              .append("ReplyTo Subscriptions: ")
              .append(System.lineSeparator())
              .append(replyToSubscriptions);

        if (showconfig) {
            output.append(System.lineSeparator())
                  .append(System.lineSeparator())
                  .append("Configuration: ")
                  .append(System.lineSeparator())
                  .append(prepareConfig(client.config(), converter));
        }
        return output.toString();
    }

    @Descriptor("Subscribes to specific topic/filter with the input context")
    public String sub(

            @Descriptor("Topic/Filter")
            @Parameter(names = { "-t", "--topic" }, absentValue = "foo/bar")
            final String topic,

            @Descriptor("Quality of Service (QoS)")
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
            final Table table = new Table();

            table.setShowVerticalLines(true);
            table.setHeaders("Configuration", "Value");

            table.addRow("Channel", topic);
            table.addRow("QoS", String.valueOf(qos));
            table.addRow("Receive Local", String.valueOf(receiveLocal));
            table.addRow("Retain as Published", String.valueOf(retainAsPublished));

            System.out.println(table.print());

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
        return "Subscribed to " + topic;
    }

    @Descriptor("Publishes to specific topic/filter with the input context")
    public String pub(

            @Descriptor("Topic/Filter")
            @Parameter(names = { "-t", "--topic" }, absentValue = "foo/bar")
            final String topic,

            @Descriptor("Quality of Service (QoS)")
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

    private String prepareSubscriptions(final SubscriptionDTO[] subscriptions) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Channel Name");

        for (final SubscriptionDTO subscription : subscriptions) {
            final ChannelDTO channel = subscription.channel;
            table.addRow(channel.name);
        }
        return table.print();
    }

    private String prepareReplyToSubscriptions(final ReplyToSubscriptionDTO[] replyToSubscriptions) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Request Channel", "Response Channel");

        for (final ReplyToSubscriptionDTO subscription : replyToSubscriptions) {
            final ChannelDTO reqChannel = subscription.requestChannel;
            final ChannelDTO resChannel = subscription.responseChannel;

            table.addRow(reqChannel.name, resChannel.name);
        }
        return table.print();
    }

    private String prepareConfig(final Config config, final Converter converter) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Name", "Value");

        table.addRow("Client ID", config.id());
        table.addRow("Automatic Reconnect", String.valueOf(config.automaticReconnect()));
        table.addRow("Clean Start", String.valueOf(config.cleanStart()));
        table.addRow("Initial Delay", String.valueOf(config.initialDelay()));
        table.addRow("Max Delay", String.valueOf(config.maxDelay()));
        table.addRow("Keep Interval", String.valueOf(config.keepAliveInterval()));
        table.addRow("Session Expiry Interval", String.valueOf(config.sessionExpiryInterval()));
        table.addRow("Simple Authentication", String.valueOf(config.simpleAuth()));
        table.addRow("Use static credentials specified in username and password configurations", String.valueOf(config.staticAuthCred()));
        table.addRow("Simple Authentication Credential Filter", String.valueOf(config.simpleAuthCredFilter()));
        table.addRow("SSL Configuration Cipher Suites", converter.convert(config.cipherSuites()).to(String.class));
        table.addRow("SSL Configuration Protocols", converter.convert(config.protocols()).to(String.class));
        table.addRow("SSL Configuration Handshake Timeout", String.valueOf(config.sslHandshakeTimeout()));
        table.addRow("SSL Configuration Key Manager Factory Service Target Filter", config.keyManagerFactoryTargetFilter());
        table.addRow("SSL Configuration Trust Manager Factory Service Target Filter", config.trustManagerFactoryTargetFilter());
        table.addRow("SSL Configuration Hostname Verifier Service Target Filter", config.hostNameVerifierTargetFilter());
        table.addRow("Last Will Topic", config.lastWillTopic());
        table.addRow("Last Will QoS", String.valueOf(config.lastWillQoS()));
        table.addRow("Last Will Payload", config.lastWillPayLoad());
        table.addRow("Last Will Content Type", config.lastWillContentType());
        table.addRow("Last Will Message Expiry Interval", String.valueOf(config.lastWillMessageExpiryInterval()));
        table.addRow("Last Will Delay Interval",  String.valueOf(config.lastWillDelayInterval()));
        table.addRow("Maximum Concurrent Messages to be received", String.valueOf(config.receiveMaximum()));
        table.addRow("Maximum Concurrent Messages to be sent", String.valueOf(config.sendMaximum()));
        table.addRow("Maximum Packet Size for receiving", String.valueOf(config.maximumPacketSize()));
        table.addRow("Maximum Packet Size for sending", String.valueOf(config.sendMaximumPacketSize()));
        table.addRow("Maximum Topic Aliases", String.valueOf(config.topicAliasMaximum()));
        table.addRow("MQTT over Web Socket", String.valueOf(config.useWebSocket()));
        table.addRow("Web Socket Query String", config.queryString());
        table.addRow("Web Socket Server Path", config.serverPath());
        table.addRow("Web Socket Sub Protocol", config.subProtocol());
        table.addRow("Web Socket Handshake Timeout", String.valueOf(config.webSocketHandshakeTimeout()));
        table.addRow("Enhanced Authentication", String.valueOf(config.useEnhancedAuthentication()));
        table.addRow("Enhanced Authentication Service Filter", config.enhancedAuthTargetFilter());
        table.addRow("Server Reauthentication", String.valueOf(config.useServerReauth()));
        table.addRow("Connected Listener Service Filter", config.connectedListenerFilter());
        table.addRow("Disconnected Listener Service Filter", config.disconnectedListenerFilter());
        table.addRow("QoS 1 Incoming Interceptor Service Filter", config.qos1IncomingInterceptorFilter());
        table.addRow("QoS 2 Incoming Interceptor Service Filter", config.qos2IncomingInterceptorFilter());
        table.addRow("QoS 1 Outgoing Interceptor Service Filter", config.qos1OutgoingInterceptorFilter());
        table.addRow("QoS 2 Outgoing Interceptor Service Filter", config.qos2OutgoingInterceptorFilter());
        table.addRow("Filter to be satisfied for the client to be active", config.osgi_ds_satisfying_condition_target());
        table.addRow("Reason for the disconnection when the client component is stopped", config.disconnectionReasonDescription());
        table.addRow("Code for the disconnection when the client component is stopped", config.disconnectionReasonCode().name());

        return table.print();

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
