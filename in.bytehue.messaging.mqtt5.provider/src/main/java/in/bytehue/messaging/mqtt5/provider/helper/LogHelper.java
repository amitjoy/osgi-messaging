package in.bytehue.messaging.mqtt5.provider.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.osgi.service.log.Logger;

/**
 * It is a conscious decision to explicitly call the system property in every
 * call to ensure that we can dynamically update the system property in runtime
 * without reinitializing this instance. Otherwise, if we would have captured
 * the property value in a constructor, we won't be able to update it
 * dynamically. We would need to restart the bundle itself after setting the
 * property. This is why, the property is checked in every call directly.
 */
public final class LogHelper {

    private static final String MQTT_DEBUG_PROP = "mqtt.debug";
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Logger logger;

    public LogHelper(final Logger logger) {
        this.logger = logger;
    }

    public void debug(final String message, final Object... args) {
        logger.debug(message, args);
        if (Boolean.getBoolean(MQTT_DEBUG_PROP)) {
            System.out.println(String.format("%s [DEBUG] %s", getTimestamp(), format(message, args)));
        }
    }

    public void info(final String message, final Object... args) {
        logger.info(message, args);
        if (Boolean.getBoolean(MQTT_DEBUG_PROP)) {
            System.out.println(String.format("%s [INFO] %s", getTimestamp(), format(message, args)));
        }
    }

    public void warn(final String message, final Object... args) {
        logger.warn(message, args);
        if (Boolean.getBoolean(MQTT_DEBUG_PROP)) {
            System.out.println(String.format("%s [WARN] %s", getTimestamp(), format(message, args)));
        }
    }

    public void error(final String message, final Throwable t) {
        logger.error(message, t);
        if (Boolean.getBoolean(MQTT_DEBUG_PROP)) {
            System.err.println(String.format("%s [ERROR] %s", getTimestamp(), message));
            t.printStackTrace(System.err);
        }
    }

    public void error(final String message, final Object... args) {
        logger.error(message, args);
        if (Boolean.getBoolean(MQTT_DEBUG_PROP)) {
            System.err.println(String.format("%s [ERROR] %s", getTimestamp(), format(message, args)));
        }
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    private String format(String message, final Object... args) {
        if (message == null || args == null || args.length == 0) {
            return message;
        }
        final StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int lastIndex = 0;
        while (argIndex < args.length) {
            final int placeholderIndex = message.indexOf("{}", lastIndex);
            if (placeholderIndex == -1) {
                sb.append(message.substring(lastIndex));
                break;
            }
            sb.append(message, lastIndex, placeholderIndex);
            sb.append(args[argIndex++]);
            lastIndex = placeholderIndex + 2;
        }
        if (lastIndex < message.length()) {
            sb.append(message.substring(lastIndex));
        }
        return sb.toString();
    }

}
