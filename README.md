<p align="center">
  <img width="563" alt="logo" src="https://user-images.githubusercontent.com/13380182/101778008-90754300-3af3-11eb-95da-91c54608f277.png" />
</p>

## OSGi Messaging Specification and compliant MQTT 5.0 Implementation

This repository comprises the rudimentary API extracted from the [OSGi RFC 246](https://github.com/osgi/design/blob/main/rfcs/rfc0246/rfc-0246-Messaging.pdf) specification and the MQTT 5.0 implementation of it. This spec is not at all the official version as it is based on the preliminary API from the current version of the spec. This is just an experimental repository for OSGi Messaging specification since the official version is not yet released. The official version is expected to be released with the release of OSGi Enterprise R8 specification in the second quarter of 2021 (as mentioned during the [EclipseCON talk](https://www.eclipsecon.org/2020/sessions/asychronous-communication-distributed-environments-new-osgi-messaging-rfc))

Further to the above, there is a utility bundle comprising an easy-to-use functionality to manage remote resources or edge devices using MQTT 5.0.  For more details, have a look at the [Remote Resource (Edge Device) Management](#remote-resource-edge-device-management) section.

-----------------------------------------------------------------------------------------------------------

[![amitjoy - osgi-messaging](https://img.shields.io/static/v1?label=amitjoy&message=osgi-messaging&color=blue&logo=github)](https://github.com/amitjoy/osgi-messaging)
[![stars - osgi-messaging](https://img.shields.io/github/stars/amitjoy/osgi-messaging?style=social)](https://github.com/amitjoy/osgi-messaging)
[![forks - osgi-messaging](https://img.shields.io/github/forks/amitjoy/osgi-messaging?style=social)](https://github.com/amitjoy/osgi-messaging)
[![License - Apache](https://img.shields.io/badge/License-Apache-blue)](#license)
[![Build - Passing](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com/amitjoy/osgi-messaging/runs/1485969918)
[![GitHub release](https://img.shields.io/github/release/amitjoy/osgi-messaging?include_prereleases&sort=semver)](https://github.com/amitjoy/osgi-messaging/releases/)

------------------------------------------------------------------------------------------------------------

### Minimum Requirements

1. Java 8
2. OSGi R7

------------------------------------------------------------------------------------------------------------

### Modules

This project comprises three projects - 

|                  Bundle                     |                 Description                        |
|---------------------------------------------|----------------------------------------------------|
| `org.osgi.service.messaging`                | The Core OSGi Messaging Specification API          |
| `in.bytehue.messaging.mqtt5.api`            | The Extended MQTT 5 API                            |
| `in.bytehue.messaging.mqtt5.provider`       | The Core Specification Implementation              |
| `in.bytehue.messaging.mqtt5.remote.adapter` | Remote Resource (Edge Device) Management over MQTT |
| `in.bytehue.messaging.mqtt5.example`        | Example Project                                    |

---------------------------------------------------------------------------------------------------------------

### Installation

To use it in the OSGi environment, you need to install `in.bytehue.messaging.mqtt5.provider`. If you
want to make use of the remote device management, you also need to install `in.bytehue.messaging.mqtt5.remote.adapter`.

--------------------------------------------------------------------------------------------------------------

#### Project Import

**Import as Eclipse Projects**

1. Install Bndtools
2. Import all the projects (`File -> Import -> General -> Existing Projects into Workspace`)

--------------------------------------------------------------------------------------------------------------

#### Building from Source

Run `./gradlew clean build` in the project root directory

--------------------------------------------------------------------------------------------------------------

### Usage

#### Client Configuration

The `in.bytehue.messaging.client` PID can be used to configure the client. The configurable properties are as follows:

| Configuration                     | Description                                                                                                                                   | Type     | Default Value                |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------------------|
| `id`                              | Client Identifier. If empty, `in.bytehue.client.id` framework property  is checked and if unavailable, a random identifier will be generated. | String   |                              |
| `server`                          | Server Host Address                                                                                                                           | String   | `broker.hivemq.com`             |
| `port`                            | Server Port                                                                                                                                   | Long     | `1883`                       |
| `automaticReconnect`              | Custom Automatic Reconnection                                                                                                                        | Boolean  | `true`                      |
| `cleanStart`                      | Resume Previously Established Session                                                                                                         | Boolean  | `false`                      |
| `initialDelay`                    | Initial Delay if Custom Automatic Reconnection is enabled (In seconds)                                                                               | Long     | `10`                          |
| `maxDelay`                        | Max Delay if Custom Automatic Reconnection is enabled (In seconds)                                                                                   | Long     | `30`                         |
| `sessionExpiryInterval`           | Keep Session State (In seconds)                                                                                                               | Long     | `30`                         |
| `simpleAuth`                      | Simple Authentication                                                                                                                         | Boolean  | `false`                      |
| `username`                        | Simple Authentication Username                                                                                                                | String   |                              |
| `password`                        | Simple Authentication Password                                                                                                                | String   |                              |
| `useSSL`                          | SSL Configuration                                                                                                                             | Boolean  | `false`                      |
| `cipherSuites`                    | SSL Configuration Cipher Suites                                                                                                               | String[] |                              |
| `sslHandshakeTimeout`             | SSL Configuration Handshake Timeout (In seconds)                                                                                              | Long     | `1`                          |
| `trustManagerFactoryTargetFilter` | SSL Configuration Trust Manager Factory Service Target Filter Refer to `javax.net.ssl.TrustManagerFactory`                                    | String   |                              |
| `lastWillTopic`                   | Last Will Topic                                                                                                                               | String   |                              |
| `lastWillQoS`                     | Last Will QoS                                                                                                                                 | Integer  | `2`                          |
| `lastWillPayLoad`                 | Last Will Payload                                                                                                                             | String   |                              |
| `lastWillContentType`             | Last Will Content Type                                                                                                                        | String   |                              |
| `lastWillMessageExpiryInterval`   | Last Will Message Expiry Interval (In seconds)                                                                                                | Long     | `120`                        |
| `lastWillDelayInterval`           | Last Will Delay Interval (In seconds)                                                                                                         | Long     | `30`                         |
| `receiveMaximum`                  | Maximum concurrent messages to be received                                                                                                    | Integer  | `10`                         |
| `sendMaximum`                     | Maximum concurrent messages to be sent                                                                                                        | Integer  | `10`                         |
| `maximumPacketSize`               | Maximum Packet Size for receiving (In bytes)                                                                                                  | Integer  | `10240`                      |
| `sendMaximumPacketSize`           | Maximum Packet Size for sending (In bytes)                                                                                                    | Integer  | `10240`                      |
| `topicAliasMaximum`               | Maximum Topic Aliases                                                                                                                         | Integer  | `0`                          |
| `useWebSocket`                    | MQTT over Web Socket                                                                                                                          | Boolean  | `false`                      |
| `queryString`                     | Web Socket Query String                                                                                                                       | String   |                              |
| `serverPath`                      | Web Socket Server Path                                                                                                                        | String   |                              |
| `subProtocol`                     | Web Socket Sub Protocol                                                                                                                       | String   | `mqtt`                       |
| `webSocketHandshakeTimeout`       | Web Socket Handshake Timeout (In seconds)                                                                                                     | Long     | `10`                         |
| `useEnhancedAuthentication`       | Enhanced Authentication                                                                                                                       | Boolean  | `false`                      |
| `enhancedAuthTargetFilter`        | Enhanced Authentication Service Filter Refer to `com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism`                                | String   |                              |
| `useServerReauth`                 | Server Reauthentication                                                                                                                       | Boolean  | `false`                      |
| `connectedListenerFilter`         | Connected Listener Service Filter Refer to `com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener`                                     | String   |                              |
| `disconnectedListenerFilter`      | Disconnected Listener Service Filter Refer to `com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener`                               | String   |                              |
| `qos1IncomingInterceptorFilter`   | QoS 1 Incoming Interceptor Service Filter Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos1Interceptor`      | String   |                              |
| `qos2IncomingInterceptorFilter`   | QoS 2 Incoming Interceptor Service Filter Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos2Interceptor`      | String   |                              |
| `qos1OutgoingInterceptorFilter`   | QoS 1 Outgoing Interceptor Service Filter Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos1Interceptor`      | String   |                              |
| `qos2OutgoingInterceptorFilter`   | QoS 2 Outgoing Interceptor Service Filter Refer to `com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos2Interceptor`      | String   |                              |
| `condition.target`                | LDAP filter that needs to be satisfied for the client to be active Refer to `in.bytehue.messaging.mqtt5.api.TargetCondition`                  | String   | `(satisfy=always)`           |
| `disconnectionReasonDescription`  | Reason for the disconnection when the component is stopped                                                                                    | String   | `OSGi Component Deactivated` |
| `disconnectionReasonCode`         | Code for the disconnection when the component is stopped Refer to `com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode` | String   | `NORMAL_DISCONNECTION`       |

#### Reply To Publisher Internal Executor Configuration

The `in.bytehue.messaging.publisher` PID can be used to configure the internal thread pool

| Configuration      | Description                                                      | Type    | Default Value            |
|--------------------|------------------------------------------------------------------|---------|--------------------------|
| `numThreads`       | Number of Threads for the internal thread pool                   | Integer | `20`                     |
| `threadNamePrefix` | Prefix of the thread name                                        | String  | `mqtt-replyto-publisher` |
| `threadNameSuffix` | Suffix of the thread name  (supports only `%d` format specifier) | String  | `-%d`                    |
| `isDaemon`         | Flag to set if the threads will be daemon threads                | Boolean | `true`                   |

#### Primary Messaging APIs

| API                                                                       | Description                                                                                                                    |
|---------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `org.osgi.service.messaging.MessagePublisher`                             | service to publish to specific topic                                                                                           |
| `org.osgi.service.messaging.MessageSubscription`                          | service to subscribe to specific topic                                                                                         |
| `org.osgi.service.messaging.Message`                                      | a message that is exchanged between subscriber and publisher                                                                   |
| `org.osgi.service.messaging.MessageContext`                               | configuration for a specific message                                                                                           |
| `org.osgi.service.messaging.MessageContextBuilder`                        | service to prepare a specific message                                                                                          |
| `org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext`        | configuration denoting if a message has been acknowledged  (this requires the implementation to support `acknowledge` feature) |
| `org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder` | service to prepare a messaging configuration for acknowledgement                                                               |
| `org.osgi.service.messaging.replyto.ReplyToPublisher`                     | service to send a request for receiving a response                                                                             |
| `org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler`     | consumer API to handle response for a request and pusblish the response                                                        |
| `org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler`           | consumer API to handle only responses without publishing                                                                       |
| `org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler`       | consumer API to handle to handle stream of requests and publish stream  of responses                                           |
| `org.osgi.service.messaging.runtime.MessageServiceRuntime`                | service to respresent the runtime information of a Message Service  instance of an implementation                              |

The included APIs also provide several helpful annotations for implementors as well as consumers. Refer to `org.osgi.service.messaging.propertytypes` and `org.osgi.service.messaging.annotations` packages.

The OSGi messaging specification is catered to provide a unified solution to accomodate the trending messaging solutions such as AMQP, MQTT etc. That's why no generic API exists for MQTT. To fill in this gap, an implementation specific API exists in `in.bytehue.messaging.mqtt5.api`.

#### Secondary Messaging APIs

| API                                                                | Description                                                                                                                                                                                                                                                      |
|--------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder`         | an extended service of `org.osgi.service.messaging.MessageContextBuilder`  that could be used to prepare MQTT 5.0 specific message context                                                                                                                       |
| `in.bytehue.messaging.mqtt5.api.TargetCondition`                   | marker service interface which consumers can implement to provide services  with properties that can be used as conditional target to the MQTT client.  That means, consumer can provide filters that should be satisfied before MQTT  client is up and running. |
| `in.bytehue.messaging.mqtt5.api.MqttMessageCorrelationIdGenerator` | service interface to be implemented by consumers to provide the functionality  for generating correlation identifiers required for reply-to channels                                                                                                             |

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

* Note that, the connection to the MQTT broker can be terminated anytime due to network issues. In such a case, you can track the availability of a connection to the broker using an OSGi service which gets registered if the connection to the broker is maintained. The service will disappear if the connection gets broken. This service contains `mqtt.connection.ready` property that is set to `true`. Also note that, the service is exported under `TargetCondition` marker interface (Refer to `Target Condition Satisfiability for MQTT client` below)
* Since more than one implementations can coexist in the OSGi runtime, we can search for the MQTT services by means of the provided service properties.
* Refer to the examples above.
* Also note that, the `in.bytehue.messaging.mqtt5.provider` bundle packages the APIs and implementation together. This bundle also packages and exports the HiveMQ Java client APIs to perform enhanced configuration to the client.
* For more details, have a look at the [example](https://github.com/amitjoy/osgi-messaging/tree/main/in.bytehue.messaging.mqtt5.example) project

#### Target Condition Satisfiability for MQTT client

In certain circumstances the MQTT client requires few services to be up and running before the client is connected to the broker. For example, you could provide the following services for the client to use before it connects to the server:

* `javax.net.ssl.TrustManagerFactory`
* `in.bytehue.messaging.mqtt5.api.MqttMessageCorrelationIdGenerator`
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

| Value      | Description                                    |
|------------|------------------------------------------------|
| `#key`     | Calculates the number of key properties        |
| `[min]key` | Calculates the minimum of all key properties   |
| `[max]key` | Calculates the maximum of all key properties   |
| `[sum]key` | Calculates the sum of all key properties       |
| `[avg]key` | Calculates the average of all key properties   |
| `[unq]key` | Calculates the number of unique key properties |

This will ensure that your services will be up and running before the client gets activated. This also guarantees that the start order of the bundles is not at all required in this scenario.

--------------------------------------------------------------------------------------------------------------

### Remote Resource (Edge Device) Management

This comprises the guidelines to structure your MQTT topic namespace for managing the remote resources or edge devices using MQTT. 

The remote resources can receive two different types of requests:

* Command to perform an action (or popularly known as `Request/Response` pattern)
* Unsolicited events when the remote resource or edge device reports status of something periodically

#### MQTT Request Response Communication

In MQTT 5.0, adding a response topic to the MQTT publish request enables the subscriber to reply to that specific topic. But there is no such standard or practice that has been advised by the MQTT specification. It is, therefore, left to the user to introduce own convention to follow.

In this section, I will propose an efficient yet flexible way of performing request-response communication using MQTT 5.0.

A publisher or popularly known as the requester can send a MQTT payload to the topic conforming to the following pattern:

`control-topic-prefix/control-topic/client-id/application-id/method/resource-id`

for example,

* `CTRL/com/company/ABCD-1234/CONF-V1/GET/configurations`
* `CTRL/com/company/ABCD-1234/CONF-V1/PUT/configurations/a.b.c.d`
* `CTRL/com/company/ABCD-1234/APP-V1/GET/sensors/temperature`
* `CTRL/com/company/ABCD-1234/COMMAND-V1/EXEC/watchdog/monitor`

Let's first discuss the pattern mentioned above to understand the workflow better:

| Pattern                | Description                                                                                                                                                                                                                                                                                                                          |
|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `control-topic-prefix` | Topic prefix to be used in the beginning of a topic for remote  resource management. By default, it is configured to `CTRL`. It can also be configured to anything. Refer to `in.bytehue.messaging.mqtt5.remote` configuration. The recommended practice would be to use a **single word** with all in upper case. |
| `control-topic`        | Topic that would be appended to the `control-topic-prefix`. As an example, this can be `com/company/project`. It is also configurable in the same configuration as mentioned above. By default, it is set to `in/bytehue`.                                                                                                        |
| `client-id`            | MQTT client identifier                                                                                                                                                                                                                                                                                                               |
| `application-id`       | MQTT application running on the edge device that we want to access remotely.  To support multiple versions of the application, it is recommended that a version number be assigned with the `application-id` (e.g., `CONF-V1`, `CONF-V2`, etc.).                                                                                      |
| `method`               | A specific operation we want to perform on the remote application. An application in the remote device supports different types of methods, such as, `GET`, `POST`, `PUT`, `DELETE`  and `EXEC`                                                                                                                                     |
| `resource-id`          | The remainder of the total topic, for example, in `CTRL/com/company/ABCD-1234/CONF-V1/PUT/configurations/a.b.c.d` topic, `configurations/a.b.c.d` is the `resource-id`.                                                                                                                                                             |

#### Read Resources

A requester can read resources from the remote edge device by sending a `GET` MQTT request to the following topic pattern:

`control-topic-prefix/control-topic/client-id/application-id/GET/resource-id`

For example,

* `CTRL/com/company/ABCD-1234/CONF-V1/GET/configurations`
* `CTRL/com/company/ABCD-1234/CONF-V1/GET/bundles`
* `CTRL/com/company/ABCD-1234/APP-V1/GET/sensors/dining_room/temperature`

#### Create Resources

Creating resources can be done by sending a `PUT` request to the following topic pattern:

`control-topic-prefix/control-topic/client-id/application-id/PUT/resource-id`

For example,

* `CTRL/com/company/ABCD-1234/CONF-V1/PUT/configurations/c.d.e.f`

#### Update Resources

Updating resources can be achieved by sending a `POST` request to the following topic pattern:

`control-topic-prefix/control-topic/client-id/application-id/POST/resource-id`

For example,

* `CTRL/com/company/ABCD-1234/CONF-V1/POST/configurations/c.d.e.f`
* `CTRL/com/company/ABCD-1234/APP-V1/POST/sensors/bedroom/temperature`

#### Delete Resources

Similarly, any requester can delete resources on the remote edge device by sending a `DELETE` request to the following topic pattern:

`control-topic-prefix/control-topic/client-id/application-id/DELETE/resource-id`

for example,

* `CTRL/com/company/ABCD-1234/CONF-V2/DELETE/configurations/c.d.e.f`
* `CTRL/com/company/ABCD-1234/CONF-V2/DELETE/nodes/my_node`

#### Execute Resources

You can also execute remote resources by sending a `EXEC` request to the following topic pattern:

`control-topic-prefix/control-topic/client-id/application-id/EXEC/resource-id`

for example,

* `CTRL/com/company/ABCD-1234/COMMAND-V1/EXEC/ifconfig`
* `CTRL/com/company/ABCD-1234/DEPLOY-V2/EXEC/com.company.bundle/start`
* `CTRL/com/company/ABCD-1234/DEPLOY-V2/EXEC/threads/my_thread/watch`

#### Important Things to Remember

1. The receiving or subscribing application would always reply to the `reply to` address with any kind of content (or popularly known as `payload`) that both the parties (publisher and subscriber) understand. It can be (de)/serialized using **Protobuf** or **JSON** or **XML**, or any other serializer.

The response contains the content if available. In addition, there also exist some other **user properties** that denote the status of the response:

The following properties will be available in the **user properties** of the MQTT response:

* `response.code` - The available response codes are: 

| Response Code | Response String             |
|---------------|-----------------------------|
| `200`         | `RESPONSE_CODE_OK`          |
| `400`         | `RESPONSE_CODE_BAD_REQUEST` |
| `404`         | `RESPONSE_CODE_NOT_FOUND`   |
| `500`         | `RESPONSE_CODE_ERROR`       |

* `response.exception.message` - Optional exception message or the string version of the exception itself if there is no message available

2. The requester should always provide a `correlation ID` in the request message
3. The remote resource or the responder (subscriber) doesn't care about the `correlation ID` at all since the correlation ID will be mapped automatically when replying back to the requester
4. The remote resource or the responder (subscriber) remains always data agnostic and just cares about the resource that needs to accessed by the requester and the request message payload (Refer to the example below)
5. The response codes and messages **must never** be added by the remote resource application explicitly. It will automatically be done internally while replying to the requester.
6. It is recommended that the requester sets a timeout to the requested message to control the amount of time that it waits for a response from the remote resource or edge device. If a response is not received within the timeout interval, the server can expect that either the edge device or the resource is offline.
7. It is recommended to **never** use the control topic for unsolicited events where the remote resource or edge device periodically sends updates

#### MQTT Application on Remote Device

Any remote device can introduce MQTT application to leverage the remote resource management functionality.

You just need to implement `in.bytehue.messaging.mqtt5.remote.api.MqttApplication`.

#### Example

```java
@Component
@MqttApplicationId("APP-V1")
public final class MyMqttApplicationExample implements MqttApplication {

    @Override
    public Message doGET(
            final String resource,
            final Message requestMessage, 
            final MessageContextBuilder messageBuilder) throws Exception {
        return messageBuilder.content(ByteBuffer.wrap("RESPONSE".getBytes())).buildMessage();
    }

    @Override
    public Message doPUT(
            final String resource,
            final Message requestMessage, 
            final MessageContextBuilder messageBuilder) throws Exception {
        final String[] res = resource.split("/");
        if (res[0].equalsIgnoreCase("my-apps")) {
            String resourceId = res[1];
            String status = udpateResource();
            return messageBuilder.content(ByteBuffer.wrap(status.getBytes())).buildMessage();
        }
        throw new IllegalStateException("Specified resource cannot be updated");
    }

    @Override
    public Message doEXEC(
            final String resource,
            final Message requestMessage, 
            final MessageContextBuilder messageBuilder) throws Exception {
        final String[] res = resource.split("/");
        if (res[0].equalsIgnoreCase("my-commands")) {
            String command = res[1];
            String response = executeCommand(command);
            return messageBuilder.content(ByteBuffer.wrap(response.getBytes())).buildMessage();
        }
        throw new IllegalStateException("Specified command cannot be executed");
    }
    
    private static String updateResource(String resource) {
        .....
    }

    private static String executeCommand(String resource) {
        .....
    }

}
```

The following is the API of `in.bytehue.messaging.mqtt5.remote.api.MqttApplication`:

```java
public interface MqttApplication {

    /**
     * The service property denoting the application identifier
     */
    String APPLICATION_ID_PROPERTY = "mqtt.application.id";

    /**
     * Used to implement a READ request for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doGET(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to implement a CREATE request for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doPUT(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to implement an UPDATE request for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doPOST(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to implement a DELETE request for a resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doDELETE(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to perform application operation not necessary tied to a given resource
     *
     * @param resource the resource identifier
     * @param requestMessage the received message
     * @param messageBuilder the builder to build the response message
     *
     * @return the response to be provided back as {@link Message}
     * @throws Exception
     *             An exception is thrown in every condition where the request cannot be full fitted due to wrong
     *             request parameters or exceptions during processing
     */
    default Message doEXEC(
            final String resource,
            final Message requestMessage,
            final MqttMessageContextBuilder messageBuilder) throws Exception {
        throw new UnsupportedOperationException();
    }

}
```

It is consumer's sole responsibility which type of functionality to provide. If an application is only required to retrieve resources, it should only implement `doGET(..)` and if an application decides to create resources and execute commands on the resources, it should implement `doPUT(..)` and `doEXEC(..)` and so on. I believe you got the idea 😉

--------------------------------------------------------------------------------------------------------------

#### Remote Resource Management Configuration

The `in.bytehue.messaging.mqtt5.remote` PID is used to provide the necessary configurations for remote resource management.

| Configuration        | Description                                                 | Type   | Default Value |
|----------------------|-------------------------------------------------------------|--------|---------------|
| `controlTopicPrefix` | The control topic prefix for the remote resource management | String | `CTRL`        |
| `controlTopic`       | The control topic for the remote resource management        | String | `in/bytehue`  |

--------------------------------------------------------------------------------------------------------------

### Developer

Amit Kumar Mondal (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

### Contribution [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgi-messaging/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/osgi-messaging/blob/master/CONTRIBUTING.md)

--------------------------------------------------------------------------------------------------------------

### License

This project is licensed under Apache License Version 2.0 [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
