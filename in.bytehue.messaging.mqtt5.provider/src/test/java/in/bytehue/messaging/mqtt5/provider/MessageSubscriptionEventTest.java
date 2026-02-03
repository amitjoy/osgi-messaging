package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.messaging.MessageSubscription;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttSubAckDTO.Type;
import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionProvider.SubscriberConfig;

@RunWith(LaunchpadRunner.class)
public class MessageSubscriptionEventTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessageSubscription subscriber;

	@Service
	private ConfigurationAdmin configAdmin;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() {
		waitForMqttConnectionReady(launchpad);
	}

	@Test
	public void test_subscription_acked_event_is_sent() throws Exception {
		final String topic = "a/b/c";
		final List<Event> receivedEvents = new CopyOnWriteArrayList<>();

		final EventHandler handler = receivedEvents::add;
		final ServiceRegistration<EventHandler> registration = registerEventHandler(handler, "mqtt/subscription/*");

		try {
			subscriber.subscribe(topic);
			await().atMost(10, SECONDS).until(() -> !receivedEvents.isEmpty());

			assertThat(receivedEvents).hasSize(1);
			final Event event = receivedEvents.get(0);

			assertThat(event.getTopic()).isEqualTo("mqtt/subscription/ACKED");
			assertEventProperties(event, topic);
		} finally {
			registration.unregister();
		}
	}

	@Test
	public void test_subscription_failed_event_is_sent() throws Exception {
		final String topic = "a/#/b"; // invalid topic
		final List<Event> receivedEvents = new CopyOnWriteArrayList<>();

		final EventHandler handler = receivedEvents::add;
		final ServiceRegistration<EventHandler> registration = registerEventHandler(handler, "mqtt/subscription/*");

		try {
			subscriber.subscribe(topic);
		} catch (final RuntimeException e) {
			// expected
		}
		await().atMost(5, SECONDS).until(() -> !receivedEvents.isEmpty());
		registration.unregister();

		assertThat(receivedEvents).hasSize(1);
		final Event event = receivedEvents.get(0);

		assertThat(event.getTopic()).isEqualTo("mqtt/subscription/NO_ACK");
		assertEventProperties(event, topic, Type.NO_ACK);
	}

	@Test
	@Ignore("flaky")
	public void test_subscription_timeout_event_is_sent() throws Exception {
		final String topic = "a/b/c";
		final List<Event> receivedEvents = new CopyOnWriteArrayList<>();

		Optional<MessageSubscriptionProvider> service = launchpad.getService(MessageSubscriptionProvider.class);

		if (service.isPresent()) {
			service.get().stop();
			service.get().init(new SubscriberConfig() {

				@Override
				public Class<? extends Annotation> annotationType() {
					return null;
				}

				@Override
				public long timeoutInMillis() {
					return 1L;
				}

				@Override
				public int qos() {
					return 0;
				}

				@Override
				public long postSubAckDelayForClusterSync() {
					return 0;
				}

			});
		}

		final EventHandler handler = receivedEvents::add;
		final ServiceRegistration<EventHandler> registration = registerEventHandler(handler, "mqtt/subscription/*");

		try {
			subscriber.subscribe(topic);
		} catch (final RuntimeException e) {
			// expected
		}
		await().atMost(5, SECONDS).until(() -> !receivedEvents.isEmpty());
		registration.unregister();

		final Event event = receivedEvents.get(0);

		assertThat(event.getTopic()).isEqualTo("mqtt/subscription/NO_ACK");
		assertEventProperties(event, topic, Type.NO_ACK);
	}

	private ServiceRegistration<EventHandler> registerEventHandler(final EventHandler handler, final String... topics) {
		final Dictionary<String, Object> props = new Hashtable<>();
		props.put(EventConstants.EVENT_TOPIC, topics);
		return launchpad.getBundleContext().registerService(EventHandler.class, handler, props);
	}

	private void assertEventProperties(final Event event, final String topic) {
		assertEventProperties(event, topic, Type.ACKED);
	}

	private void assertEventProperties(final Event event, final String topic, final Type type) {
		assertThat(event.getProperty("topic")).isEqualTo(topic);
		assertThat(event.getProperty("type")).isEqualTo(type);
		assertThat(event.getProperty("qos")).isInstanceOf(Integer.class);
		assertThat(event.getProperty("replyTo")).isInstanceOf(Boolean.class);
		assertThat(event.getProperty("timestamp")).isInstanceOf(Long.class);
		assertThat(event.getProperty("reasonCodes")).isNotNull();
	}

}
