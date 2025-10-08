package in.bytehue.messaging.mqtt5.api;

import org.osgi.dto.DTO;

/**
 * MQTT 5 SubAck message. This message is translated from and to an MQTT 5
 * SUBACK packet.
 */
public class MqttSubAckDTO extends DTO {

	/**
	 * The Reason Codes of this SubAck message, each belonging to a MQTT subscription
	 */
	public String reason;

	/**
	 * The optional reason string of this SubAck message
	 */
	public int[] reasonCodes;

}
