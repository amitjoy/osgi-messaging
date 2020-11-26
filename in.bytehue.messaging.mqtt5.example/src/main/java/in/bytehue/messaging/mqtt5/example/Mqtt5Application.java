package in.bytehue.messaging.mqtt5.example;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.configurator.annotations.RequireConfigurator;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;

@RequireConfigurator
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