package in.bytehue.messaging.mqtt5.api;

import org.osgi.dto.DTO;

/**
 * MQTT SubAck message. This message is translated from and to an MQTT SUBACK packet.
 */
public class MqttSubAckDTO extends DTO {

	/**
	 * The optional reason string of this SubAck message
	 */
	public String reason;

	/**
	 * The Reason Code of this SubAck message, each belonging to a MQTT subscription
	 */
	public int[] reasonCodes;
	
}
