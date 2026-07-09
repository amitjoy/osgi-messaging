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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.Message;
import org.osgi.util.pushstream.PushStream;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public final class MessageSubscriptionConditionTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessageSubscriptionProvider subscriber;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_subscription_condition_properties_update() throws Exception {
		// 1. Verify initial condition state
		await().atMost(3, SECONDS).until(() -> {
			ServiceReference<?>[] refs = launchpad.getBundleContext().getAllServiceReferences(null,
					"(osgi.condition.id=mqtt.subscription)");
			return refs != null && refs.length > 0;
		});
		ServiceReference<?> ref = launchpad.getBundleContext().getAllServiceReferences(null,
				"(osgi.condition.id=mqtt.subscription)")[0];

		assertThat(ref.getProperty("osgi.condition.id")).isEqualTo("mqtt.subscription");
		assertThat(ref.getProperty("#topic")).isEqualTo(0);
		assertThat(ref.getProperty("topic")).isNull();

		// 2. Subscribe to a topic
		final String channel1 = "test/topic/1";
		final PushStream<Message> stream1 = subscriber.subscribe(channel1);

		// Wait for properties to be updated
		await().atMost(3, SECONDS).until(() -> {
			ServiceReference<?>[] refs = launchpad.getBundleContext().getAllServiceReferences(null,
					"(osgi.condition.id=mqtt.subscription)");
			if (refs == null || refs.length == 0)
				return false;
			ServiceReference<?> r = refs[0];
			Integer count = (Integer) r.getProperty("#topic");
			if (count == null || count != 1) {
				System.out.println("Condition props: ");
				for (String key : r.getPropertyKeys()) {
					System.out.println(key + " = " + r.getProperty(key));
				}
			}
			return count != null && count == 1;
		});

		ref = launchpad.getBundleContext().getAllServiceReferences(null, "(osgi.condition.id=mqtt.subscription)")[0];
		String[] topics = (String[]) ref.getProperty("topic");
		assertThat(topics).containsExactly(channel1);
		assertThat(ref.getProperty("[unq]topic")).isEqualTo(1);

		// 3. Subscribe to another topic
		final String channel2 = "test/topic/2";
		final PushStream<Message> stream2 = subscriber.subscribe(channel2);

		await().atMost(3, SECONDS).until(() -> {
			ServiceReference<?>[] refs = launchpad.getBundleContext().getAllServiceReferences(null,
					"(osgi.condition.id=mqtt.subscription)");
			if (refs == null || refs.length == 0)
				return false;
			ServiceReference<?> r = refs[0];
			Integer count = (Integer) r.getProperty("#topic");
			return count != null && count == 2;
		});

		ref = launchpad.getBundleContext().getAllServiceReferences(null, "(osgi.condition.id=mqtt.subscription)")[0];
		topics = (String[]) ref.getProperty("topic");
		assertThat(topics).containsExactlyInAnyOrder(channel1, channel2);
		assertThat(ref.getProperty("[unq]topic")).isEqualTo(2);

		// 3.5 Subscribe to the SAME topic again to test [unq]topic
		final PushStream<Message> stream3 = subscriber.subscribe(channel1);
		
		await().atMost(3, SECONDS).until(() -> {
			ServiceReference<?>[] refs = launchpad.getBundleContext().getAllServiceReferences(null,
					"(osgi.condition.id=mqtt.subscription)");
			if (refs == null || refs.length == 0)
				return false;
			ServiceReference<?> r = refs[0];
			Integer count = (Integer) r.getProperty("#topic");
			return count != null && count == 3;
		});

		ref = launchpad.getBundleContext().getAllServiceReferences(null, "(osgi.condition.id=mqtt.subscription)")[0];
		topics = (String[]) ref.getProperty("topic");
		assertThat(topics).containsExactlyInAnyOrder(channel1, channel2, channel1);
		assertThat(ref.getProperty("[unq]topic")).isEqualTo(2);

		// 3.6 Test LDAP Filters for min, max, sum
		await().atMost(3, SECONDS).until(() -> {
			ServiceReference<?>[] refs = launchpad.getBundleContext().getAllServiceReferences(null,
					"(&(osgi.condition.id=mqtt.subscription)([max]qos>=0)([min]qos=0))");
			return refs != null && refs.length > 0;
		});

		// 4. Unsubscribe and verify it reverts
		stream1.close();
		stream2.close();
		stream3.close();

		await().atMost(3, SECONDS).until(() -> {
			ServiceReference<?>[] refs = launchpad.getBundleContext().getAllServiceReferences(null,
					"(osgi.condition.id=mqtt.subscription)");
			if (refs == null || refs.length == 0)
				return false;
			ServiceReference<?> r = refs[0];
			Integer count = (Integer) r.getProperty("#topic");
			return count != null && count == 0;
		});

		ref = launchpad.getBundleContext().getAllServiceReferences(null, "(osgi.condition.id=mqtt.subscription)")[0];
		assertThat(ref.getProperty("#topic")).isEqualTo(0);
		assertThat(ref.getProperty("topic")).isNull();
	}
}
