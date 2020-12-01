package in.bytehue.messaging.mqtt5.provider;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageSubPubTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MessagePublisher publisher;

    @Service
    private MessageSubscription subscriber;

    @Service
    private MessageContextBuilder mcb;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Before
    public void setup() {
    }

    @After
    public void teardown() {
    }

    @Test
    public void test_sub_pub_with_1() throws Exception {
        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

        subscriber.subscribe(channel).forEach(m -> {
            final String topic = m.getContext().getChannel();
            final String ctype = m.getContext().getContentType();
            final String content = new String(m.payload().array(), UTF_8);

            assertThat(channel).isEqualTo(topic);
            assertThat(payload).isEqualTo(content);
            assertThat(contentType).isEqualTo(ctype);
        });

        waitForRequestProcessing();

        publisher.publish(message);

        waitForRequestProcessing();
    }

    @Test
    public void test_sub_pub_with_2() throws Exception {
        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

        subscriber.subscribe(message.getContext()).forEach(m -> {
            final String topic = m.getContext().getChannel();
            final String ctype = m.getContext().getContentType();
            final String content = new String(m.payload().array(), UTF_8);

            assertThat(channel).isEqualTo(topic);
            assertThat(payload).isEqualTo(content);
            assertThat(contentType).isEqualTo(ctype);
        });

        waitForRequestProcessing();

        publisher.publish(message);

        waitForRequestProcessing();
    }

    @Test
    public void test_sub_pub_with_3() throws Exception {
        final String channel = "a/b";
        final String inputChannel = "c/d";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

        subscriber.subscribe(inputChannel).forEach(m -> {
            final String topic = m.getContext().getChannel();
            final String ctype = m.getContext().getContentType();
            final String content = new String(m.payload().array(), UTF_8);

            assertThat(inputChannel).isEqualTo(topic);
            assertThat(payload).isEqualTo(content);
            assertThat(contentType).isEqualTo(ctype);
        });

        waitForRequestProcessing();

        // inputChannel has higher priority over message.getContext().getChannel()
        publisher.publish(message, inputChannel);

        waitForRequestProcessing();
    }

    @Test
    public void test_sub_pub_with_4() throws Exception {
        final String channel = "a/b";
        final String inputChannel = "c/d";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();

        final MessageContext messageContext = mcb.channel(inputChannel)
                .buildContext();
        // @formatter:on

        subscriber.subscribe(inputChannel).forEach(m -> {
            final String topic = m.getContext().getChannel();
            final String ctype = m.getContext().getContentType();
            final String content = new String(m.payload().array(), UTF_8);

            assertThat(inputChannel).isEqualTo(topic);
            assertThat(payload).isEqualTo(content);
            assertThat(contentType).isEqualTo(ctype);
        });

        waitForRequestProcessing();

        // messageContext has higher priority over message.getContext().getChannel()
        publisher.publish(message, messageContext);

        waitForRequestProcessing();
    }

    private static void waitForRequestProcessing() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
    }

}
