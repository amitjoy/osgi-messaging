<p align="center">
  <img width="563" alt="logo" src="https://user-images.githubusercontent.com/13380182/101778008-90754300-3af3-11eb-95da-91c54608f277.png" />
</p>

## OSGi Messaging Specification and compliant MQTT 5.0 Implementation

This repository comprises the rudimentary API extracted from the [OSGi RFC 246](https://github.com/osgi/design/blob/main/rfcs/rfc0246/rfc-0246-Messaging.pdf) specification and the MQTT 5.0 implementation of it. This spec is not at all the official version as it is based on the preliminary API from the current version of the spec. This is just an experimental repository for OSGi Messaging specification since the official version is not yet released. The official version is expected to be released with the release of OSGi Enterprise R8 specification in the second quarter of 2021 (as mentioned during the [EclipseCON talk](https://www.eclipsecon.org/2020/sessions/asychronous-communication-distributed-environments-new-osgi-messaging-rfc))

--------------------------------------------------------------------------------------------------------------

[![amitjoy - osgi-messaging](https://img.shields.io/static/v1?label=amitjoy&message=osgi-messaging&color=blue&logo=github)](https://github.com/amitjoy/osgi-messaging)
[![stars - osgi-messaging](https://img.shields.io/github/stars/amitjoy/osgi-messaging?style=social)](https://github.com/amitjoy/osgi-messaging)
[![forks - osgi-messaging](https://img.shields.io/github/forks/amitjoy/osgi-messaging?style=social)](https://github.com/amitjoy/osgi-messaging)
[![License - Apache](https://img.shields.io/badge/License-Apache-blue)](#license)
[![Build - Passing](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com/amitjoy/osgi-messaging/runs/1485969918)
[![GitHub release](https://img.shields.io/github/release/amitjoy/osgi-messaging?include_prereleases&sort=semver)](https://github.com/amitjoy/osgi-messaging/releases/)

--------------------------------------------------------------------------------------------------------------

### Minimum Requirements

1. Java 8
2. OSGi R7

--------------------------------------------------------------------------------------------------------------

### Dependencies

This project comprises three projects - 

1. `org.osgi.service.messaging` - The core OSGi messaging specification API
2. `in.bytehue.messaging.mqtt5.api` - The extended API
2. `in.bytehue.messaging.mqtt5.provider` - The core implementation
3. `in.bytehue.messaging.mqtt5.example` - Example project showing how to use in codebase

--------------------------------------------------------------------------------------------------------------

### Installation

To use it in the OSGi environment, you only need to install `in.bytehue.messaging.mqtt5.provider`.

--------------------------------------------------------------------------------------------------------------

#### Project Import

**Import as Eclipse Projects**

1. Install Bndtools
2. Import all the projects (`File -> Import -> General -> Existing Projects into Workspace`)

--------------------------------------------------------------------------------------------------------------

#### Building from Source

Run `./gradlew clean build` in the project root directory

--------------------------------------------------------------------------------------------------------------

### Developer

Amit Kumar Mondal (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

### Contribution [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgi-messaging/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/osgi-messaging/blob/master/CONTRIBUTING.md)

--------------------------------------------------------------------------------------------------------------

### License

This project is licensed under Apache License Version 2.0 [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

--------------------------------------------------------------------------------------------------------------

### Usage

#### Client Configuration

The `in.bytehue.messaging.client` PID can be used to configure the client. The configurable properties are listed below:

* `id` - Client Identifier (optional) (default: empty string) - if empty, `in.bytehue.client.id` framework property is checked and if unavailable, a random identifier will be generated.
* `server` - Server Address (optional) (default: `broker.hivemq.com`)
* `port` - Server Port (optional) (default: `1883`)
* `automaticReconnect` - Automatic Reconnection (optional) (default: `false`)
* `cleanStart` - Resume Previously Established Session (optional) (default: `false`)
* `initialDelay` - Initial Delay if Automatic Reconnection is enabled (optional) (default: `1` second)
* `maxDelay` - Max Delay if Automatic Reconnection is enabled (optional) (default: `30` seconds)
* `sessionExpiryInterval` - Keep Session State (optional) (default: `30` seconds)
* `simpleAuth` - Simple Authentication (optional) (default: `false`)
* `username` - Simple Authentication Username (optional) (default: empty string)
* `password` - Simple Authentication Password (optional) (default: empty string)
* `useSSL` - SSL Configuration (optional) (default: `false`)
* `cipherSuites` - SSL Configuration Cipher Suites (optional) (default: empty array)
* `sslHandshakeTimeout` - SSL Configuration Handshake Timeout (optional) (default: `1` second)
* `trustManagerFactoryTargetFilter` - SSL Configuration Trust Manager Factory Service Target Filter (optional) (default: empty string) (Refer to `javax.net.ssl.TrustManagerFactory`)
* `lastWillTopic` - Last Will Topic (optional) (default: empty string)
* `lastWillQoS` - Last Will QoS (optional) (default: `2`)
* `lastWillPayLoad` - Last Will Payload (optional) (default: empty string)
* `lastWillContentType` - Last Will Content Type (optional) (default: empty string)
* `lastWillMessageExpiryInterval` - Last Will Message Expiry Interval" (optional) (default: `120` seconds)
* `lastWillDelayInterval` - Last Will Delay Interval (optional) (default: `30` seconds)
* `receiveMaximum` - Maximum concurrent messages to be received (optional) (default: `10`)
* `sendMaximum` - Maximum concurrent messages to be sent (optional) (default: `10`)
* `maximumPacketSize` - Maximum Packet Size for receiving (optional) (default: `10240` - 10 KB)
* `sendMaximumPacketSize` - Maximum Packet Size for sending (optional) (default: `10240` - 10 KB)
* `topicAliasMaximum` - Maximum Topic Aliases (optional) (default: `0`)
* `useWebSocket` - MQTT over Web Socket (optional) (default: `false`)
* `queryString` - Web Socket Query String (optional) (default: empty string)
* `serverPath` - Web Socket Server Path (optional) (default: empty string)
* `subProtocol` - Web Socket Sub Protocol (optional) (default: `mqtt`)
* `webSocketHandshakeTimeout` - Web Socket Handshake Timeout (optional) (default: `10` seconds)
* `useEnhancedAuthentication` - Enhanced Authentication (optional) (default: `false`)
* `enhancedAuthTargetFilter` - Enhanced Authentication Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism`)
* `useServerReauth` - Server Reauthentication (optional) (default: `false`)
* `connectedListenerFilter` - Connected Listener Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener`)
* `disconnectedListenerFilter` - Disconnected Listener Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener`)
* `qos1IncomingInterceptorFilter` - QoS 1 Incoming Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos1Interceptor`)
* `qos2IncomingInterceptorFilter` - QoS 2 Incoming Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos2Interceptor`)
* `qos1OutgoingInterceptorFilter` - QoS 1 Outgoing Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos1Interceptor`)
* `qos2OutgoingInterceptorFilter` - QoS 2 Outgoing Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos2Interceptor`)
* `condition.target` - LDAP filter that needs to be satisfied for the client to be active (default: `(satisfy=always)`) (Refer to `in.bytehue.messaging.mqtt5.api.TargetCondition`)
* `disconnectionReasonDescription` - Reason for the disconnection when the component is stopped (optional) (default: `OSGi Component Deactivated`)
* `disconnectionReasonCode` - Code for the disconnection when the component is stopped (optional) (default: `NORMAL_DISCONNECTION`) (Refer to `com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode`)

#### Reply-To Publisher Internal Executor Configuration

The `in.bytehue.messaging.publisher` PID can be used to configure the internal thread pool

* `numThreads` - Number of Threads for the internal thread pool (optional) (default: `20`)
* `threadNamePrefix` - Prefix of the thread name (optional) (default: `mqtt-replyto-publisher`)
* `threadNameSuffix` - Suffix of the thread name (supports only `%d` format specifier) (optional) (default: `-%d`)
* `isDaemon` - Flag to set if the threads will be daemon threads (optional) (default: `true`)

#### Primary Messaging APIs

* `org.osgi.service.messaging.MessagePublisher` - service to publish to specific topic
* `org.osgi.service.messaging.MessageSubscription` - service to subscribe to specific topic
* `org.osgi.service.messaging.Message` - a message that is exchanged between subscriber and publisher
* `org.osgi.service.messaging.MessageContext` - configuration for a specific message
* `org.osgi.service.messaging.MessageContextBuilder` - service to prepare a specific message
* `org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext` - configuration denoting if a message has been acknowledged (this requires the implementation to support `acknowledge` feature)
* `org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder` - service to prepare a messaging configuration for acknowledgement
* `org.osgi.service.messaging.replyto.ReplyToPublisher` - service to send a request for receiving a response
* `org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler` - consumer API to handle response for a request and pusblish the response
* `org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler` - consumer API to handle only responses without publishing
* `org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler` - consumer API to handle to handle stream of requests and publish stream of responses
* `org.osgi.service.messaging.runtime.MessageServiceRuntime` - service to respresent the runtime information of a Message Service instance of an implementation.

The included APIs also provide several helpful annotations for implementors as well as consumers. Refer to `org.osgi.service.messaging.propertytypes` and `org.osgi.service.messaging.annotations` packages.

The OSGi messaging specification is catered to provide a unified solution to accomodate the trending messaging solutions such as AMQP, MQTT etc. That's why no generic API exists for MQTT. To fill in this gap, an implementation specific API exists in `in.bytehue.messaging.mqtt5.api`.

#### Secondary Messaging APIs

* `in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder` - an extended service of `org.osgi.service.messaging.MessageContextBuilder` that could be used to prepare MQTT 5.0 specific message context.
* `in.bytehue.messaging.mqtt5.api.TargetCondition` - marker service interface which consumers can implement to provide services with properties that can be used as conditional target to the MQTT client. That means, consumer can provide filters that should be satisfied before MQTT client is up and running.

#### Examples in Action


* Simple Pub/Sub Example:

```java
@Component
public final class Mqtt5PubSubExample {

    private PushStream<Message> stream;

    @Reference(target = "(osgi.messaging.protocol=mqtt5)")
    private MessagePublisher publisher;

    @Reference(target = "(osgi.messaging.protocol=mqtt5)")
    private MessageSubscription subscriber;

    @Reference(target = "(osgi.messaging.protocol=mqtt5)")
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    @Deactivate
    void deactivate() {
        stream.close();
    }

    public String sub(final String channel) {
        stream = subscriber.subscribe(channel);
        stream.forEach(m -> {
            System.out.println("Message Received");
            System.out.println(StandardCharsets.UTF_8.decode(m.payload()).toString());
        });
        return "Subscribed to " + channel;
    }

    public String pub(final String channel, final String data) {
        final MessageContextBuilder mcb = mcbFactory.getService();
        try {
            // @formatter:off
            publisher.publish(
                    mcb.content(ByteBuffer.wrap(data.getBytes()))
                       .channel(channel)
                       .buildMessage());
            // @formatter:on
        } finally {
            mcbFactory.ungetService(mcb);
        }
        return "Published to " + channel;
    }
}
```

* Simple Reply-To Publish Example

```java
@Component
public final class Mqtt5ReplyToExample {

    @Reference(target = "(osgi.messaging.protocol=mqtt5)")
    private ReplyToPublisher mqttPublisher;

    @Reference(target = "(osgi.messaging.protocol=mqtt5)")
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    public void publishReplyToMessage() {
        final MessageContextBuilder mcb = mcbFactory.getService();
        try {
            // @formatter:off
            final Message request =
                    mcb.channel("/demo")
                       .correlationId("test123")
                       .replyTo("demo_response")
                       .content(ByteBuffer.wrap("Hello Word!".getBytes()))
                       .buildMessage();
            // @formatter:on
            mqttPublisher.publishWithReply(request).onSuccess(System.out::println);
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }
}
```

* Simple Reply-To Single Subscription Handler Example:

```java
@Component
@ReplyToSubscription(target = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo)))", channel = "a/b", replyChannel = "c/d")
public final class Mqtt5ReplyToSingleSubscriptionHandler implements ReplyToSingleSubscriptionHandler {

    @Override
    public Message handleResponse(final Message requestMessage, final MessageContextBuilder responseBuilder) {
        final String content = StandardCharsets.UTF_8.decode(requestMessage.payload()).append("EXAMPLE").toString();
        return responseBuilder.content(ByteBuffer.wrap(content.getBytes())).buildMessage();
    }

}
```

* Simple Message Acknowledgement Example:

```java
@Component
public final class Mqtt5AckExample {

    @Reference(target = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.feature=acknowledge))")
    private MessageSubscription mqttSubscription;

    @Reference(target = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.feature=acknowledge))")
    private ComponentServiceObjects<AcknowledgeMessageContextBuilder> amcbFactory;

    public void acknowledge() {
        final AcknowledgeMessageContextBuilder ackBuilder = amcbFactory.getService();
        try {
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
        } finally {
            amcbFactory.ungetService(ackBuilder);
        }
    }
}
```

* Updating LWT dynamically by sending publish request:

```java
@Component
public final class Mqtt5LwtPublish {

    @Reference(target = "(osgi.messaging.protocol=mqtt5)")
    private MessagePublisher publisher;

    @Reference(target = "(osgi.messaging.protocol=mqtt5)")
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    public void lastWillPublish() {
        final MessageContextBuilder mcb = mcbFactory.getService();
        try {
            publisher.publish(
                    mcb.content(ByteBuffer.wrap("CLOSED_CONNECTION".getBytes()))
                       .channel("last/will/topc/example")
                       .extensionEntry(EXTENSION_QOS, 2)
                       .extensionEntry(EXTENSION_LAST_WILL, true)
                       .extensionEntry(LAST_WILL_DELAY_INTERVAL, 30)
                       .extensionEntry(MESSAGE_EXPIRY_INTERVAL, 30)
                       .buildMessage());
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }

}
```

#### Useful Notes

* Since more than one implementations can coexist in the OSGi runtime, we can search for the MQTT services by means of the provided service properties.
* Refer to the examples above.
* Also note that, the `in.bytehue.messaging.mqtt5.provider` bundle packages the APIs and implementation together. This bundle also packages and exports the HiveMQ Java client APIs to perform enhanced configuration to the client.
* For more details, have a look at the [example](https://github.com/amitjoy/osgi-messaging/tree/main/in.bytehue.messaging.mqtt5.example) project

#### Target Condition Satisfiability for MQTT client

In certain circumstances the MQTT client requires few services to be up and running before the client is connected to the broker. For example, you could provide the following services for the client to use before it connects to the server:

* `javax.net.ssl.TrustManagerFactory`
* `com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener`
* `com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener`
* `com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism`
* `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos1Interceptor`
* `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos2.Mqtt5IncomingQos2Interceptor`
* `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos1Interceptor`
* `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos2.Mqtt5OutgoingQos2Interceptor`

In such scenario, consumers can implement `in.bytehue.messaging.mqtt5.api.TargetCondition` marker API to provide services with properties that can be used as conditional target to the MQTT client (refer to `condition.target` property in `in.bytehue.messaging.client` configuration. That means, consumer can provide filters that should be satisfied before MQTT client is up and running.

Assume we have a service R that represents a link to a remote service. For performance and reliability reasons, we require at least 3 of those services to be present before we can start the MQTT client. Additionally, these services must come from at least 2 different regions. For this reason, we define a property region that can take the values south, east, north, and west.

You can, therefore, use this filter: `(&(#>=3)([unq]region>=2))`

You can make use of the following extended calculated values:

* `#key` - Calculates the number of key properties
* `[avg]key` - Calculates the average of all key properties
* `[min]key` - Calculates the minimum of all key properties
* `[max]key` - Calculates the maximum of all key properties
* `[sum]key` - Calculates the sum of all key properties
* `[unq]key` - Calculates the number of unique key properties

This will ensure that your services will be up and running before the client gets activated. This also guarantees that the start order of the bundles is not at all required in this scenario.
