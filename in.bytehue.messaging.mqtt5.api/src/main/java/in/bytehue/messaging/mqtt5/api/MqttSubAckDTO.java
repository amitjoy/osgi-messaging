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
	 * Type enum for clarity.
	 */
	public enum Type {
		ACKED, FAILED
	}

}
