/*******************************************************************************
 * Copyright 2020 Amit Kumar Mondal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.provider;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageSubPubWithAcknowledgeTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MessagePublisher publisher;

    @Service
    private MessageSubscription subscriber;

    @Service
    private AcknowledgeMessageContextBuilder amcb;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Test
    public void test_handle_acknowledge_consumer() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.handleAcknowledge(m -> flag1.set(true))
                                    .messageContextBuilder()
                                    .channel(channel)
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

            flag2.set(true);
        });
        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    @Test
    public void test_handle_acknowledge_consumer_filter() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        final Consumer<Message> acknowledgeHandler = m -> flag1.set(true);
        launchpad.register(Consumer.class, acknowledgeHandler, "foo", "bar");

        // @formatter:off
        final Message message = amcb.handleAcknowledge("(foo=bar)")
                .messageContextBuilder()
                .channel(channel)
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

            flag2.set(true);
        });
        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    @Test
    public void test_handle_acknowledge_filter() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.filterAcknowledge(m -> { flag1.set(true); return flag1.get(); })
                                    .messageContextBuilder()
                                    .channel(channel)
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

            flag2.set(true);
        });
        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    @Test
    public void test_handle_acknowledge_service_filter() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        final Predicate<Message> predicate = m -> {
            flag1.set(true);
            return flag1.get();
        };
        launchpad.register(Predicate.class, predicate, "foo", "bar");

        // @formatter:off
        final Message message = amcb.filterAcknowledge("(foo=bar)")
                                    .messageContextBuilder()
                                    .channel(channel)
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

            flag2.set(true);
        });
        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    @Test
    public void test_handle_post_acknowledge_consumer() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();
        final AtomicBoolean flag3 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.filterAcknowledge(m -> { flag1.set(true); return flag1.get(); })
                                    .postAcknowledge(m -> flag3.set(true))
                                    .messageContextBuilder()
                                    .channel(channel)
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

            flag2.set(true);
        });
        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
        waitForRequestProcessing(flag3);
    }

    @Test
    public void test_handle_post_acknowledge_filter() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();
        final AtomicBoolean flag3 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        final Consumer<Message> acknowledgeConsumer = m -> flag3.set(true);
        launchpad.register(Consumer.class, acknowledgeConsumer, "foo", "bar");
        // @formatter:off
        final Message message = amcb.filterAcknowledge(m -> { flag1.set(true); return flag1.get(); })
                                    .postAcknowledge("(foo=bar)")
                                    .messageContextBuilder()
                                    .channel(channel)
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

            flag2.set(true);
        });
        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
        waitForRequestProcessing(flag3);
    }

    private static void waitForRequestProcessing(final AtomicBoolean flag) throws InterruptedException {
        await().atMost(3, TimeUnit.SECONDS).untilTrue(flag);
    }

}
