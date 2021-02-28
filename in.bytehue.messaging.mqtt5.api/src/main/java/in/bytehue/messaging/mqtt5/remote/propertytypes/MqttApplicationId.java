package in.bytehue.messaging.mqtt5.remote.propertytypes;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * Component Property Type for MQTT Application.
 *
 * <ul>
 * <li>mqtt.application.id</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on a {@link Component} to declare the values of
 * the Message Feature.
 *
 * @see "Component Property Types"
 */
@ComponentPropertyType
public @interface MqttApplicationId {

    String value();

}
