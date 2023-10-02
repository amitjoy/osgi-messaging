package in.bytehue.messaging.mqtt5.api;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * {@link MqttCommandExtension} is primarily used to provide more information
 * to be displayed in MQTT Gogo command.
 *
 * @since 1.0
 */
@ConsumerType
public interface MqttCommandExtension {

	/**
	 * Returns the row name to be displayed in the command
	 *
	 * @return the row name
	 */
	String rowName();

	/**
	 * Returns the row value to be disaplyed in the command
	 *
	 * @return the row value
	 */
	String rowValue();

}
