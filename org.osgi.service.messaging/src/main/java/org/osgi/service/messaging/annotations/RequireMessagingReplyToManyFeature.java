package org.osgi.service.messaging.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_SPECIFICATION_VERSION;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_IMPLEMENTATION;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;

/**
 * This annotation can be used to require the Messaging Reply-To-Many
 * implementation. It can be used directly, or as a meta-annotation.
 * <p>
 * This annotation is applied to several of the Messaging component property
 * annotations meaning that it does not normally need to be applied to
 * Declarative Services components which use the Messaging.
 */
@Documented
@Retention(CLASS)
@Target({ TYPE, PACKAGE })
@Requirement( //
		namespace = IMPLEMENTATION_NAMESPACE, //
		name = REPLY_TO_IMPLEMENTATION, //
		version = MESSAGING_SPECIFICATION_VERSION, //
		attribute = "many:List<String>='publish,subscribe'")
public @interface RequireMessagingReplyToManyFeature {
	// This is a marker annotation.
}
