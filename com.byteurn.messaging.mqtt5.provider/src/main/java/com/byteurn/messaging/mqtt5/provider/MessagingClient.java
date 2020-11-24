package com.byteurn.messaging.mqtt5.provider;

import static com.byteurn.messaging.mqtt5.provider.MessagingClient.PID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.metatype.annotations.AttributeType.PASSWORD;

import java.util.Arrays;
import java.util.UUID;

import javax.net.ssl.TrustManagerFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.annotations.ProvideMessagingFeature;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.byteurn.messaging.mqtt5.provider.MessagingClient.Config;
import com.byteurn.messaging.mqtt5.provider.helper.MessagingHelper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;

@ProvideMessagingFeature
@Designate(ocd = Config.class)
@Component(service = MessagingClient.class, configurationPid = PID)
public final class MessagingClient {

    public static final String PID = "com.byteurn.messaging.client";

    @ObjectClassDefinition( //
            name = "MQTT Messaging Client Configuration", //
            description = "This configuration is used to configure the messaging connection")
    @interface Config {
        @AttributeDefinition(name = "Client Identifier")
        String id();

        @AttributeDefinition(name = "Server Host Address")
        String server()

        default "broker.hivemq.com";

        @AttributeDefinition(name = "Automatic Reconnection")
        boolean automticReconnect()

        default false;

        @AttributeDefinition(name = "Initial Delay if Automatic Reconnection is enabled")
        long initialDelay()

        default 1L;

        @AttributeDefinition(name = "Max Delay if Automatic Reconnection is enabled")
        long maxDelay()

        default 30L;

        @AttributeDefinition(name = "Server Port", min = "1", max = "65535")
        int port()

        default 1883;

        @AttributeDefinition(name = "Simple Authentication")
        boolean simpleAuth()

        default false;

        @AttributeDefinition(name = "Simple Authentication Username")
        String username()

        default "";

        @AttributeDefinition(name = "Simple Authentication Password", type = PASSWORD)
        String password()

        default "";

        @AttributeDefinition(name = "SSL Configuration")
        boolean useSSL()

        default false;

        @AttributeDefinition(name = "SSL Configuration Cipher Suites")
        String[] cipherSuites() default {};

        @AttributeDefinition(name = "SSL Configuration Handshake Timeout")
        long handshakeTimeout()

        default 1L;

        @AttributeDefinition(name = "SSL Configuration Trust Manager Factory Service Target Filter")
        String trustManagerFactoryTargetFilter()

        default "";

        @AttributeDefinition(name = "Last Will Topic")
        String lastWillTopic()

        default "";

        @AttributeDefinition(name = "Last Will QoS")
        int lastWillQoS()

        default 2;

        @AttributeDefinition(name = "Last Will Payload")
        String lastWillPayLoad()

        default "";

        @AttributeDefinition(name = "Last Will Payload")
        String lastWillContentType()

        default "";

        @AttributeDefinition(name = "Last Will Message Expiry Interval")
        long lastWillMessageExpiryInterval()

        default 120L;

        @AttributeDefinition(name = "Last Will Delay Interval")
        long delayInterval()

        default 30L;

        @AttributeDefinition(name = "Maximum concurrent messages to be received")
        int receiveMaximum()

        default 10;

        @AttributeDefinition(name = "Maximum concurrent messages to be sent")
        int sendMaximum()

        default 10;

        @AttributeDefinition(name = "Maximum Packet Size for receiving")
        int maximumPacketSize()

        default 10_240; // 10KB

        @AttributeDefinition(name = "Maximum Packet Size for sending")
        int sendMaximumPacketSize()

        default 10_240; // 10KB

        @AttributeDefinition(name = "Maximum Topic Aliases")
        int topicAliasMaximum() default 0;
    }

    Mqtt5Client client;

    @Activate
    private BundleContext bundleContext;

    private final Logger logger;

    @Activate
    public MessagingClient(final Config config, @Reference(service = LoggerFactory.class) final Logger logger) {
        this.logger = logger;
        final String clientId = config.id() != null ? config.id() : UUID.randomUUID().toString();
        // @formatter:off
        final Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder()
                                                           .identifier(clientId)
                                                           .serverHost(config.server())
                                                           .serverPort(config.port());
        if (config.automticReconnect()) {
            logger.debug("Applying automatic reconnect configuration");
            clientBuilder.automaticReconnect()
                         .initialDelay(config.initialDelay(), SECONDS)
                         .maxDelay(config.maxDelay(), SECONDS)
                         .applyAutomaticReconnect();
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
                         .trustManagerFactory(getTrustManagerFactory(config))
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
        // restrictions
        logger.debug("Applying Server Restrictions");
        clientBuilder.buildAsync()
                     .connectWith()
                     .restrictions()
                     .receiveMaximum(config.receiveMaximum())
                     .sendMaximum(config.sendMaximum())
                     .maximumPacketSize(config.maximumPacketSize())
                     .sendMaximumPacketSize(config.sendMaximumPacketSize())
                     .sendTopicAliasMaximum(config.topicAliasMaximum())
                     .applyRestrictions();

        // @formatter:on
        client = clientBuilder.build();
    }

    private TrustManagerFactory getTrustManagerFactory(final Config config) {
        try {
            // @formatter:off
            return MessagingHelper.getService(
                        TrustManagerFactory.class,
                        config.trustManagerFactoryTargetFilter(),
                        bundleContext);
            // @formatter:on
        } catch (final Exception e) {
            logger.error("MQTT SSL Configuration Failed {}", e);
        }
        return null;
    }

}