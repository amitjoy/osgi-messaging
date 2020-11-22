package org.osgi.service.messaging.propertytypes;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * Component Property Type for Reply-To subscription.
 *
 * <ul>
 * <li>osgi.messaging.replyToSubscription.target</li>
 * <li>osgi.messaging.replyToSubscription.channel</li>
 * <li>osgi.messaging.replyToSubscription.replyChannel</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on a {@link Component} to declare the values of
 * the Reply-To subscription.
 *
 * @see "Component Property Types"
 */
@ComponentPropertyType
public @interface ReplyToSubscription {

    String PREFIX_ = "osgi.messaging.replyToSubscription.";

    String target();

    String[] channel();

    String replyChannel() default "";

}
