package in.bytehue.messaging.mqtt5.provider;

import static org.awaitility.Awaitility.await;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToManyHandlerTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MessageContextBuilder mcb;

    @Service
    private MessagePublisher publisher;

    @Service
    private MessageSubscription subscriber;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Test
    public void test_reply_to_many_subscription_handler() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String replyToChannel = "c/d";
        final String payload = "abc";
        final String responsePyload = "test";
        final String contentType = "text/plain";

        final PushStreamProvider provider = new PushStreamProvider();
        final SimplePushEventSource<Message> source = provider.createSimpleEventSource(Message.class);

        final ReplyToManySubscriptionHandler handler = (m, b) -> {
            final Message message = b.content(ByteBuffer.wrap(responsePyload.getBytes())).buildMessage();
            new Thread(() -> {
                while (true) {
                    source.publish(message);
                    flag1.set(true);
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

        final String channelKey = "osgi.messaging.replyToSubscription.channel";
        final String[] channelValue = new String[] { channel };

        final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
        final String replyToChannelValue = replyToChannel;

        launchpad.register(ReplyToManySubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
                channelValue, replyToChannelKey, replyToChannelValue);

        // @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

        subscriber.subscribe(replyToChannel).forEach(m -> {
            if (responsePyload.equals(new String(m.payload().array(), StandardCharsets.UTF_8))) {
                flag2.set(true);
            }
        });

        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    private static void waitForRequestProcessing(final AtomicBoolean flag) throws InterruptedException {
        await().atMost(10, TimeUnit.SECONDS).untilTrue(flag);
    }

}
