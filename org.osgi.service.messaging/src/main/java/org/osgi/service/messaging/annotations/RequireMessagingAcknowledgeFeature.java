package org.osgi.service.messaging.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.service.messaging.MessageConstants.ACKNOWLEDGE_IMPLEMENTATION;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_SPECIFICATION_VERSION;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;

/**
 * This annotation can be used to require the Messaging Acknowledge implementation. It
 * can be used directly, or as a meta-annotation.
 * <p>
 * This annotation is applied to several of the Messaging component
 * property annotations meaning that it does not normally need to be applied to
 * Declarative Services components which use the Messaging.
 */
@Documented
@Retention(CLASS)
@Target({ TYPE, PACKAGE })
@Requirement(namespace = IMPLEMENTATION_NAMESPACE, name = ACKNOWLEDGE_IMPLEMENTATION, version = MESSAGING_SPECIFICATION_VERSION)
public @interface RequireMessagingAcknowledgeFeature {
    // This is a marker annotation.
}
