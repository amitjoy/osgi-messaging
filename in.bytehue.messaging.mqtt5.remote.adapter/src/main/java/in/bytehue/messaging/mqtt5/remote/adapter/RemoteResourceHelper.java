package in.bytehue.messaging.mqtt5.remote.adapter;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.CLIENT_ID_FRAMEWORK_PROPERTY;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CLIENT;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.UUID;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.messaging.Message;

public final class RemoteResourceHelper {

    private RemoteResourceHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public enum MethodType {
        GET,
        POST,
        PUT,
        DELETE,
        EXEC
    }

    public static class RequestDTO extends DTO {
        String applicationId;
        String resource;
        MethodType method;
        Message requestMessage;
    }

    public static class MqttException extends RuntimeException {

        private static final long serialVersionUID = 4877572873981748364L;

        public final int code;

        public MqttException(final int code, final String message) {
            super(message);
            this.code = code;
        }

    }

    public static String clientID(final ConfigurationAdmin configurationAdmin, final BundleContext bundleContext) {
        try {
            final Configuration configuration = configurationAdmin.getConfiguration(CLIENT, "?");
            final Dictionary<String, Object> properties = configuration.getProperties();
            final Object clientId = properties.get("id");
            // check for the existence of configuration
            if (clientId == null) {
                // check for framework property if available
                final String id = bundleContext.getProperty(CLIENT_ID_FRAMEWORK_PROPERTY);
                // generate client ID if framework property is absent
                return id == null ? UUID.randomUUID().toString() : id;
            } else {
                return clientId.toString();
            }
        } catch (final IOException e) {
            // not gonna happen at all
        }
        return "+";
    }

    public static String exceptionToString(final Exception exception) {
        final StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
