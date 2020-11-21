package org.osgi.service.messaging.annotations;

import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.service.messaging.MessagingConstants.MESSAGING_IMPLEMENTATION;
import static org.osgi.service.messaging.MessagingConstants.MESSAGING_SPECIFICATION_VERSION;

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.messaging.Message;

/**
 * Define messaging feature capability for a bundle.
 *
 * <p>
 * For example:
 *
 * <pre>
 * &#64;ProvideMessagingFeature
 * </pre>
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests or otherwise process the package.
 */
@Capability(//
        name = MESSAGING_IMPLEMENTATION, //
        namespace = IMPLEMENTATION_NAMESPACE, //
        version = MESSAGING_SPECIFICATION_VERSION, //
        uses = Message.class)
public @interface ProvideMessagingFeature {

}
