/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.provider.command;

import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.stackTraceToString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.condition.Condition.CONDITION_ID;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.dto.ChannelDTO;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.dto.ReplyToSubscriptionDTO;
import org.osgi.service.messaging.dto.SubscriptionDTO;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import com.hivemq.client.mqtt.MqttClientState;

import in.bytehue.messaging.mqtt5.api.MqttCommandExtension;
import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.provider.LogMirrorService;
import in.bytehue.messaging.mqtt5.provider.MessageClientProvider;
import in.bytehue.messaging.mqtt5.provider.MessageClientProvider.Config;
import in.bytehue.messaging.mqtt5.provider.MessagePublisherProvider;
import in.bytehue.messaging.mqtt5.provider.MessagePublisherProvider.PublisherConfig;
import in.bytehue.messaging.mqtt5.provider.MessageReplyToPublisherProvider;
import in.bytehue.messaging.mqtt5.provider.MessageReplyToPublisherProvider.ReplyToConfig;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionProvider;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionProvider.SubscriberConfig;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionRegistry;
import in.bytehue.messaging.mqtt5.provider.helper.FelixGogoCommand;
import in.bytehue.messaging.mqtt5.provider.helper.Table;

// @formatter:off
@Descriptor("MQTT 5 Messaging")
@Component(immediate = true, service = MqttCommand.class)
@SatisfyingConditionTarget("(" + CONDITION_ID +"=gogo-available)")
@FelixGogoCommand(scope = "mqtt", function = { "pub", "sub", "unsub", "runtime", "mirror", "connect", "disconnect" })
public final class MqttCommand {

	@Reference
	private LogMirrorService logMirror;

	@Reference
    private MessageClientProvider client;

    @Reference
    private MessageServiceRuntime runtime;

    @Reference
    private MessagePublisherProvider publisher;

    @Reference
    private MessageSubscriptionRegistry registry;

    @Reference
    private MessageSubscriptionProvider subscriber;

    @Reference
    private MessageReplyToPublisherProvider replyToPublisher;

    @Reference
    private ComponentServiceObjects<MqttMessageContextBuilder> mcbFactory;

    @Reference
    private volatile Collection<MqttCommandExtension> extensions;

    @Descriptor("Returns the current runtime information of the MQTT client")
    public String runtime() {
        return _runtime(null);
    }

    @Descriptor("Displays specific configuration details for the MQTT client")
    public String runtime(
            @Descriptor("The subcommand, must be 'config'")
            final String command,
            @Descriptor("The type of configuration to display: client, pub, sub, or replytopub")
            final String type) {
        if (!"config".equals(command)) {
            return "Usage: mqtt:runtime config <type>";
        }
        return _runtime(type);
    }

    private String _runtime(final String configType) {
        final Converter converter = Converters.standardConverter();
        final MessagingRuntimeDTO runtimeInfo = runtime.getRuntimeDTO();
        final StringBuilder output = new StringBuilder();
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Name", "Value");

        table.addRow("Connection URI", runtimeInfo.connectionURI);
        table.addRow("Connection Port", String.valueOf(client.config().port()));
        table.addRow("Connection SSL", String.valueOf(client.config().useSSL()));
        table.addRow("Connection State", getState());

        final long uptimeMillis = client.getConnectedTimestamp() > 0 ? System.currentTimeMillis() - client.getConnectedTimestamp() : 0;
        table.addRow("Connection Uptime", formatUptime(uptimeMillis));

        if (client.getConnectedTimestamp() == -1) {
            table.addRow("Last Disconnect Reason", client.getLastDisconnectReason());
        }

        table.addRow("Provider", runtimeInfo.providerName);
        table.addRow("Supported Protocols", converter.convert(runtimeInfo.protocols).to(String.class));
        table.addRow("Instance ID", runtimeInfo.instanceId);

        if (extensions != null) {
            extensions.forEach(p -> {
                final String rowName = p.rowName();
                final String rowValue = p.rowValue();

                requireNonNull(rowName, "Row name cannot be null");
                table.addRow(rowName, rowValue == null ? "NULL" : rowValue);
            });
        }

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

        if (configType != null) {
            output.append(System.lineSeparator()).append(System.lineSeparator());
            switch (configType) {
            case "client":
                output.append("Client Configuration: ").append(System.lineSeparator())
                        .append(prepareClientConfig(client.config(), converter));
                break;
            case "pub":
                output.append("Publisher Configuration: ").append(System.lineSeparator())
                        .append(preparePublisherConfig(publisher.config()));
                break;
            case "sub":
                output.append("Subscriber Configuration: ").append(System.lineSeparator())
                        .append(prepareSubscriberConfig(subscriber.config()));
                break;
            case "replytopub":
                output.append("Reply-To Publisher Configuration: ").append(System.lineSeparator())
                        .append(prepareReplyToPublisherConfig(replyToPublisher.config()));
                break;
            default:
                return "Error: Unknown configuration type '" + configType + "'.\n"
                        + "Available types: client, pub, sub, replytopub";
            }
        }
        return output.toString();
    }

    private String getState() {
		MqttClientState state = client.client.getState();
		return state.isConnected() ? "CONNECTED" : "DISCONNECTED";
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
                System.out.println("\n--- Message Received ---");
                System.out.println("Topic: " + m.getContext().getChannel());

                final ByteBuffer payload = m.payload();
                final byte[] bytes = new byte[payload.remaining()];
                payload.duplicate().get(bytes);

                System.out.println("Payload: " + new String(bytes, UTF_8));
                System.out.println("------------------------");
            });
        } catch (final Exception e) {
            return stackTraceToString(e);
        } finally {
            mcbFactory.ungetService(mcb);
        }
        return "Subscribed to '" + topic + "'.\n" + "Note: If you are subscribing to a topic for temporary debugging, don't forget to unsubscribe using the 'unsub' command.";
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

    @Descriptor("Unsubscribes from a specific topic that was subscribed to via the 'sub' command")
    public String unsub(
            @Descriptor("Topic/Filter to unsubscribe from")
            @Parameter(names = { "-t", "--topic" }, absentValue = "")
            final String topic) {

        if (topic.isEmpty()) {
            return "Error: Topic must be specified using -t or --topic.";
        }

        try {
        	// Just remove the local subscription.
            // The stream's onClose handler will manage the broker unsubscribe.
        	registry.removeSubscription(topic);
            return "Successfully unsubscribed from topic: " + topic;
        } catch (final Exception e) {
            return "An error occurred while unsubscribing: " + stackTraceToString(e);
        }
    }

    @Descriptor("Shows the status of the log mirror or start/stop the log mirror")
    public String mirror(
            @Descriptor("Supported values - [status, on, off]")
            @Parameter(absentValue = "STATUS", names = { "-i" }) final String extra) {
    	if ("STATUS".equalsIgnoreCase(extra)) {
            return logMirror.isMirrorEnabled() ? "Log Mirror ENABLED" : "Log Mirror DISABLED";
        } else if ("OFF".equalsIgnoreCase(extra)) {
            logMirror.disableMirror();
            return "Log Mirror DISABLED";
        } else if ("ON".equalsIgnoreCase(extra)) {
        	logMirror.enableMirror();
            return "Log Mirror ENABLED";
        } else {
        	return "Invalid Parameter";
        }
    }

    @Descriptor("Connects to the MQTT broker")
    public String connect(
            @Descriptor("Username for authentication")
            @Parameter(names = { "-u", "--username" }, absentValue = "")
            final String username,

            @Descriptor("Password for authentication")
            @Parameter(names = { "-p", "--password" }, absentValue = "")
            final String password) {

        try {
            if (client.isConnected()) {
                return "Already connected to the MQTT broker";
            }
            final Table table = new Table();
            table.setShowVerticalLines(true);
            table.setHeaders("Configuration", "Value");
            table.addRow("Server", client.config().server());
            table.addRow("Port", String.valueOf(client.config().port()));
            table.addRow("SSL", String.valueOf(client.config().useSSL()));
            
            if (!username.isEmpty() && !password.isEmpty()) {
                table.addRow("Authentication", "Using provided credentials");
                System.out.println(table.print());
                client.connect(username, password.getBytes());
            } else {
                table.addRow("Authentication", "Using configured credentials");
                System.out.println(table.print());
                client.connect();
            }
            return "Connection initiated to the MQTT broker";
        } catch (final Exception e) {
            return "Failed to connect to the MQTT broker: " + stackTraceToString(e);
        }
    }

    @Descriptor("Disconnects from the MQTT broker")
    public String disconnect() {
        try {
            if (!client.isConnected()) {
                return "Not connected to the MQTT broker";
            }
            client.disconnect();
            return "Disconnection initiated from the MQTT broker";
        } catch (final Exception e) {
            return "Failed to disconnect from the MQTT broker: " + stackTraceToString(e);
        }
    }
    
    private String prepareSubscriptions(final SubscriptionDTO[] subscriptions) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Channel Name", "Subscription QoS");

        for (final SubscriptionDTO subscription : subscriptions) {
            final ChannelDTO channel = subscription.channel;
            table.addRow(channel.name, String.valueOf(subscription.qos));
        }
        return table.print();
    }

    private String prepareReplyToSubscriptions(final ReplyToSubscriptionDTO[] replyToSubscriptions) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Request Channel", "Response Channel", "Subscription QoS");

        for (final ReplyToSubscriptionDTO subscription : replyToSubscriptions) {
            final ChannelDTO reqChannel = subscription.requestChannel;
            final ChannelDTO resChannel = subscription.responseChannel;
            
            final String reqChannelName = reqChannel == null ? "" : reqChannel.name;
            final String resChannelName = resChannel == null ? "" : resChannel.name;

            table.addRow(reqChannelName, resChannelName, String.valueOf(subscription.qos));
        }
        return table.print();
    }

    private String prepareClientConfig(final Config config, final Converter converter) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Name", "Value");

        // General Connection
        table.addRow("Server Host Address", config.server());
        table.addRow("Server Port", String.valueOf(config.port()));
        table.addRow("Client ID", config.id());
        table.addRow("MQTT Topic Prefix", config.topicPrefix());

        // Reconnection and Session
        table.addRow("Automatic Reconnect Using Default Config", String.valueOf(config.automaticReconnectWithDefaultConfig()));
        table.addRow("Initial Delay", String.valueOf(config.initialDelay()));
        table.addRow("Max Delay", String.valueOf(config.maxDelay()));
        table.addRow("Clean Start", String.valueOf(config.cleanStart()));
        table.addRow("Keep Alive Interval", String.valueOf(config.keepAliveInterval()));
        table.addRow("Use Session Expiry", String.valueOf(config.useSessionExpiry()));
        table.addRow("Session Expiry Interval", String.valueOf(config.sessionExpiryInterval()));
        table.addRow("Use Session Expiry for Disconnect", String.valueOf(config.useSessionExpiryForDisconnect()));
        table.addRow("Session Expiry Interval for Disconnect", String.valueOf(config.sessionExpiryIntervalForDisconnect()));

        // Simple Authentication
        table.addRow("Simple Authentication", String.valueOf(config.simpleAuth()));
        table.addRow("Use static credentials specified in username and password configurations", String.valueOf(config.staticAuthCred()));
        table.addRow("Simple Authentication Username", config.username());
        table.addRow("Simple Authentication Password", config.password()); // Security Warning: Avoid logging passwords
        table.addRow("Simple Authentication Credential Filter", String.valueOf(config.simpleAuthCredFilter()));

        // Custom Executor
        table.addRow("Custom Executor Configuration", String.valueOf(config.useCustomExecutor()));
        table.addRow("Custom Executor Number of Threads", String.valueOf(config.numberOfThreads()));
        table.addRow("Custom Executor Prefix of the thread name", config.threadNamePrefix());
        table.addRow("Custom Executor Suffix of the thread name", config.threadNameSuffix());
        table.addRow("Flag to set if the threads will be daemon threads", String.valueOf(config.isDaemon()));
        table.addRow("Custom Thread Executor Service Class Name", config.executorTargetClass());
        table.addRow("Custom Thread Executor Service Target Filter", config.executorTargetFilter());

        // SSL
        table.addRow("SSL Configuration", String.valueOf(config.useSSL()));
        table.addRow("SSL Configuration Cipher Suites", converter.convert(config.cipherSuites()).to(String.class));
        table.addRow("SSL Configuration Protocols", converter.convert(config.protocols()).to(String.class));
        table.addRow("SSL Configuration Handshake Timeout", String.valueOf(config.sslHandshakeTimeout()));
        table.addRow("SSL Configuration Key Manager Factory Service Target Filter", config.keyManagerFactoryTargetFilter());
        table.addRow("SSL Configuration Trust Manager Factory Service Target Filter", config.trustManagerFactoryTargetFilter());
        table.addRow("SSL Configuration Hostname Verifier Service Target Filter", config.hostNameVerifierTargetFilter());

        // Last Will
        table.addRow("Last Will Topic", config.lastWillTopic());
        table.addRow("Last Will QoS", String.valueOf(config.lastWillQoS()));
        table.addRow("Last Will Payload", config.lastWillPayLoad());
        table.addRow("Last Will Content Type", config.lastWillContentType());
        table.addRow("Last Will Message Expiry Interval", String.valueOf(config.lastWillMessageExpiryInterval()));
        table.addRow("Last Will Delay Interval",  String.valueOf(config.lastWillDelayInterval()));

        // Packet/Message Size
        table.addRow("Maximum Concurrent Messages to be received", String.valueOf(config.receiveMaximum()));
        table.addRow("Maximum Concurrent Messages to be sent", String.valueOf(config.sendMaximum()));
        table.addRow("Maximum Packet Size for receiving", String.valueOf(config.maximumPacketSize()));
        table.addRow("Maximum Packet Size for sending", String.valueOf(config.sendMaximumPacketSize()));
        table.addRow("Maximum Topic Aliases", String.valueOf(config.topicAliasMaximum()));

        // WebSocket
        table.addRow("MQTT over Web Socket", String.valueOf(config.useWebSocket()));
        table.addRow("Web Socket Server Path", config.serverPath());
        table.addRow("Web Socket Sub Protocol", config.subProtocol());
        table.addRow("Web Socket Query String", config.queryString());
        table.addRow("Web Socket Handshake Timeout", String.valueOf(config.webSocketHandshakeTimeout()));

        // Advanced Auth and Interceptors
        table.addRow("Enhanced Authentication", String.valueOf(config.useEnhancedAuthentication()));
        table.addRow("Enhanced Authentication Service Filter", config.enhancedAuthTargetFilter());
        table.addRow("Server Reauthentication", String.valueOf(config.useServerReauth()));
        table.addRow("Connected Listener Service Filters", Arrays.asList(config.connectedListenerFilters()).toString());
        table.addRow("Disconnected Listener Service Filters", Arrays.asList(config.disconnectedListenerFilters()).toString());
        table.addRow("QoS 1 Incoming Interceptor Service Filter", config.qos1IncomingInterceptorFilter());
        table.addRow("QoS 2 Incoming Interceptor Service Filter", config.qos2IncomingInterceptorFilter());
        table.addRow("QoS 1 Outgoing Interceptor Service Filter", config.qos1OutgoingInterceptorFilter());
        table.addRow("QoS 2 Outgoing Interceptor Service Filter", config.qos2OutgoingInterceptorFilter());

        // Component Lifecycle
        table.addRow("Filter to be satisfied for the client to be active", config.osgi_ds_satisfying_condition_target());
        table.addRow("Reason for the disconnection when the client component is stopped", config.disconnectionReasonDescription());
        table.addRow("Code for the disconnection when the client component is stopped", config.disconnectionReasonCode().name());

        return table.print();
    }

    private String prepareSubscriberConfig(final SubscriberConfig config) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Name", "Value");

        table.addRow("Default Timeout (ms)", String.valueOf(config.timeoutInMillis()));
        table.addRow("Default QoS", String.valueOf(config.qos()));

        return table.print();
    }

    private String preparePublisherConfig(final PublisherConfig config) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Name", "Value");

        table.addRow("Default Timeout (ms)", String.valueOf(config.timeoutInMillis()));
        table.addRow("Default QoS", String.valueOf(config.qos()));

        return table.print();
    }

    private String prepareReplyToPublisherConfig(final ReplyToConfig config) {
        final Table table = new Table();

        table.setShowVerticalLines(true);
        table.setHeaders("Name", "Value");

        table.addRow("Thread Pool Size", String.valueOf(config.numThreads()));
        table.addRow("Thread Name Prefix", config.threadNamePrefix());
        table.addRow("Thread Name Suffix", config.threadNameSuffix());
        table.addRow("Daemon Threads", String.valueOf(config.isDaemon()));

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

    private String formatUptime(long millis) {
        if (millis <= 0) {
            return "N/A";
        }
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }

}