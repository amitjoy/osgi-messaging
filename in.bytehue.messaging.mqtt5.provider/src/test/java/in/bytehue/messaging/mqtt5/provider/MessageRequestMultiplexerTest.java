package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.TestHelper.waitForMqttConnectionReady;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.TimeoutException;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.MqttRequestMultiplexer;

@RunWith(LaunchpadRunner.class)
public final class MessageRequestMultiplexerTest {

	@Service
	private Launchpad launchpad;

	@Service
	private MessagePublisher publisher;

	@Service
	private MqttRequestMultiplexer multiplexer;

	@Service
	private ServiceReference<MessageContextBuilder> mcbRef;

	@SuppressWarnings("resource")
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

	@Before
	public void setup() throws InterruptedException {
		waitForMqttConnectionReady(launchpad);
	}

	/**
	 * Helper to obtain a FRESH instance of the prototype-scoped builder. This
	 * ensures that modifying the channel for one message doesn't affect previously
	 * built messages.
	 */
	private MessageContextBuilder getFreshBuilder() {
		BundleContext bundleContext = launchpad.getBundleContext();
		ServiceObjects<MessageContextBuilder> so = bundleContext.getServiceObjects(mcbRef);
		return so.getService();
	}

	@Test
	public void test_single_request_response() throws Exception {
		final String reqChannel = "request/single";
		final String resChannel = "response/single";
		final String correlationId = UUID.randomUUID().toString();
		final String payload = "test-payload";

		// 1. Build Request using a fresh builder
		final Message request = getFreshBuilder().channel(reqChannel).replyTo(resChannel).correlationId(correlationId)
				.content(ByteBuffer.wrap(payload.getBytes())).buildMessage();

		// 2. Build Context using a different fresh builder
		final MessageContext subCtx = getFreshBuilder().channel(resChannel).buildContext();

		final Promise<Message> promise = multiplexer.request(request, subCtx);

		// 3. Simulate remote response using another fresh builder
		final Message response = getFreshBuilder().channel(resChannel).correlationId(correlationId)
				.content(ByteBuffer.wrap("reply".getBytes())).buildMessage();
		publisher.publish(response);

		final Message result = promise.getValue();
		assertNotNull("Response should not be null", result);
		assertEquals(correlationId, result.getContext().getCorrelationId());
	}

	@Test
	public void test_concurrent_multiplexed_requests() throws Exception {
		final int requestCount = 5;
		final String reqChannel = "request/multi";
		final String resWildcard = "response/multi/#"; // Wildcard for SUBSCRIPTION
		final AtomicInteger successCount = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(requestCount);

		for (int i = 0; i < requestCount; i++) {
			final String correlationId = "id-" + i;
			final String specificResTopic = "response/multi/" + i; // Specific for PUBLICATION

			// 1. Build Request (Specific Channel) - Isolated Builder
			// This ensures the request object keeps "request/multi" as its topic
			final Message request = getFreshBuilder().channel(reqChannel).replyTo(specificResTopic)
					.correlationId(correlationId).buildMessage();

			// 2. Build Context (Wildcard Channel) - Isolated Builder
			// This prevents "response/multi/#" from overwriting the request's channel
			final MessageContext subCtx = getFreshBuilder().channel(resWildcard).buildContext();

			multiplexer.request(request, subCtx).onSuccess(m -> {
				successCount.incrementAndGet();
				latch.countDown();
			});

			// 3. Simulate Responder (Specific Channel) - Isolated Builder
			final Message response = getFreshBuilder().channel(specificResTopic).correlationId(correlationId)
					.buildMessage();
			publisher.publish(response);
		}

		latch.await(10, TimeUnit.SECONDS);
		assertEquals("All multiplexed requests should succeed", requestCount, successCount.get());
	}

	@Test
	public void test_request_timeout() throws Exception {
		// Use a unique topic to ensure no interference from previous test runs
		final String uniqueTopic = "timeout/" + UUID.randomUUID().toString();

		final Message request = getFreshBuilder().channel("request/timeout").replyTo(uniqueTopic)
				.correlationId(UUID.randomUUID().toString()).buildMessage();

		final MessageContext subCtx = getFreshBuilder().channel(uniqueTopic).buildContext();

		final Promise<Message> promise = multiplexer.request(request, subCtx).timeout(500);

		try {
			// This will block and then throw a wrapped TimeoutException because
			// no message is ever published to 'uniqueTopic'.
			promise.getValue();
			fail("Should have thrown an exception on timeout");
		} catch (Exception e) {
			// OSGi Promise wraps failure causes (usually in InvocationTargetException)
			Throwable cause = (e.getCause() != null) ? e.getCause() : e;
			assertEquals("Failure cause should be TimeoutException", TimeoutException.class, cause.getClass());
		}
	}
}