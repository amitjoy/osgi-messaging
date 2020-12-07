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

import static com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CLIENT;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getOptionalService;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.metatype.annotations.AttributeType.PASSWORD;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.TrustManagerFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.annotations.ProvideMessagingFeature;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.hivemq.client.internal.mqtt.message.publish.MqttWillPublish;
import com.hivemq.client.mqtt.datatypes.MqttClientIdentifier;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.advanced.Mqtt5ClientAdvancedConfigBuilder.Nested;
import com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos1Interceptor;
import com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos1Interceptor;
import com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos2.Mqtt5IncomingQos2Interceptor;
import com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos2.Mqtt5OutgoingQos2Interceptor;
import com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;

import aQute.osgi.conditionaltarget.api.ConditionalTarget;
import in.bytehue.messaging.mqtt5.api.TargetCondition;
import in.bytehue.messaging.mqtt5.provider.MessageClientProvider.Config;

@ProvideMessagingFeature
@Designate(ocd = Config.class)
@Component(service = MessageClientProvider.class, configurationPid = CLIENT)
public final class MessageClientProvider {

    //@formatter:off
    @ObjectClassDefinition(
            name = "MQTT v5 Messaging Client Configuration",
            description = "This configuration is used to configure the messaging connection")
    @interface Config {
        @AttributeDefinition(name = "Client Identifier")
        String id() default "";

        @AttributeDefinition(name = "Server Host Address")
        String server() default "broker.hivemq.com";

        @AttributeDefinition(name = "Automatic Reconnection")
        boolean automticReconnect() default false;

        @AttributeDefinition(name = "Resume Previously Established Session")
        boolean cleanStart() default false;

        @AttributeDefinition(name = "Initial Delay if Automatic Reconnection is enabled")
        long initialDelay() default 1L;

        @AttributeDefinition(name = "Max Delay if Automatic Reconnection is enabled")
        long maxDelay() default 30L;

        @AttributeDefinition(name = "Keep Session State")
        long sessionExpiryInterval() default 30L;

        @AttributeDefinition(name = "Server Port", min = "1", max = "65535")
        int port() default 1883;

        @AttributeDefinition(name = "Simple Authentication")
        boolean simpleAuth() default false;

        @AttributeDefinition(name = "Simple Authentication Username")
        String username() default "";

        @AttributeDefinition(name = "Simple Authentication Password", type = PASSWORD)
        String password() default "";

        @AttributeDefinition(name = "SSL Configuration")
        boolean useSSL() default false;

        @AttributeDefinition(name = "SSL Configuration Cipher Suites")
        String[] cipherSuites() default {};

        @AttributeDefinition(name = "SSL Configuration Handshake Timeout")
        long handshakeTimeout() default 1L;

        @AttributeDefinition(name = "SSL Configuration Trust Manager Factory Service Target Filter")
        String trustManagerFactoryTargetFilter() default "";

        @AttributeDefinition(name = "Last Will Topic")
        String lastWillTopic() default "";

        @AttributeDefinition(name = "Last Will QoS", min = "0", max = "2")
        int lastWillQoS() default 2;

        @AttributeDefinition(name = "Last Will Payload")
        String lastWillPayLoad() default "";

        @AttributeDefinition(name = "Last Will Content Type")
        String lastWillContentType() default "";

        @AttributeDefinition(name = "Last Will Message Expiry Interval")
        long lastWillMessageExpiryInterval() default 120L;

        @AttributeDefinition(name = "Last Will Delay Interval")
        long lastWillDelayInterval() default 30L;

        @AttributeDefinition(name = "Maximum concurrent messages to be received")
        int receiveMaximum() default 10;

        @AttributeDefinition(name = "Maximum concurrent messages to be sent")
        int sendMaximum() default 10;

        @AttributeDefinition(name = "Maximum Packet Size for receiving")
        int maximumPacketSize() default 10_240; // 10KB

        @AttributeDefinition(name = "Maximum Packet Size for sending")
        int sendMaximumPacketSize() default 10_240; // 10KB

        @AttributeDefinition(name = "Maximum Topic Aliases")
        int topicAliasMaximum() default 0;

        @AttributeDefinition(name = "Enhanced Authentication")
        boolean useEnhancedAuthentication() default false;

        @AttributeDefinition(name = "Enhanced Authentication Service Filter")
        String enhancedAuthTargetFilter() default "";

        @AttributeDefinition(name = "Server Reauthentication")
        boolean useServerReauth() default false;

        @AttributeDefinition(name = "Connected Listener Service Filter")
        String connectedListenerFilter() default "";

        @AttributeDefinition(name = "Disconnected Listener Service Filter")
        String disconnectedListenerFilter() default "";

        @AttributeDefinition(name = "QoS 1 Incoming Interceptor Service Filter")
        String qos1IncomingInterceptorFilter() default "";

        @AttributeDefinition(name = "QoS 2 Incoming Interceptor Service Filter")
        String qos2IncomingInterceptorFilter() default "";

        @AttributeDefinition(name = "QoS 1 Outgoing Interceptor Service Filter")
        String qos1OutgoingInterceptorFilter() default "";

        @AttributeDefinition(name = "QoS 2 Outgoing Interceptor Service Filter")
        String qos2OutgoingInterceptorFilter() default "";

        @AttributeDefinition(name = "Filter that needs to be satisfied for the client to be active")
        String condition_target() default "";

        @AttributeDefinition(name = "Reason for the disconnection when the component is stopped")
        String disconnectionReasonDescription() default "OSGi Component Deactivated";

        @AttributeDefinition(name = "Code for the disconnection when the component is stopped")
        Mqtt5DisconnectReasonCode disconnectionReasonCode() default NORMAL_DISCONNECTION;
    }

    private static final long SESSION_EXPIRY_ON_LAST_WILL_UPDATE_DISCONNECT = 600L;
    private static final String CLIENT_ID_FRAMEWORK_PROPERTY = "in.bytehue.client.id";

    public final Mqtt5AsyncClient client;

    private final Logger logger;
    private final Config config;
    private final BundleContext bundleContext;
    private final Mqtt5ClientBuilder clientBuilder;

    @Reference(target = "(satisfy=always)")
    private ConditionalTarget<TargetCondition> condition;

    @Activate
    public MessageClientProvider(
            final Config config,
            final BundleContext bundleContext,

            @Reference(service = LoggerFactory.class)
            final Logger logger) {

        this.logger = logger;
        this.config = config;
        this.bundleContext = bundleContext;

        final String clientId = clientID(bundleContext);

        // @formatter:off
        clientBuilder = Mqtt5Client.builder()
                                   .identifier(MqttClientIdentifier.of(clientId))
                                   .serverHost(config.server())
                                   .serverPort(config.port());
        client = clientBuilder.buildAsync();

        // last will can be configured in two different ways =>
        // 1. using initial configuration
        // 2. client can send a special publish request which will be used as will message
        // In case of the second scenario, a reconnection happens
        initLastWill(null);
        connect();
    }

    @Deactivate
    void deactivate() {
        client.disconnectWith()
                  .reasonCode(config.disconnectionReasonCode())
                  .reasonString(config.disconnectionReasonDescription())
              .send();
    }

    public void updateLWT(final MqttWillPublish lastWillMessage) {
        // disconnect but keep the previous session alive for 10 mins before reconnection
        // previous session is stored to not remove any previous subscriptions
        client.disconnectWith()
                  .reasonCode(NORMAL_DISCONNECTION)
                  .reasonString("Updated Last will and Testament (LWT) dynamically using publish request message")
                  .sessionExpiryInterval(SESSION_EXPIRY_ON_LAST_WILL_UPDATE_DISCONNECT)
              .send()
              .thenAccept(v -> {
                  initLastWill(lastWillMessage);
                  connect();
              });
    }

    private void connect() {
        final Nested<? extends Mqtt5ClientBuilder> advancedConfig = clientBuilder.advancedConfig();

        if (config.automticReconnect()) {
            logger.debug("Applying automatic reconnect configuration");
            clientBuilder.automaticReconnect()
                             .initialDelay(config.initialDelay(), SECONDS)
                             .maxDelay(config.maxDelay(), SECONDS)
                         .applyAutomaticReconnect();
        } else {
            clientBuilder.automaticReconnectWithDefaultConfig();
        }
        if (config.simpleAuth()) {
            logger.debug("Applying Simple authentiation configuration");
            clientBuilder.simpleAuth()
                             .username(config.username())
                             .password(config.password().getBytes())
                         .applySimpleAuth();
        }
        if (config.useSSL()) {
            logger.debug("Applying SSL configuration");
            clientBuilder.sslConfig()
                             .cipherSuites(Arrays.asList(config.cipherSuites()))
                             .handshakeTimeout(config.handshakeTimeout(), SECONDS)
                             .trustManagerFactory(
                                     getOptionalService(
                                             TrustManagerFactory.class,
                                             config.trustManagerFactoryTargetFilter(),
                                             bundleContext,
                                             logger).orElse(null))
                             .applySslConfig();
        }
        if (config.useEnhancedAuthentication()) {
            logger.debug("Applying Enhanced Authentication configuration");
            clientBuilder.enhancedAuth(
                    getOptionalService(
                           Mqtt5EnhancedAuthMechanism.class,
                           config.enhancedAuthTargetFilter(),
                           bundleContext,
                           logger).orElse(null));
        }
        if (config.useServerReauth()) {
            logger.debug("Applying Server Reauthentication configuration");
            advancedConfig.allowServerReAuth(config.useServerReauth());
        }
        if (!config.connectedListenerFilter().isEmpty()) {
            logger.debug("Applying Connected Listener configuration");
            final Optional<MqttClientConnectedListener> listener =
                    getOptionalService(
                            MqttClientConnectedListener.class,
                            config.connectedListenerFilter(),
                            bundleContext,
                            logger);
            listener.ifPresent(clientBuilder::addConnectedListener);
        }
        if (!config.disconnectedListenerFilter().isEmpty()) {
            logger.debug("Applying Disconnected Listener configuration");
            final Optional<MqttClientDisconnectedListener> listener =
                    getOptionalService(
                            MqttClientDisconnectedListener.class,
                            config.disconnectedListenerFilter(),
                            bundleContext,
                            logger);
            listener.ifPresent(clientBuilder::addDisconnectedListener);
        }
        if (!config.qos1IncomingInterceptorFilter().isEmpty()) {
            logger.debug("Applying Incoming and Outgoing Interceptors' configuration");
            advancedConfig.interceptors()
                              .incomingQos1Interceptor(
                                      getOptionalService(
                                              Mqtt5IncomingQos1Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger).orElse(null))
                              .incomingQos2Interceptor(
                                      getOptionalService(
                                              Mqtt5IncomingQos2Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger).orElse(null))
                              .outgoingQos1Interceptor(
                                      getOptionalService(
                                              Mqtt5OutgoingQos1Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger).orElse(null))
                              .outgoingQos2Interceptor(
                                      getOptionalService(
                                              Mqtt5OutgoingQos2Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger).orElse(null))
                          .applyInterceptors();
        }
        advancedConfig.applyAdvancedConfig();

        final Mqtt5ConnAck connAck = client.toBlocking()
                                           .connectWith()
                                               .cleanStart(config.cleanStart())
                                               .sessionExpiryInterval(config.sessionExpiryInterval())
                                           .restrictions()
                                               .receiveMaximum(config.receiveMaximum())
                                               .sendMaximum(config.sendMaximum())
                                               .maximumPacketSize(config.maximumPacketSize())
                                               .sendMaximumPacketSize(config.sendMaximumPacketSize())
                                               .sendTopicAliasMaximum(config.topicAliasMaximum())
                                           .applyRestrictions()
                                           .send();

        logger.debug("Connection successfully established - {}", connAck);
    }

    private void initLastWill(final MqttWillPublish publish) {

        String topic = null;
        MqttQos qos = null;
        byte[] payload = null;
        String contentType = null;
        long messageExpiryInterval = 0;
        long delayInterval = 0;

        if (publish == null) {
            topic = config.lastWillTopic();
            qos = MqttQos.fromCode(config.lastWillQoS());
            payload = config.lastWillPayLoad().getBytes();
            contentType = config.lastWillContentType();
            messageExpiryInterval = config.lastWillMessageExpiryInterval();
            delayInterval = config.lastWillDelayInterval();
        } else {
            topic = publish.getTopic().toString();
            qos = publish.getQos();
            payload = publish.getPayloadAsBytes();
            contentType = publish.getContentType().map(MqttUtf8String::toString).orElse(null);
            messageExpiryInterval = publish.getRawMessageExpiryInterval();
            delayInterval = publish.getDelayInterval();
        }

        if (!topic.isEmpty()) {
            logger.debug("Applying Last Will Configuration");
            clientBuilder.willPublish()
                             .topic(topic)
                             .qos(qos)
                             .payload(payload)
                             .contentType(contentType)
                             .messageExpiryInterval(messageExpiryInterval)
                             .delayInterval(delayInterval)
                         .applyWillPublish();
        }
    }

    private String clientID(final BundleContext bundleContext) {
        // check for the existence of configuration
        if(config.id().isEmpty()) {
            // check for framework property if available
            final String id = bundleContext.getProperty(CLIENT_ID_FRAMEWORK_PROPERTY);
            // generate client ID if framework property is absent
            return id == null ? UUID.randomUUID().toString() : id;
        } else {
            return config.id();
        }
    }

}
