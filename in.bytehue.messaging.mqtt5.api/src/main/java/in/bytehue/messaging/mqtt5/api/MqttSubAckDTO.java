package in.bytehue.messaging.mqtt5.api;

import org.osgi.dto.DTO;

public class MqttSubAckDTO extends DTO {
	
	public String reason;

	public int[] reasonCodes;
	

}
