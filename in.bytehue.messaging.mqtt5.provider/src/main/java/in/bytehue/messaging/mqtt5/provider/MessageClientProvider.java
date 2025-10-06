/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
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
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.CLIENT_ID_FRAMEWORK_PROPERTY;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MQTT_CONNECTION_READY_SERVICE_PROPERTY;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CLIENT;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getOptionalService;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getOptionalServiceWithoutType;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.condition.Condition.CONDITION_ID;
import static org.osgi.service.condition.Condition.CONDITION_ID_TRUE;
import static org.osgi.service.metatype.annotations.AttributeType.PASSWORD;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.annotations.ProvideMessagingFeature;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.hivemq.client.internal.netty.NettyEventLoopProvider;
import com.hivemq.client.mqtt.datatypes.MqttClientIdentifier;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
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
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectBuilder.Send;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectBuilder.SendVoid;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;

import in.bytehue.messaging.mqtt5.api.MqttClient;
import in.bytehue.messaging.mqtt5.provider.MessageClientProvider.Config;
import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

@ProvideMessagingFeature
@Designate(ocd = Config.class)
@Component(immediate = true, service = { MessageClientProvider.class, MqttClient.class }, configurationPid = CLIENT, configurationPolicy = REQUIRE)
public final class MessageClientProvider implements MqttClient {

	@ObjectClassDefinition(
            name = "MQTT 5.0 Messaging Client Configuration",
            description = "This configuration is used to configure the MQTT 5.0 messaging connection. "
                        + "Note that, all time-based configurations are in seconds.")
    public @interface Config {

        // @formatter:off

        //---------- General Connection ----------//

        @AttributeDefinition(name = "Server Host Address")
        String server();

        @AttributeDefinition(name = "Server Port", min = "1", max = "65535")
        int port() default 1883;

        @AttributeDefinition(name = "Client Identifier")
        String id() default "";

        @AttributeDefinition(name = "MQTT Topic Prefix",
                             description = "The prefix will be added to all the topics automatically if set. "
                                         + "It should not contain trailing slash")
        String topicPrefix() default "";

        //---------- Reconnection and Session ----------//

        @AttributeDefinition(name = "Custom Automatic Reconnection")
        boolean automaticReconnectWithDefaultConfig() default true;

        @AttributeDefinition(name = "Initial Delay if Custom Automatic Reconnection is enabled", min = "0")
        long initialDelay() default 10L;

        @AttributeDefinition(name = "Max Delay if Custom Automatic Reconnection is enabled", min = "0")
        long maxDelay() default 600L;

        @AttributeDefinition(name = "Always create new session after the client is connected")
        boolean cleanStart() default false;

        @AttributeDefinition(name = "Keep Alive Interval", min = "0", max = "65535")
        int keepAliveInterval() default 60;

        @AttributeDefinition(name = "Flag to enable/disable session expiry")
        boolean useSessionExpiry() default false;

        @AttributeDefinition(name = "Keep Session State", min = "0")
        int sessionExpiryInterval() default 30;

        @AttributeDefinition(name = "Flag to enable/disable session expiry for disconnection")
        boolean useSessionExpiryForDisconnect() default true;

        @AttributeDefinition(name = "Keep Session State after disconnection", min = "0")
        int sessionExpiryIntervalForDisconnect() default 0;

        //---------- Simple Authentication ----------//

        @AttributeDefinition(name = "Simple Authentication")
        boolean simpleAuth() default false;

        @AttributeDefinition(name = "Configuration to use static credentials specified in username and password configurations")
        boolean staticAuthCred() default true;

        @AttributeDefinition(name = "Simple Authentication Username")
        String username() default "";

        @AttributeDefinition(name = "Simple Authentication Password", type = PASSWORD)
        String password() default "";

        @AttributeDefinition(name = "Simple Authentication Service Filter")
        String simpleAuthCredFilter() default "";

        //---------- Custom Executor ----------//

        @AttributeDefinition(name = "Custom Executor Configuration")
        boolean useCustomExecutor() default false;

        @AttributeDefinition(name = "Custom Executor Number of Threads")
        int numberOfThreads() default 5;

        @AttributeDefinition(name = "Custom Executor Prefix of the thread name")
        String threadNamePrefix() default "osgi-mqtt";

        @AttributeDefinition(name = "Custom Executor Suffix of the thread name (supports only {@code %d} format specifier)")
        String threadNameSuffix() default "-%d";

        @AttributeDefinition(name = "Flag to set if the threads will be daemon threads")
        boolean isDaemon() default true;

        @AttributeDefinition(name = "Custom Thread Executor Service Class Name (Note that, the service should be an instance of Java Executor)")
        String executorTargetClass() default "";

        @AttributeDefinition(name = "Custom Thread Executor Service Target Filter")
        String executorTargetFilter() default "";

        //---------- SSL ----------//

        @AttributeDefinition(name = "SSL Configuration")
        boolean useSSL() default false;

        @AttributeDefinition(name = "SSL Configuration Cipher Suites")
        String[] cipherSuites() default {};

        @AttributeDefinition(name = "SSL Configuration Protocols")
        String[] protocols() default {};

        @AttributeDefinition(name = "SSL Configuration Handshake Timeout", min = "0")
        long sslHandshakeTimeout() default 10L;

        @AttributeDefinition(name = "SSL Configuration Key Manager Factory Service Target Filter")
        String keyManagerFactoryTargetFilter() default "";

        @AttributeDefinition(name = "SSL Configuration Trust Manager Factory Service Target Filter")
        String trustManagerFactoryTargetFilter() default "";

        @AttributeDefinition(name = "SSL Configuration Host Name Verifier Service Target Filter")
        String hostNameVerifierTargetFilter() default "";

        //---------- Last Will ----------//

        @AttributeDefinition(name = "Last Will Topic")
        String lastWillTopic() default "";

        @AttributeDefinition(name = "Last Will QoS", min = "0", max = "2")
        int lastWillQoS() default 2;

        @AttributeDefinition(name = "Last Will Payload")
        String lastWillPayLoad() default "";

        @AttributeDefinition(name = "Last Will Content Type")
        String lastWillContentType() default "";

        @AttributeDefinition(name = "Last Will Message Expiry Interval", min = "0")
        long lastWillMessageExpiryInterval() default 120L;

        @AttributeDefinition(name = "Last Will Delay Interval", min = "0")
        long lastWillDelayInterval() default 30L;

        //---------- Packet/Message Size ----------//

        @AttributeDefinition(name = "Maximum Concurrent Messages to be received", min = "1")
        int receiveMaximum() default 10;

        @AttributeDefinition(name = "Maximum Concurrent Messages to be sent", min = "0")
        int sendMaximum() default 10;

        @AttributeDefinition(name = "Maximum Packet Size for receiving", min = "10")
        int maximumPacketSize() default 10_240; // 10KB

        @AttributeDefinition(name = "Maximum Packet Size for sending", min = "10")
        int sendMaximumPacketSize() default 10_240; // 10KB

        @AttributeDefinition(name = "Maximum Topic Aliases", min = "0")
        int topicAliasMaximum() default 0;

        //---------- WebSocket ----------//

        @AttributeDefinition(name = "MQTT over Web Socket")
        boolean useWebSocket() default false;

        @AttributeDefinition(name = "Web Socket Server Path")
        String serverPath() default "";

        @AttributeDefinition(name = "Web Socket Sub Protocol")
        String subProtocol() default "mqtt";

        @AttributeDefinition(name = "Web Socket Query String")
        String queryString() default "";

        @AttributeDefinition(name = "Web Socket Handshake Timeout")
        long webSocketHandshakeTimeout() default 10L;

        //---------- Advanced Auth and Interceptors ----------//

        @AttributeDefinition(name = "Enhanced Authentication")
        boolean useEnhancedAuthentication() default false;

        @AttributeDefinition(name = "Enhanced Authentication Service Filter")
        String enhancedAuthTargetFilter() default "";

        @AttributeDefinition(name = "Server Reauthentication")
        boolean useServerReauth() default false;

        @AttributeDefinition(name = "Connected Listener Service Filters")
        String[] connectedListenerFilters() default {};

        @AttributeDefinition(name = "Disconnected Listener Service Filters")
        String[] disconnectedListenerFilters() default {};

        @AttributeDefinition(name = "QoS 1 Incoming Interceptor Service Filter")
        String qos1IncomingInterceptorFilter() default "";

        @AttributeDefinition(name = "QoS 2 Incoming Interceptor Service Filter")
        String qos2IncomingInterceptorFilter() default "";

        @AttributeDefinition(name = "QoS 1 Outgoing Interceptor Service Filter")
        String qos1OutgoingInterceptorFilter() default "";

        @AttributeDefinition(name = "QoS 2 Outgoing Interceptor Service Filter")
        String qos2OutgoingInterceptorFilter() default "";

        //---------- Component Lifecycle ----------//

        @AttributeDefinition(name = "Filter to be satisfied for the messaging client to be active")
        String osgi_ds_satisfying_condition_target() default "(" + CONDITION_ID + "=" + CONDITION_ID_TRUE + ")";

        @AttributeDefinition(name = "Reason for the disconnection when the component is stopped gracefully")
        String disconnectionReasonDescription() default "OSGi Component Deactivated";

        @AttributeDefinition(name = "Code for the disconnection when the component is stopped gracefully")
        Mqtt5DisconnectReasonCode disconnectionReasonCode() default NORMAL_DISCONNECTION;

        // @formatter:on
    }

    public volatile Mqtt5AsyncClient client;

    @Reference(service = LoggerFactory.class)
    private Logger logger;
    @Activate
    private BundleContext bundleContext;

    public volatile Config config;
    private ScheduledExecutorService customExecutor;
    private ServiceRegistration<Object> readyServiceReg;

    private String lastDisconnectReason;
    private AtomicLong connectedTimestamp = new AtomicLong(-1L);

	@Activate
    void activate(final Config config, final Map<String, Object> properties) {
    	init(config);
    }

    @Modified
    void modified(final Config config, final Map<String, Object> properties) {
    	logger.info("Client configuration has been modified");
    	disconnect(true);
    	init(config);
    }

    @Deactivate
    void deactivate(final Map<String, Object> properties) {
    	disconnect(false);
    }

    public synchronized Config config() {
        return config;
    }
    
    public long getConnectedTimestamp() {
        return connectedTimestamp.get();
    }

    public String getLastDisconnectReason() {
        return lastDisconnectReason;
    }

    private void init(final Config config) {
    	logger.info("Performing connection");
		this.config = config;
        try {
            connectInternal();
        } catch (final Exception e) {
            logger.error("Error occurred while establishing connection to the broker '{}'", config.server(), e);
        }
	}

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> disconnect(false));
    }

    private void disconnect(final boolean isNormalDisconnection) {
    	logger.info("Performing disconnection");
    	Mqtt5DisconnectReasonCode reasonCode;
    	String reasonDescription;
    	if (isNormalDisconnection) {
    		reasonCode = NORMAL_DISCONNECTION;
    		reasonDescription = "";
    	} else {
    		reasonCode = config.disconnectionReasonCode();
    		reasonDescription = config.disconnectionReasonDescription();
    	}
    	// blocking disconnection ensures that we gracefully disconnect the established connection
		final SendVoid disconnectParams = client.toBlocking()
    	      .disconnectWith()
    	          .reasonCode(reasonCode)
                  .reasonString(reasonDescription);

		if (config.useSessionExpiryForDisconnect()) {
			logger.debug("Applying Session Expiry Interval for Disconnect: {}", config.sessionExpiryIntervalForDisconnect());
			disconnectParams.sessionExpiryInterval(config.sessionExpiryIntervalForDisconnect());
		} else {
			logger.debug("Session Expiry for Disconnect is not enabled");
			disconnectParams.noSessionExpiry();
		}
		disconnectParams.send();
		// shutdown the custom executor if used
		if (customExecutor != null) {
	        customExecutor.shutdownNow();
	        NettyEventLoopProvider.INSTANCE.releaseEventLoop(customExecutor);
    		customExecutor = null;
    	}
	}

    @Override
    public CompletableFuture<Void> connect() {
        if (client != null && client.getState() == com.hivemq.client.mqtt.MqttClientState.CONNECTED) {
            throw new IllegalStateException("Client is already connected");
        }
        return CompletableFuture.runAsync(() -> {
            try {
                connectInternal();
            } catch (final Exception e) {
                throw new RuntimeException("Failed to connect to MQTT broker", e);
            }
        });
    }

    @Override
    public boolean isConnected() {
        return client != null && client.getState() == com.hivemq.client.mqtt.MqttClientState.CONNECTED;
    }

    private void connectInternal() {
    	final String clientId = getClientID(bundleContext);
    	final Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder()
                .identifier(MqttClientIdentifier.of(clientId))
                .serverHost(config.server())
                .serverPort(config.port());
        final Nested<? extends Mqtt5ClientBuilder> advancedConfig = clientBuilder.advancedConfig();
        initLastWill(clientBuilder);

        logger.debug(
                "Adding highest priority connection listeners for (de)/registering MQTT connection ready OSGi service");

        clientBuilder.addConnectedListener(this::registerReadyService);
        clientBuilder.addDisconnectedListener(this::unregisterReadyService);
        
        clientBuilder.addConnectedListener(context -> connectedTimestamp.set(System.currentTimeMillis()));
        clientBuilder.addDisconnectedListener(context -> {
        	connectedTimestamp.set(-1);
        	lastDisconnectReason = context.getCause().getMessage();
        });
        
        if (config.automaticReconnectWithDefaultConfig()) {
            logger.debug("Applying Custom Automatic Reconnect Configuration");
            clientBuilder.automaticReconnect()
                             .initialDelay(config.initialDelay(), SECONDS)
                             .maxDelay(config.maxDelay(), SECONDS)
                         .applyAutomaticReconnect();
        }
        if (config.simpleAuth()) {
        	logger.debug("Applying Simple Authentication Configuration");
        	String username = null;
        	String password = null;
        	if (config.staticAuthCred()) {
        		logger.debug("Applying Simple Authentication Configuration (Static)");
        		username = config.username();
        		password = config.password();
        	} else {
        		logger.debug("Applying Simple Authentication Configuration (Dynamic)");
        		@SuppressWarnings("rawtypes")
				final Optional<Supplier> auth =
        				getOptionalService(
        						Supplier.class,
        						config.simpleAuthCredFilter(),
        						bundleContext,
        						logger);
        		if (auth.isPresent()) {
        			logger.debug("Found Simple Authentication Service - {}", auth.get().getClass().getName());
        			try {
        				final Supplier<?> supplier = auth.get();
        				final Object instance = supplier.get();
						if (!(instance instanceof String)) {
        					throw new RuntimeException("Simple Authentication Service should contain type of String");
        				}
						final String cred = (String) instance;
						final String[] tokens = cred.split(":");
						if (tokens == null) {
							throw new RuntimeException("Simple Authentication Service should return non-null String");
						}
						username = tokens[0];
						password = tokens[1];
					} catch (final Exception e) {
						logger.error("Cannot Retrieve Credentials from Simple Authentication Service", e);
					}
        		} else {
        			logger.warn("Simple Authentiation Service Not Found");
        		}
        	}
        	if (username == null || password == null) {
        		logger.warn("Skipping Simple Authentication Configuration - Username or Password is null");
        	} else {
	        	clientBuilder.simpleAuth()
				 			 .username(username)
				 			 .password(password.getBytes())
				 			 .applySimpleAuth();
        	}
        }
        if (config.useWebSocket()) {
            logger.debug("Applying Web Socket Configuration");
            clientBuilder.webSocketConfig()
                             .serverPath(config.serverPath())
                             .subprotocol(config.subProtocol())
                             .queryString(config.queryString())
                             .handshakeTimeout(config.webSocketHandshakeTimeout(), SECONDS)
                         .applyWebSocketConfig();
        }
        if (config.useSSL()) {
            logger.debug("Applying SSL Configuration");
            clientBuilder.sslConfig()
                             .cipherSuites(emptyToNull(config.cipherSuites()))
                             .protocols(emptyToNull(config.protocols()))
                             .handshakeTimeout(config.sslHandshakeTimeout(), SECONDS)
                             .keyManagerFactory(
                                     getOptionalService(
                                             KeyManagerFactory.class,
                                             config.keyManagerFactoryTargetFilter(),
                                             bundleContext,
                                             logger)
                                     .orElse(null))
                             .trustManagerFactory(
                                     getOptionalService(
                                             TrustManagerFactory.class,
                                             config.trustManagerFactoryTargetFilter(),
                                             bundleContext,
                                             logger)
                                     .orElse(null))
                             .hostnameVerifier(
                                     getOptionalService(
                                             HostnameVerifier.class,
                                             config.hostNameVerifierTargetFilter(),
                                             bundleContext,
                                             logger)
                                     .orElse(null))
                             .applySslConfig();
        }
        if (config.useCustomExecutor()) {
        	logger.debug("Applying Custom Executor Configuration");
        	final String clazz = config.executorTargetClass().trim();
        	if (clazz.isEmpty()) {
        		logger.debug("Applying Executor as Non-Service Configuration");
                final ThreadFactory threadFactory =
                        new ThreadFactoryBuilder()
                                .setThreadFactoryName(config.threadNamePrefix())
                                .setThreadNameFormat(config.threadNameSuffix())
                                .setDaemon(config.isDaemon())
                                .build();
                customExecutor = Executors.newScheduledThreadPool(config.numberOfThreads(), threadFactory);
                ((ScheduledThreadPoolExecutor) customExecutor).setRemoveOnCancelPolicy(true);
                clientBuilder.executorConfig()
	                         .nettyExecutor(customExecutor)
                             .applyExecutorConfig();
        	} else {
        		logger.debug("Applying Executor as Service Configuration");
        		String filter = config.executorTargetFilter().trim();
        		Optional<Object> service = 
        				getOptionalServiceWithoutType(
        				            clazz,
        				            filter,
        				            bundleContext,
        				            logger);
        		service.ifPresent(executor -> clientBuilder.executorConfig()
                                                           .nettyExecutor((Executor) executor)
                                                           .applyExecutorConfig());
        	}
        }
        if (config.useEnhancedAuthentication()) {
            logger.debug("Applying Enhanced Authentication Configuration");
            clientBuilder.enhancedAuth(
                    getOptionalService(
                           Mqtt5EnhancedAuthMechanism.class,
                           config.enhancedAuthTargetFilter(),
                           bundleContext,
                           logger)
                    .orElse(null));
        }
        if (config.useServerReauth()) {
            logger.debug("Applying Server Reauthentication Configuration");
            advancedConfig.allowServerReAuth(config.useServerReauth());
        }
        if (config.connectedListenerFilters().length != 0) {
            logger.debug("Applying Connected Listener Configuration");
            final String[] filters = config.connectedListenerFilters();
            for (final String filter : filters) {
            	if (filter.trim().isEmpty()) {
            		logger.warn("Connected listener filter is empty");
            		continue;
            	}
            	final Optional<MqttClientConnectedListener> listener =
                        getOptionalService(
                                MqttClientConnectedListener.class,
                                filter,
                                bundleContext,
                                logger);
            	listener.ifPresent(l -> {
            		logger.debug("Adding Custom MQTT Connected Listener - {}", l.getClass().getSimpleName());
            		clientBuilder.addConnectedListener(l);
            	});
            }
        }
        if (config.disconnectedListenerFilters().length != 0) {
            logger.debug("Applying Disconnected Listener Configuration");
            final String[] filters = config.disconnectedListenerFilters();
            for (final String filter : filters) {
            	if (filter.trim().isEmpty()) {
            		logger.warn("Disconnected listener filter is empty");
            		continue;
            	}
            	final Optional<MqttClientDisconnectedListener> listener =
                        getOptionalService(
                        		MqttClientDisconnectedListener.class,
                                filter,
                                bundleContext,
                                logger);
            	listener.ifPresent(l -> {
            		logger.debug("Adding Custom MQTT Disconnected Listener - {}", l.getClass().getSimpleName());
            		clientBuilder.addDisconnectedListener(l);
            	});
            }
        }
        if (!config.qos1IncomingInterceptorFilter().isEmpty()) {
            logger.debug("Applying Incoming and Outgoing Interceptor Configuration");
            advancedConfig.interceptors()
                              .incomingQos1Interceptor(
                                      getOptionalService(
                                              Mqtt5IncomingQos1Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger)
                                      .orElse(null))
                              .incomingQos2Interceptor(
                                      getOptionalService(
                                              Mqtt5IncomingQos2Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger)
                                      .orElse(null))
                              .outgoingQos1Interceptor(
                                      getOptionalService(
                                              Mqtt5OutgoingQos1Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger)
                                      .orElse(null))
                              .outgoingQos2Interceptor(
                                      getOptionalService(
                                              Mqtt5OutgoingQos2Interceptor.class,
                                              config.qos1IncomingInterceptorFilter(),
                                              bundleContext,
                                              logger)
                                      .orElse(null))
                          .applyInterceptors();
        }

        advancedConfig.applyAdvancedConfig();
        client = clientBuilder.buildAsync();

		final Send<CompletableFuture<Mqtt5ConnAck>> connectionParams = client.toAsync()
		      .connectWith()
		           .cleanStart(config.cleanStart())
		           .keepAlive(config.keepAliveInterval());

		if (config.useSessionExpiry()) {
			logger.debug("Applying Session Expiry Interval: {}", config.sessionExpiryInterval());
			connectionParams.sessionExpiryInterval(config.sessionExpiryInterval());
		} else {
			logger.debug("Session Expiry is not enabled");
			connectionParams.noSessionExpiry();
		}

		final CompletableFuture<Mqtt5ConnAck> ack =
                connectionParams
                       .restrictions()
                           .receiveMaximum(config.receiveMaximum())
                           .sendMaximum(config.sendMaximum())
                           .maximumPacketSize(config.maximumPacketSize())
                           .sendMaximumPacketSize(config.sendMaximumPacketSize())
                           .sendTopicAliasMaximum(config.topicAliasMaximum())
                       .applyRestrictions()
                       .send();

        ack.whenComplete((connAck, throwable) -> {
            if (throwable != null) {
                logger.error(
                        "Error occurred while connecting to the broker '{}'", config.server(), throwable);
            } else {
                logger.debug("Successfully connected to the broker - '{}'", connAck);
            }
        });
    }

    private void initLastWill(Mqtt5ClientBuilder clientBuilder) {
        String topic = null;
        MqttQos qos = null;
        byte[] payload = null;
        String contentType = null;
        long messageExpiryInterval = 0;
        long delayInterval = 0;

        topic = config.lastWillTopic();
        qos = MqttQos.fromCode(config.lastWillQoS());
        payload = config.lastWillPayLoad().getBytes();
        contentType = config.lastWillContentType();
        messageExpiryInterval = config.lastWillMessageExpiryInterval();
        delayInterval = config.lastWillDelayInterval();

        if (!topic.isEmpty()) {
            logger.debug("Applying Last Will and Testament Configuration");
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

    private String getClientID(final BundleContext bundleContext) {
        // check for the existence of configuration
        if(!config.id().isEmpty()) {
            return config.id();
        }
		// check for framework property if available
		final String id = bundleContext.getProperty(CLIENT_ID_FRAMEWORK_PROPERTY);
		// generate client ID if framework property is absent
		if (id == null) {
		    final String generatedClientId = UUID.randomUUID().toString();
		    // update the generated framework property for others to use
		    System.setProperty(CLIENT_ID_FRAMEWORK_PROPERTY, generatedClientId);
		    return generatedClientId;
		}
		return id;
    }

    private synchronized void registerReadyService(final MqttClientConnectedContext context) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(MQTT_CONNECTION_READY_SERVICE_PROPERTY, "true");

        readyServiceReg = bundleContext.registerService(Object.class, new Object(), FrameworkUtil.asDictionary(properties));
    }

    private synchronized void unregisterReadyService(final MqttClientDisconnectedContext context) {
    	try {
    		if (readyServiceReg != null) {
    			readyServiceReg.unregister();
    			readyServiceReg = null;
    		}
		} catch (final IllegalStateException e) {
			// this could happen if the reconnect happens pretty quickly
			logger.debug("The MQTT Connection Ready service has already been deregistered");
		}
    }

    private <T> List<T> emptyToNull(final T[] array) {
        if (array.length == 0) {
            return null;
        }
        return Arrays.asList(array);
    }

}
