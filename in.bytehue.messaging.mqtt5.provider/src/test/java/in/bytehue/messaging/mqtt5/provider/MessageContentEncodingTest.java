/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
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

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageContentEncodingTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageSubscription subscriber;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_content_encoding_utf8_preservation() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<String> encodingRef = new AtomicReference<>();

		final String channel = "test/encoding/utf8/" + UUID.randomUUID().toString();
		final String payload = "test-payload-utf8";

		subscriber.subscribe(channel).forEach(m -> {
			encodingRef.set(m.getContext().getContentEncoding());
			flag.set(true);
		});

		final MessageContextBuilder mcb = launchpad.getService(MessageContextBuilder.class).get();
		final Message message = mcb.channel(channel)
				.contentEncoding("UTF-8")
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		waitForRequestProcessing(flag);

		assertThat(encodingRef.get()).isEqualTo("utf_8");
	}

	@Test
	public void test_content_encoding_null_by_default() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final AtomicReference<String> encodingRef = new AtomicReference<>();

		final String channel = "test/encoding/default/" + UUID.randomUUID().toString();
		final String payload = "test-payload-default";

		subscriber.subscribe(channel).forEach(m -> {
			encodingRef.set(m.getContext().getContentEncoding());
			flag.set(true);
		});

		final MessageContextBuilder mcb = launchpad.getService(MessageContextBuilder.class).get();
		final Message message = mcb.channel(channel)
				.content(ByteBuffer.wrap(payload.getBytes(UTF_8)))
				.buildMessage();

		publisher.publish(message);
		waitForRequestProcessing(flag);

		assertThat(encodingRef.get()).isNull();
	}

}
