<img width="558" alt="logo" src="https://user-images.githubusercontent.com/13380182/99921124-a2e83200-2d28-11eb-92d3-b96295b841a9.png">

## OSGi Messaging Specification and compliant MQTT v5 Implementation

This repository comprises the rudimentary API extracted from the [OSGi RFC 246](https://github.com/osgi/design/blob/main/rfcs/rfc0246/rfc-0246-Messaging.pdf) specification and the MQTT v5 implementation of it. This spec is not at all the official version as it is simply based on the prelimary API from the current version of the spec. This is just an experimental repository for OSGi Messaging specification since the official version is not yet released. The official version is expected to be released with the release of OSGi Enterprise R8 specification in the second quarter of 2021 (as mentioned during the [EclipseCON talk](https://www.eclipsecon.org/2020/sessions/asychronous-communication-distributed-environments-new-osgi-messaging-rfc))

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

```java
@GogoCommand(function = { "pub", "sub" }, scope = "test")
@Component(service = Mqtt5Application.class, immediate = true)
public final class Mqtt5Application {

    @Reference
    private MessagePublisher publisher;

    @Reference
    private MessageSubscription subscriber;

    @Reference
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    public String sub(final String channel) {
        subscriber.subscribe(channel).forEach(m -> {
            System.out.println("Message Received");
            System.out.println(StandardCharsets.UTF_8.decode(m.payload()).toString());
        });
        return "Subscribed to " + channel;
    }

    public String pub(final String channel, final String data) {
        final MessageContextBuilder mcb = mcbFactory.getService();
        try {
            publisher.publish(mcb.content(ByteBuffer.wrap(data.getBytes())).channel(channel).buildMessage());
        } finally {
            mcbFactory.ungetService(mcb);
        }
        return "Published to " + channel;
    }

}
```
