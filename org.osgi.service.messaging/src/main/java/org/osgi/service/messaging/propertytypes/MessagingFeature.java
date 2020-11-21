package org.osgi.service.messaging.propertytypes;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * Component Property Type for Messaging Feature.
 *
 * <ul>
 * <li>osgi.messaging.name</li>
 * <li>osgi.messaging.protocol</li>
 * <li>osgi.messaging.feature</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on a {@link Component} to declare the values of
 * the Message Feature.
 *
 * @see "Component Property Types"
 */
@ComponentPropertyType
public @interface MessagingFeature {

    String PREFIX_ = "osgi.messaging.";

    String name();

    String[] protocol();

    String[] feature() default {};

}
