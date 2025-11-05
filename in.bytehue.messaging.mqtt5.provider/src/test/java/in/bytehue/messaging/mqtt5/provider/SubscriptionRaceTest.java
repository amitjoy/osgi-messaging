package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.provider.helper.SubscriptionAck;

@RunWith(LaunchpadRunner.class)
public final class SubscriptionRaceTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessagePublisher publisher;

	@Service
	private MessageSubscriptionProvider subscriber;

	@Service
	private MessageSubscriptionRegistry registry;

	@Service
	private MessageContextBuilder mcb;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	/**
	 * This test is designed to be "flaky" if the race condition exists. It runs the
	 * problematic sequence in a loop. If it *ever* fails, it proves the race
	 * condition is real.
	 */
	@Test
	public void test_subscription_race_condition() throws Exception {
		final int ITERATIONS = 50;
		final String topic = "race/test/topic";
		final AtomicInteger successCount = new AtomicInteger(0);

		for (int i = 0; i < ITERATIONS; i++) {
			// Use a new latch for each iteration
			final CountDownLatch latch = new CountDownLatch(1);

			// 1. Subscribe (S1)
			final SubscriptionAck s1 = subscriber._subscribe(topic);

			// 2. Close S1's stream.
			// This synchronously removes S1 from the registry and
			// *asynchronously* schedules the UNSUBSCRIBE packet.
			s1.stream().close();

			// 3. Immediately Subscribe (S2) - This is the race!
			// We are trying to subscribe *after* the local registry is clear
			// but *before* the UNSUBSCRIBE packet is sent.
			final SubscriptionAck s2 = subscriber._subscribe(topic);
			s2.stream().forEach(msg -> {
				latch.countDown();
				successCount.incrementAndGet();
			});

			// 4. Publish a message to S2.
			final Message message = mcb.channel(topic).content(ByteBuffer.wrap(("test-" + i).getBytes()))
					.buildMessage();
			publisher.publish(message);

			// 5. Check if S2 received the message.
			// If the race occurred, the UNSUBSCRIBE from S1 killed S2's
			// subscription, and this will time out.
			final boolean received = latch.await(2, TimeUnit.SECONDS);

			// Clean up S2 for the next iteration
			s2.stream().close();

			// This assertion is the key: if it fails, we proved the race.
			assertThat(received).as("Race condition triggered: Subscriber failed to receive message on iteration " + i)
					.isTrue();

			// Short sleep to allow the S2 close to propagate
			TimeUnit.MILLISECONDS.sleep(50);
		}

		System.out.println("Race test completed. Successful receives: " + successCount.get() + "/" + ITERATIONS);
		assertThat(successCount.get()).isEqualTo(ITERATIONS);
	}

}