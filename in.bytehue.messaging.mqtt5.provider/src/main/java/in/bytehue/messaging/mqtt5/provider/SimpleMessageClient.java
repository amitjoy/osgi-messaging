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
import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.PID.CLIENT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.metatype.annotations.AttributeType.PASSWORD;

import java.util.Arrays;
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

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;

import in.bytehue.messaging.mqtt5.provider.SimpleMessageClient.Config;
import in.bytehue.messaging.mqtt5.provider.helper.MessageHelper;

@ProvideMessagingFeature
@Designate(ocd = Config.class)
@Component(service = SimpleMessageClient.class, configurationPid = CLIENT)
public final class SimpleMessageClient {

    @ObjectClassDefinition( //
            name = "MQTT v5 Messaging Client Configuration", //
            description = "This configuration is used to configure the messaging connection")
    @interface Config {
        @AttributeDefinition(name = "Client Identifier")
        String id();

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

        @AttributeDefinition(name = "Last Will QoS")
        int lastWillQoS() default 2;

        @AttributeDefinition(name = "Last Will Payload")
        String lastWillPayLoad() default "";

        @AttributeDefinition(name = "Last Will Payload")
        String lastWillContentType() default "";

        @AttributeDefinition(name = "Last Will Message Expiry Interval")
        long lastWillMessageExpiryInterval() default 120L;

        @AttributeDefinition(name = "Last Will Delay Interval")
        long delayInterval() default 30L;

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

        @AttributeDefinition(name = "Reason for the disconnection when the component is stopped")
        String disconnectionReasonDescription() default "OSGi Component Deactivated";

        @AttributeDefinition(name = "Code for the disconnection when the component is stopped")
        Mqtt5DisconnectReasonCode disconnectionReasonCode() default NORMAL_DISCONNECTION;
    }

    private final Config config;
    public final Mqtt5AsyncClient client;

    @Activate
    public SimpleMessageClient( //
            final BundleContext bundleContext, //
            final Config config, //
            @Reference(service = LoggerFactory.class) final Logger logger) {

        this.config = config;
        final String clientId = config.id() != null ? config.id() : UUID.randomUUID().toString();
        // @formatter:off
        final Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder()
                                                            .identifier(clientId)
                                                            .serverHost(config.server())
                                                            .serverPort(config.port());

        logger.debug("Applying automatic reconnect configuration");
        if (config.automticReconnect()) {
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
                             .trustManagerFactory(getTrustManagerFactory(config, bundleContext))
                         .applySslConfig();
        }
        final String lastWillTopic = config.lastWillTopic();
        if (!lastWillTopic.isEmpty()) {
            logger.debug("Applying Last Will Configuration");
            clientBuilder.willPublish()
                             .topic(lastWillTopic)
                             .qos(MqttQos.fromCode(config.lastWillQoS()))
                             .payload(config.lastWillPayLoad().getBytes())
                             .contentType(config.lastWillContentType())
                             .messageExpiryInterval(config.lastWillMessageExpiryInterval())
                             .delayInterval(config.lastWillMessageExpiryInterval())
                         .applyWillPublish();
        }
        if (config.useEnhancedAuthentication()) {
            logger.debug("Applying Enhanced Authentication configuration");
            clientBuilder.enhancedAuth(getEnhancedAuth(config, bundleContext));
        }

        client = clientBuilder.buildAsync();

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

        // @formatter:on
        logger.debug("Connection successfully established - {}", connAck);
    }

    @Deactivate
    void deactivate() {
        // @formatter:off
        client.disconnectWith()
              .reasonCode(config.disconnectionReasonCode())
              .reasonString(config.disconnectionReasonDescription())
              .send();
        // @formatter:on
    }

    private TrustManagerFactory getTrustManagerFactory(final Config config, final BundleContext bundleContext) {
        try {
            // @formatter:off
            return MessageHelper.getService(
                        TrustManagerFactory.class,
                        config.trustManagerFactoryTargetFilter(),
                        bundleContext);
            // @formatter:on
        } catch (final Exception e) {
            throw new RuntimeException(
                    "MQTT SSL Configuration Failed since specified'TrustManagerFactory' service is not found");
        }
    }

    private Mqtt5EnhancedAuthMechanism getEnhancedAuth(final Config config, final BundleContext bundleContext) {
        try {
            // @formatter:off
            return MessageHelper.getService(
                    Mqtt5EnhancedAuthMechanism.class,
                    config.enhancedAuthTargetFilter(),
                    bundleContext);
            // @formatter:on
        } catch (final Exception e) {
            throw new RuntimeException(
                    "MQTT Enhanced Authentication Configuration Failed since 'Mqtt5EnhancedAuthMechanism' service is not found");
        }
    }

}
