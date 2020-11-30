package in.bytehue.messaging.mqtt5.provider.helper;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.service.component.annotations.ComponentPropertyType;

@Target(TYPE)
@Retention(CLASS)
@ComponentPropertyType
public @interface GogoCommand {

    String PREFIX_ = "osgi.command.";

    /**
     * @return the scope used to disambiguate command functions
     */
    String scope();

    /**
     * @return the command functions provided by the service
     */
    String[] function();

}
