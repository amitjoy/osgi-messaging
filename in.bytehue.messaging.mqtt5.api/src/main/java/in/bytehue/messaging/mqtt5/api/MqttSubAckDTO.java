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

import org.osgi.dto.DTO;

/**
 * MQTT SubAck DTO — represents MQTT 5.0 SUBACK status translated for events.
 */
public class MqttSubAckDTO extends DTO {

	/** Outcome type of this subscription. */
	public Type type;

	/** The MQTT topic this SubAck refers to (added for context). */
	public String topic;

	/** QoS requested for this subscription. */
	public int qos;

	/** Whether this was a reply-to subscription. */
	public boolean replyTo;

	/** Optional reason string from the SUBACK packet. */
	public String reason;

	/** Reason codes from the SUBACK packet (one per requested topic). */
	public int[] reasonCodes;

	/** Timestamp when this event was created. */
	public long timestamp;

	/**
	 * Represents the outcome of a subscription request, providing clarity on the
	 * status of the MQTT SUBACK packet.
	 */
	public enum Type {
		/**
		 * The subscription was successfully acknowledged by the broker.
		 * <p>
		 * This status is set when a SUBACK packet is received from the broker and
		 * contains a success reason code (e.g., {@code GRANTED_QOS_0},
		 * {@code GRANTED_QOS_1}, or {@code GRANTED_QOS_2}).
		 */
		ACKED,

		/**
		 * The broker explicitly rejected the subscription.
		 * <p>
		 * This status is set when a SUBACK packet is received but contains a failure
		 * reason code (e.g., {@code UNSPECIFIED_ERROR},
		 * {@code TOPIC_FILTER_INVALID}). This represents a definitive failure communicated
		 * by the broker.
		 */
		FAILED,

		/**
		 * No acknowledgement (SUBACK) was received from the broker.
		 * <p>
		 * This status is set when the client does not receive a SUBACK packet within
		 * the configured timeout period. This can occur due to a network issue, the
		 * broker being offline, or a client-side exception (e.g.,
		 * {@code TimeoutException}, {@code InterruptedException}) that prevents the
		 * acknowledgement from being processed.
		 */
		NO_ACK
	}

}
