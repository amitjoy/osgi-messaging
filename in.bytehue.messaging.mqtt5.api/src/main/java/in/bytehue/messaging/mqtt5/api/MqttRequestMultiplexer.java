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
package in.bytehue.messaging.mqtt5.api;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.util.promise.Promise;

/**
 * A centralized multiplexer for MQTT request-response interactions.
 * <p>
 * This service provides a high-level abstraction for performing
 * request-response operations over MQTT. Unlike standard Reply-To publishers,
 * this multiplexer maintains persistent, shared subscriptions (Master Streams)
 * for response topics, preventing the overhead of frequent
 * subscribe/unsubscribe cycles.
 * </p>
 * 
 * @since 1.1
 */
@ProviderType
public interface MqttRequestMultiplexer {

	/**
	 * Publishes a request message and returns a {@link Promise} for the response.
	 * <p>
	 * <b>Usage Example:</b>
	 * 
	 * <pre>
	 * // The Request must go to a SPECIFIC topic
	 * Message request = mcb.channel("my/request/topic")
	 *                      .replyTo("my/response/topic/123") // Specific reply topic
	 *                      .correlationId(UUID.randomUUID().toString())
	 *                      .content(payload)
	 *                      .buildMessage();
	 *
	 * // The Context defines the SHARED subscription (Wildcard allowed)
	 * MessageContext subCtx = mcb.channel("my/response/topic/#")
	 *                            .buildContext();
	 * *
	 *  multiplexer.request(request, subCtx)
	 *             .timeout(5000)
	 *             .onSuccess(response -> handle(response));
	 * </pre>
	 * 
	 * * @param request the {@link Message} to publish (must have correlationId).
	 * 
	 * @param subscriptionContext context defining the response subscription
	 *                            (wildcards allowed).
	 * @return a Promise resolving to the response message.
	 */
	Promise<Message> request(Message request, MessageContext subscriptionContext);
}