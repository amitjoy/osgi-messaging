package org.osgi.service.messaging.annotations;

import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.service.messaging.MessagingConstants.ACKNOWLEDGE_IMPLEMENTATION;
import static org.osgi.service.messaging.MessagingConstants.MESSAGING_SPECIFICATION_VERSION;

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContextBuilder;

/**
 * Define messaging Acknowledge feature capability for a bundle.
 *
 * <p>
 * For example:
 *
 * <pre>
 * &#64;ProvideMessagingAcknowledgeFeature
 * </pre>
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests or otherwise process the package.
 */
@Capability(//
        name = ACKNOWLEDGE_IMPLEMENTATION, //
        namespace = IMPLEMENTATION_NAMESPACE, //
        version = MESSAGING_SPECIFICATION_VERSION, //
        uses = { Message.class, AcknowledgeMessageContextBuilder.class })
public @interface ProvideMessagingAcknowledgeFeature {

}
