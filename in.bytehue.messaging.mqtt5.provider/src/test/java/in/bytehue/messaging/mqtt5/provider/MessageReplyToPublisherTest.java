package in.bytehue.messaging.mqtt5.provider;

import static org.awaitility.Awaitility.await;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.replyto.ReplyToPublisher;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToPublisherTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MessagePublisher publisher;

    @Service
    private ReplyToPublisher replyToPublisher;

    @Service
    private MessageContextBuilder mcb;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Test
    public void test_publish_with_reply_1() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String reqChannel = "a/b";
        final String resChannel = "c/d";
        final String payload = "abc";

        // @formatter:off
        final Message message = mcb.channel(reqChannel)
                                   .replyTo(resChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        replyToPublisher.publishWithReply(message).onSuccess(m -> flag.set(true));

        final Message reqMessage = mcb.channel(reqChannel)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on

        publisher.publish(reqMessage);

        waitForRequestProcessing(flag);
    }

    @Test
    public void test_publish_with_reply_2() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String reqChannel = "a/b";
        final String resChannel = "c/d";
        final String payload = "abc";

        // @formatter:off
        final Message message = mcb.channel(reqChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        final MessageContext context = mcb.channel(reqChannel)
                                          .replyTo(resChannel)
                                          .buildContext();

        replyToPublisher.publishWithReply(message, context).onSuccess(m -> flag.set(true));

        final Message reqMessage = mcb.channel(reqChannel)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on

        publisher.publish(reqMessage);

        waitForRequestProcessing(flag);
    }

    private static void waitForRequestProcessing(final AtomicBoolean flag) throws InterruptedException {
        await().atMost(3, TimeUnit.SECONDS).untilTrue(flag);
    }

}
