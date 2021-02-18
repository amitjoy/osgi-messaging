package in.bytehue.messaging.mqtt5.example.gogo;

import java.nio.ByteBuffer;

import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.replyto.ReplyToPublisher;

@GogoCommand(scope = "foo", function = "send")
@Component(service = MyReplyToPublisher.class)
public final class MyReplyToPublisher {

    @interface Config {
        String channel() default "channel_foo";

        String replyToChannel() default "channel_bar";
    }

    @Reference
    private ReplyToPublisher publisher;

    @Reference
    private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

    @Activate
    private Config config;

    public void send(final String value) {
        final MessageContextBuilder mcb = mcbFactory.getService();
        try {
            // @formatter:off
            final Message message = mcb.channel(config.channel())
                                       .replyTo(config.replyToChannel())
                                       .content(ByteBuffer.wrap(value.getBytes()))
                                       .buildMessage();
            // @formatter:on
            publisher.publishWithReply(message).onSuccess(System.out::println);
        } finally {
            mcbFactory.ungetService(mcb);
        }
    }

}
