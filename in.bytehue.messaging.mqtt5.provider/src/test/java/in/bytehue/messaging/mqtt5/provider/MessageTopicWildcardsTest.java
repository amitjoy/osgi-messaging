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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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
public final class MessageTopicWildcardsTest {

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
	public void test_multilevel_wildcard_hash_matching_multiple_levels() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final List<String> receivedTopics = new CopyOnWriteArrayList<>();

		final String prefix = "wildcard/test/" + UUID.randomUUID().toString();
		final String subFilter = prefix + "/#";

		final String topic1 = prefix + "/temp";
		final String topic2 = prefix + "/hvac/status";
		final String topic3 = prefix + "/sensors/floor1/room12/humidity";

		subscriber.subscribe(subFilter).forEach(m -> {
			receivedTopics.add(m.getContext().getChannel());
			if (receivedTopics.size() == 3) {
				flag.set(true);
			}
		});

		publisher.publish(newMessage(topic1, "val1"));
		publisher.publish(newMessage(topic2, "val2"));
		publisher.publish(newMessage(topic3, "val3"));

		waitForRequestProcessing(flag);

		assertThat(receivedTopics).containsExactlyInAnyOrder(topic1, topic2, topic3);
	}

	@Test
	public void test_multilevel_wildcard_hash_matches_exact_parent() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final List<String> receivedTopics = new CopyOnWriteArrayList<>();

		final String prefix = "wildcard/parent/" + UUID.randomUUID().toString();
		final String subFilter = prefix + "/#";

		// In MQTT, 'prefix/#' matches 'prefix' as well
		subscriber.subscribe(subFilter).forEach(m -> {
			receivedTopics.add(m.getContext().getChannel());
			flag.set(true);
		});

		publisher.publish(newMessage(prefix, "parent-content"));

		waitForRequestProcessing(flag);
		assertThat(receivedTopics).contains(prefix);
	}

	@Test
	public void test_singlelevel_wildcard_plus() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final List<String> receivedTopics = new CopyOnWriteArrayList<>();

		final String prefix = "wildcard/single/" + UUID.randomUUID().toString();
		final String subFilter = prefix + "/+/temperature";

		final String matchedTopic1 = prefix + "/kitchen/temperature";
		final String matchedTopic2 = prefix + "/bedroom/temperature";
		final String unmatchedTopic1 = prefix + "/kitchen/humidity";
		final String unmatchedTopic2 = prefix + "/livingroom/guest/temperature";

		subscriber.subscribe(subFilter).forEach(m -> {
			receivedTopics.add(m.getContext().getChannel());
			if (receivedTopics.size() == 2) {
				flag.set(true);
			}
		});

		publisher.publish(newMessage(matchedTopic1, "21.5"));
		publisher.publish(newMessage(unmatchedTopic1, "55%"));
		publisher.publish(newMessage(matchedTopic2, "19.0"));
		publisher.publish(newMessage(unmatchedTopic2, "22.0"));

		waitForRequestProcessing(flag);

		assertThat(receivedTopics).containsExactlyInAnyOrder(matchedTopic1, matchedTopic2);
		assertThat(receivedTopics).doesNotContain(unmatchedTopic1, unmatchedTopic2);
	}

	@Test
	public void test_combined_wildcards() throws Exception {
		final AtomicBoolean flag = new AtomicBoolean();
		final List<String> receivedTopics = new CopyOnWriteArrayList<>();

		final String prefix = "wildcard/combo/" + UUID.randomUUID().toString();
		final String subFilter = prefix + "/+/metrics/#";

		final String match1 = prefix + "/node1/metrics/cpu";
		final String match2 = prefix + "/node2/metrics/memory/heap";
		final String noMatch = prefix + "/node1/logs/error";

		subscriber.subscribe(subFilter).forEach(m -> {
			receivedTopics.add(m.getContext().getChannel());
			if (receivedTopics.size() == 2) {
				flag.set(true);
			}
		});

		publisher.publish(newMessage(match1, "cpu-ok"));
		publisher.publish(newMessage(noMatch, "error-log"));
		publisher.publish(newMessage(match2, "heap-ok"));

		waitForRequestProcessing(flag);

		assertThat(receivedTopics).containsExactlyInAnyOrder(match1, match2);
		assertThat(receivedTopics).doesNotContain(noMatch);
	}

	private Message newMessage(final String topic, final String payload) {
		final MessageContextBuilder mcb = launchpad.getService(MessageContextBuilder.class).get();
		return mcb.channel(topic).content(ByteBuffer.wrap(payload.getBytes(UTF_8))).buildMessage();
	}

}
