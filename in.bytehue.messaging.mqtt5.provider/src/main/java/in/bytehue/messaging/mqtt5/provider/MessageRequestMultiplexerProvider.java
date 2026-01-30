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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MQTT_CONNECTION_READY_CONDITION;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_PUBLISH;
import static org.osgi.service.messaging.Features.REPLY_TO_MANY_SUBSCRIBE;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.pushstream.PushStream;

import in.bytehue.messaging.mqtt5.api.MqttRequestMultiplexer;

@Component
// @formatter:off
@MessagingFeature(
        name = MESSAGING_ID,
        protocol = MESSAGING_PROTOCOL,
        feature = {
                    REPLY_TO,
                    REPLY_TO_MANY_PUBLISH,
                    REPLY_TO_MANY_SUBSCRIBE
                  }
)
// @formatter:on
public final class MessageRequestMultiplexerProvider implements MqttRequestMultiplexer {

	@Reference
	private MessagePublisher publisher;

	@Reference
	private MessageSubscription subscriber;

	private final Lock lock = new ReentrantLock();
	private final Map<String, PushStream<Message>> masterStreams = new ConcurrentHashMap<>();
	private final Map<String, Deferred<Message>> pendingRequests = new ConcurrentHashMap<>();

	@Deactivate
	void deactivate() {
		clearStreams("Multiplexer is being deactivated");
	}

	// ========== MQTT CONNECTION LIFECYCLE ==========

	@Reference(target = MQTT_CONNECTION_READY_CONDITION, cardinality = OPTIONAL, policy = DYNAMIC)
	protected void bindMqttConnectionReadyCondition(Condition condition) {
	}

	protected void unbindMqttConnectionReadyCondition(Condition condition) {
		clearStreams("MQTT connection is NOT READY");
	}

	@Override
	public Promise<Message> request(final Message request, final MessageContext subscriptionContext) {
		requireNonNull(request, "Request message cannot be null");
		requireNonNull(subscriptionContext, "Subscription context cannot be null");

		final String correlationId = request.getContext().getCorrelationId();
		if (correlationId == null || correlationId.trim().isEmpty()) {
			throw new IllegalArgumentException("Correlation ID is missing in the request context");
		}

		final Deferred<Message> deferred = new Deferred<>();

		// Atomic Registration
		pendingRequests.put(correlationId, deferred);

		try {
			// Mark as reply-to subscription
			subscriptionContext.getExtensions().put(REPLY_TO, true);

			// Ensure Master Stream (Thread-Safe)
			ensureMasterStream(subscriptionContext.getChannel(), subscriptionContext);

			// Publish Request
			publisher.publish(request);
		} catch (final Exception e) {
			pendingRequests.remove(correlationId);
			deferred.fail(e);
			// Re-throw to inform the caller immediately of sync failures (e.g.
			// connectivity)
			throw e;
		}

		return deferred.getPromise();
	}

	private void ensureMasterStream(final String topic, final MessageContext context) {
		if (masterStreams.containsKey(topic)) {
			return;
		}
		lock.lock();
		try {
			if (!masterStreams.containsKey(topic)) {
				// Initialize the persistent stream
				final PushStream<Message> stream = subscriber.subscribe(context);

				// One-time terminal operation setup
				final Promise<Void> closedPromise = stream.forEach(msg -> {
					final String cid = msg.getContext().getCorrelationId();
					if (cid != null) {
						// Atomic remove-and-resolve
						final Deferred<Message> waiter = pendingRequests.remove(cid);
						if (waiter != null) {
							waiter.resolve(msg);
						}
					}
				});

				// Self-cleaning: If stream dies for ANY reason, remove it from the map
				closedPromise.onResolve(() -> {
					lock.lock();
					try {
						masterStreams.remove(topic, stream);
					} finally {
						lock.unlock();
					}
				});
				masterStreams.put(topic, stream);
			}
		} finally {
			lock.unlock();
		}
	}

	private void clearStreams(String message) {
		lock.lock();
		try {
			// Close all streams (will trigger the onResolve cleanup above, which is fine)
			masterStreams.values().forEach(stream -> {
				try {
					stream.close();
				} catch (Exception ignored) {
					// ignore
				}
			});
			masterStreams.clear();

			final Exception shutdownError = new IllegalStateException(message);
			pendingRequests.values().forEach(d -> d.fail(shutdownError));
			pendingRequests.clear();
		} finally {
			lock.unlock();
		}
	}

}