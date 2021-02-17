/*******************************************************************************
 * Copyright 2021 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForRequestProcessing;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.replyto.ReplyToPublisher;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.api.MqttMessageCorrelationIdGenerator;

@RunWith(LaunchpadRunner.class)
public final class MessageReplyToPublisherCorrelationIdTest {

    @Service
    private Launchpad launchpad;

    @Service
    private MessagePublisher publisher;

    @Service
    private ReplyToPublisher replyToPublisher;

    @Service
    private MqttMessageContextBuilder mcb;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Test
    public void test_auto_generated_correlation_id_is_UUID() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean();

        final String reqChannel = "a/b";
        final String resChannel = "c/d";
        final String payload = "abc";

        // @formatter:off
        final Message message = mcb.channel(reqChannel)
                                   .replyTo(resChannel)
                                   .content(ByteBuffer.wrap(payload.getBytes()))
                                   .buildMessage();

        replyToPublisher.publishWithReply(message).onSuccess(m -> {
                final String correlationId = m.getContext().getCorrelationId();
                UUID.fromString(correlationId);
                flag.set(true);
            });

        final Message reqMessage = mcb.channel(reqChannel)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on

        publisher.publish(reqMessage);

        waitForRequestProcessing(flag);
    }

    @Test
    public void test_custom_generated_correlation_id() throws Exception {

        final String ID = "MY_CORRELATION_ID";
        final MqttMessageCorrelationIdGenerator gen = () -> ID;

        launchpad.register(MqttMessageCorrelationIdGenerator.class, gen, "custom_generator", "abc");

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
                                          .correlationIdGenerator("(custom_generator=abc)")
                                          .buildContext();

        replyToPublisher.publishWithReply(message, context).onSuccess(m -> {
            flag.set(true);
            final String correlationId = m.getContext().getCorrelationId();
            assertThat(correlationId).isEqualTo(ID);
            flag.set(true);
        });

        final Message reqMessage = mcb.channel(reqChannel)
                                      .content(ByteBuffer.wrap(payload.getBytes()))
                                      .buildMessage();
        // @formatter:on

        publisher.publish(reqMessage);

        waitForRequestProcessing(flag);
    }

}
