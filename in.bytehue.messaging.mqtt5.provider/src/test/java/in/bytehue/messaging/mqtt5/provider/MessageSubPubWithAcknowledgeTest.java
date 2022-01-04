/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForRequestProcessing;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.ACKNOWLEDGED;
import static org.osgi.service.messaging.acknowledge.AcknowledgeType.REJECTED;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder;
import org.osgi.service.messaging.acknowledge.AcknowledgeType;

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

    @Before
    public void setup() throws InterruptedException {
        waitForMqttConnectionReady(launchpad);
    }

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

    @Test
    public void test_handle_acknowledge_with_filter_when_message_acknowledged() throws Exception {
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

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            final String topic = m.getContext().getChannel();
            final String ctype = m.getContext().getContentType();
            final String content = new String(m.payload().array(), UTF_8);
            final AcknowledgeType acknowledgeState = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeState();

            assertThat(channel).isEqualTo(topic);
            assertThat(payload).isEqualTo(content);
            assertThat(contentType).isEqualTo(ctype);
            assertThat(acknowledgeState).isEqualTo(ACKNOWLEDGED);

            flag2.set(true);
        });
        publisher.publish(message);
        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    @Test
    public void test_handle_acknowledge_with_filter_when_message_not_acknowledged() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.filterAcknowledge(m -> { flag.set(true); return false; })
                                    .messageContextBuilder()
                                    .channel(channel)
                                    .contentType(contentType)
                                    .content(ByteBuffer.wrap(payload.getBytes()))
                                    .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            final AcknowledgeType acknowledgeState = ((AcknowledgeMessageContext) context).getAcknowledgeState();
            assertThat(acknowledgeState).isEqualTo(REJECTED);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag);
    }

    @Test
    public void test_handle_acknowledge_without_filter_and_handler() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.messageContextBuilder()
                                    .channel(channel)
                                    .contentType(contentType)
                                    .content(ByteBuffer.wrap(payload.getBytes()))
                                    .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            flag.set(true);
            final AcknowledgeType acknowledgeState = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeState();
            assertThat(acknowledgeState).isEqualTo(ACKNOWLEDGED);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag);
    }

    @Test
    public void test_handle_acknowledge_without_filter_but_handler() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.handleAcknowledge(m -> {
                                        final AcknowledgeHandler h = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeHandler();
                                        h.acknowledge();
                                    })
                                    .messageContextBuilder()
                                    .channel(channel)
                                    .contentType(contentType)
                                    .content(ByteBuffer.wrap(payload.getBytes()))
                                    .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            flag.set(true);
            final AcknowledgeType acknowledgeState = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeState();
            assertThat(acknowledgeState).isEqualTo(ACKNOWLEDGED);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag);
    }

    @Test
    public void test_handle_acknowledge_with_filter_but_handler() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.filterAcknowledge(m -> flag1.compareAndSet(false, true))
                .messageContextBuilder()
                .channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            flag2.set(true);
            final AcknowledgeType acknowledgeState = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeState();
            assertThat(acknowledgeState).isEqualTo(ACKNOWLEDGED);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    @Test
    public void test_handle_acknowledge_with_filter_and_handler() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb.filterAcknowledge(m -> flag1.compareAndSet(false, true))
                .handleAcknowledge(m -> {
                    throw new AssertionError("Will never be executed");
                })
                .messageContextBuilder()
                .channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            flag2.set(true);
            final AcknowledgeType acknowledgeState = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeState();
            assertThat(acknowledgeState).isEqualTo(ACKNOWLEDGED);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
    }

    @Test
    public void test_handle_acknowledge_when_user_tried_to_acknowledge_message_when_already_rejected()
            throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb
                .handleAcknowledge(m -> {
                    final AcknowledgeHandler h = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeHandler();
                    assertThat(h.reject()).isTrue();
                    flag.compareAndSet(false, true);
                })
                .postAcknowledge(m -> {
                    final AcknowledgeHandler h = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeHandler();
                    assertThat(h.acknowledge()).isFalse();
                })
                .messageContextBuilder()
                .channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            final AcknowledgeMessageContext ctx = (AcknowledgeMessageContext) m.getContext();
            assertThat(ctx.getAcknowledgeState()).isEqualTo(REJECTED);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag);
        waitForRequestProcessing(flag);
    }

    @Test
    public void test_handle_acknowledge_when_user_tried_to_reject_message_when_already_acknowledged() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        // @formatter:off
        final Message message = amcb
                .handleAcknowledge(m -> {
                    final AcknowledgeHandler h = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeHandler();
                    assertThat(h.acknowledge()).isTrue();
                    flag.compareAndSet(false, true);
                })
                .postAcknowledge(m -> {
                    final AcknowledgeHandler h = ((AcknowledgeMessageContext) m.getContext()).getAcknowledgeHandler();
                    assertThat(h.reject()).isFalse();
                })
                .messageContextBuilder()
                .channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            final AcknowledgeMessageContext ctx = (AcknowledgeMessageContext) m.getContext();
            assertThat(ctx.getAcknowledgeState()).isEqualTo(ACKNOWLEDGED);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag);
        waitForRequestProcessing(flag);
    }

    @Test
    public void test_handle_acknowledge_order_of_execution_1() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();
        final AtomicBoolean flag3 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        final Predicate<Message> acknowledgeFilter = m -> flag1.compareAndSet(false, true);
        final Consumer<Message> acknowledgeHandler = m -> {
            throw new AssertionError("Will not be called since filter is set");
        };
        final Consumer<Message> postAcknowledgeConsumer = m -> {
            if (flag1.get()) {
                flag2.set(true);
            }
        };
        // @formatter:off
        final Message message = amcb.filterAcknowledge(acknowledgeFilter)
                                    .handleAcknowledge(acknowledgeHandler)
                                    .postAcknowledge(postAcknowledgeConsumer)
                                    .messageContextBuilder()
                                    .channel(channel)
                                    .contentType(contentType)
                                    .content(ByteBuffer.wrap(payload.getBytes()))
                                    .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            flag3.set(true);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
        waitForRequestProcessing(flag3);
    }

    @Test
    public void test_handle_acknowledge_order_of_execution_2() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();
        final AtomicBoolean flag3 = new AtomicBoolean();

        final String channel = "a/b";
        final String payload = "abc";
        final String contentType = "text/plain";

        final Consumer<Message> acknowledgeHandler = m -> flag1.compareAndSet(false, true);
        final Consumer<Message> postAcknowledgeConsumer = m -> {
            if (flag1.get()) {
                flag2.set(true);
            }
        };
        // @formatter:off
        final Message message = amcb.handleAcknowledge(acknowledgeHandler)
                                    .postAcknowledge(postAcknowledgeConsumer)
                                    .messageContextBuilder()
                                    .channel(channel)
                                    .contentType(contentType)
                                    .content(ByteBuffer.wrap(payload.getBytes()))
                                    .buildMessage();
        // @formatter:on

        final MessageContext context = message.getContext();
        subscriber.subscribe(context).forEach(m -> {
            flag3.set(true);
        });
        publisher.publish(message);

        waitForRequestProcessing(flag1);
        waitForRequestProcessing(flag2);
        waitForRequestProcessing(flag3);
    }

}
