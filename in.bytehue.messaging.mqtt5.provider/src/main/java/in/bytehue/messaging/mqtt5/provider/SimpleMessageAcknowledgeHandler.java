package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.MessageConstants.MQTT_PROTOCOL;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;
import org.osgi.service.messaging.propertytypes.MessagingFeature;

@Component(service = { AcknowledgeHandler.class, SimpleMessageAcknowledgeHandler.class })
@MessagingFeature(name = "mqtt5-procol-specific-acknowledge-handler", protocol = MQTT_PROTOCOL)
public final class SimpleMessageAcknowledgeHandler implements AcknowledgeHandler {

    // TODO think about if we have any message restriction for this handler?
    @Override
    public boolean acknowledge() {
        return true;
    }

    @Override
    public boolean reject() {
        return false;
    }

}