package in.bytehue.messaging.mqtt5.provider;

import static org.awaitility.Awaitility.await;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToManyPublisherTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MessagePublisher publisher;

    @Service
    private ReplyToManyPublisher replyToPublisher;

    @Service
    private MqttMessageContextBuilder mcb;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Test(expected = RuntimeException.class)
    public void test_publish_with_reply_many_without_end_stream_filter() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String reqChannel = "a/b";
        final String resChannel = "c/d";
        final String payload = "abc";

        // @formatter:off
        final Message message = mcb.channel(reqChannel)
                                   .replyTo(resChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        replyToPublisher.publishWithReplyMany(message).forEach(m -> flag.set(true));

        final Message reqMessage = mcb.channel(reqChannel)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on

        publisher.publish(reqMessage);

        waitForRequestProcessing(flag);
    }

    @Test
    public void test_publish_with_reply_many() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String reqChannel = "a/b";
        final String resChannel = "c/d";
        final String payload = "abc";
        final String stopPayload = "stop";

        // @formatter:off
        final Message message = mcb.channel(reqChannel)
                                   .replyTo(resChannel)
                                   .withReplyToManyEndPredicate(m -> {
                                       final String content = new String(m.payload().array(), StandardCharsets.UTF_8);
                                       final boolean fl = content.equals(stopPayload);
                                       if (fl) {
                                           flag2.set(true);
                                       }
                                       return fl;
                                   })
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        replyToPublisher.publishWithReplyMany(message).forEach(m -> flag1.set(true));

        final Message reqMessage = mcb.channel(reqChannel)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();

        final MessageProvider stopMessage = new MessageProvider();
        stopMessage.byteBuffer = ByteBuffer.wrap(stopPayload.getBytes());
        stopMessage.messageContext = new MessageContextProvider();
        // @formatter:on

        publisher.publish(reqMessage);
        publisher.publish(reqMessage);
        publisher.publish(reqMessage);

        publisher.publish(stopMessage, reqChannel);

        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    private static void waitForRequestProcessing(final AtomicBoolean flag) throws InterruptedException {
        await().atMost(3, TimeUnit.SECONDS).untilTrue(flag);
    }

}
