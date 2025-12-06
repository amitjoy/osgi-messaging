package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MQTT_CONNECTION_READY_SERVICE_PROPERTY;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CONNECTION_READY;
import static org.osgi.service.condition.Condition.CONDITION_ID;

import org.osgi.service.component.AnyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

import in.bytehue.messaging.mqtt5.provider.helper.LogHelper;

//@formatter:off
@Component(
     immediate = true,
     configurationPid = CONNECTION_READY,
     property = {  
                  MQTT_CONNECTION_READY_SERVICE_PROPERTY + "=true",
                  CONDITION_ID + "=mqtt-ready" 
                }
)
//@formatter:on
public final class ConnectionReadyService implements Condition {

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private LogMirrorService logMirror;

	@Reference(service = AnyService.class, target = "(connection.ready.condition=true)")
	private Object connectionReadyCondition;

	private LogHelper logHelper;

	@Activate
	void activate() {
		logHelper = new LogHelper(logger, logMirror);
		logHelper.info("MQTT Connection is READY");
	}

	@Deactivate
	void deactivate() {
		logHelper.info("MQTT Connection is LOST");
	}
}
