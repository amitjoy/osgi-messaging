<img width="558" alt="logo" src="https://user-images.githubusercontent.com/13380182/99921124-a2e83200-2d28-11eb-92d3-b96295b841a9.png">

## OSGi Messaging Specification and compliant MQTT v5 Implementation

This repository comprises the rudimentary API extracted from the [OSGi RFC 246](https://github.com/osgi/design/blob/main/rfcs/rfc0246/rfc-0246-Messaging.pdf) specification and the MQTT v5 implementation of it. This spec is not at all the official version as it is simply based on the prelimary API from the current version of the spec. This is just an experimental repository for OSGi Messaging specification since the official version is not yet released. The official version is expected to be released with the release of OSGi Enterprise R8 specification in the second quarter of 2021 (as mentioned during the [EclipseCON talk](https://www.eclipsecon.org/2020/sessions/asychronous-communication-distributed-environments-new-osgi-messaging-rfc))

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

To use it in OSGi environment, you only need to install `in.bytehue.messaging.mqtt5.provider`.

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

This project is licensed under Apache License [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

--------------------------------------------------------------------------------------------------------------

### Usage

#### Client Configuration

The `in.bytehue.messaging.client` can be used to configure the client.

* `id` - Client Identifier (optional) (default: empty string) - if empty, `in.bytehue.client.id` framework property is checked and if unavailable, a random identifier will be generated.
* `server` - Server Address (optional) (default: `broker.hivemq.com`)
* `port` - Server Port (optional) (default: `1883`)
* `automticReconnect` - Automatic Reconnection (optional) (default: `false`)
* `cleanStart` - Resume Previously Established Session (optional) (default: `false`)
* `initialDelay` - Initial Delay if Automatic Reconnection is enabled (optional) (default: `1` second)
* `maxDelay` - Max Delay if Automatic Reconnection is enabled (optional) (default: `30` seconds)
* `sessionExpiryInterval` - Keep Session State (optional) (default: `30` seconds)
* `simpleAuth` - Simple Authentication (optional) (default: `false`)
* `username` - Simple Authentication Username (optional) (default: empty string)
* `password` - Simple Authentication Password (optional) (default: empty string)
* `useSSL` - SSL Configuration (optional) (default: `false`)
* `cipherSuites` - SSL Configuration Cipher Suites (optional) (default: empty array)
* `handshakeTimeout` - SSL Configuration Handshake Timeout (optional) (default: `1` second)
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
* `useEnhancedAuthentication` - Enhanced Authentication (optional) (default: `false`)
* `enhancedAuthTargetFilter` - Enhanced Authentication Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism`)
* `useServerReauth` - Server Reauthentication (optional) (default: `false`)
* `connectedListenerFilter` - Connected Listener Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener`)
* `disconnectedListenerFilter` - Disconnected Listener Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener`)
* `qos1IncomingInterceptorFilter` - QoS 1 Incoming Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos1Interceptor`)
* `qos2IncomingInterceptorFilter` - QoS 2 Incoming Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos2Interceptor`)
* `qos1OutgoingInterceptorFilter` - QoS 1 Outgoing Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos1Interceptor`)
* `qos2OutgoingInterceptorFilter` - QoS 2 Outgoing Interceptor Service Filter (optional) (default: empty string) (Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos2Interceptor`)
* `condition.target` - LDAP filter that needs to be satisfied for the client to be active (default: empty string) (Refer to `in.bytehue.messaging.mqtt5.api.TargetCondition`)
* `disconnectionReasonDescription` - Reason for the disconnection when the component is stopped (optional) (default: `OSGi Component Deactivated`)
* `disconnectionReasonCode` - Code for the disconnection when the component is stopped (optional) (default: `NORMAL_DISCONNECTION`) (Refer to `com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode`)

#### Primary APIs

* `org.osgi.service.messaging.MessagePublisher` - service to publish to specific topic
* `org.osgi.service.messaging.MessageSubscription` - service to subscribe to specific topic
* `org.osgi.service.messaging.Message` - a message that is exchanged between subscriber and publisher
* `org.osgi.service.messaging.MessageContext` - configuration for a specific message
* `org.osgi.service.messaging.MessageContextBuilder` - service to prepare a specific message
* `in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder` - extended service to prepare a specific MQTT message
* `org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext` - configuration denoting if a message has been acknowledged (this requires the implementation to support `acknowledge` feature)
* `org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder` - service to prepare a messaging configuration for acknowledgement
* `org.osgi.service.messaging.replyto.ReplyToPublisher` - service to send a request for receiving a response
* `org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler` - consumer API to handle response for a request and pusblish the response
* `org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler` - consumer API to handle only responses without publishing
* `org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler` - consumer API to handle to handle stream of requests and publish stream of responses
* `org.osgi.service.messaging.runtime.MessageServiceRuntime` - service to respresent the runtime information of a Message Service instance of an implementation.

The included APIs also provide several helpful annotations for implementors as well as consumers. Refer to `org.osgi.service.messaging.propertytypes` and `org.osgi.service.messaging.annotations` packages.

The OSGi messaging specification is catered to provide a unified solution to accomodate the trending messaging solutions such as AMQP, MQTT etc. That's why no generic API exists for MQTT. To fill in this gap, an implementation specific API exists in `in.bytehue.messaging.mqtt5.api`.


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

#### Useful Notes

* Since more than one implementations can coexist in the OSGi runtime, we can search for the MQTT services by means of the provided service properties.
* Refer to the examples above.
* Also note that, the `in.bytehue.messaging.mqtt5.provider` bundle packages the APIs and implementation together. This bundle also packages and exports the HiveMQ Java client APIs to perform enhanced configuration to the client.
* For more details, have a look at the [example](https://github.com/amitjoy/osgi-messaging/tree/main/in.bytehue.messaging.mqtt5.example) project
