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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
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

    @Before
    public void setup() throws InterruptedException {
        waitForMqttConnectionReady(launchpad);
    }

    @Test
    public void test_reply_to_many_subscription_handler() throws Exception {
        final AtomicBoolean flag1 = new AtomicBoolean();
        final AtomicBoolean flag2 = new AtomicBoolean();

        final String channel = "ab/ba";
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

    @Test
    public void test_reply_to_many_subscription_handler_without_protocol_name_in_target_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

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
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_without_messaging_name_in_target_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.feature=replyTo))";

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
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_without_feature_in_target_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter))";

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
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_with_different_feature_in_target_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=abc))";

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
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_with_different_messaging_name_in_target_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=blahblah)(osgi.messaging.feature=replyTo))";

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
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_with_different_protocol_in_target_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=amqp)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

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
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_without_target_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String channelKey = "osgi.messaging.replyToSubscription.channel";
        final String[] channelValue = new String[] { channel };

        final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
        final String replyToChannelValue = replyToChannel;

        launchpad.register(ReplyToManySubscriptionHandler.class, handler, channelKey, channelValue, replyToChannelKey,
                replyToChannelValue);

        // @formatter:off
        final Message message = mcb.channel(channel)
                .contentType(contentType)
                .content(ByteBuffer.wrap(payload.getBytes()))
                .buildMessage();
        // @formatter:on

        subscriber.subscribe(replyToChannel).forEach(m -> {
            if (responsePyload.equals(new String(m.payload().array(), StandardCharsets.UTF_8))) {
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_without_channel_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

        final String replyToChannelKey = "osgi.messaging.replyToSubscription.replyChannel";
        final String replyToChannelValue = replyToChannel;

        launchpad.register(ReplyToManySubscriptionHandler.class, handler, targetKey, targetValue, replyToChannelKey,
                replyToChannelValue);

        // @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

        subscriber.subscribe(replyToChannel).forEach(m -> {
            if (responsePyload.equals(new String(m.payload().array(), StandardCharsets.UTF_8))) {
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

    @Test
    public void test_reply_to_many_subscription_handler_without_reply_to_channel_key() throws Exception {
        final String channel = "ab/ba";
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
                    try {
                        TimeUnit.MILLISECONDS.sleep(800);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    throw new AssertionError("Will never be executed");
                }
            }).start();
            source.endOfStream();
            return provider.createStream(source);
        };
        final String targetKey = "osgi.messaging.replyToSubscription.target";
        final String targetValue = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo))";

        final String channelKey = "osgi.messaging.replyToSubscription.channel";
        final String[] channelValue = new String[] { channel };

        launchpad.register(ReplyToManySubscriptionHandler.class, handler, targetKey, targetValue, channelKey,
                channelValue);

        // @formatter:off
        final Message message = mcb.channel(channel)
                                   .contentType(contentType)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();
        // @formatter:on

        subscriber.subscribe(replyToChannel).forEach(m -> {
            if (responsePyload.equals(new String(m.payload().array(), StandardCharsets.UTF_8))) {
                throw new AssertionError("Will never be executed");
            }
        });

        publisher.publish(message);
    }

}
