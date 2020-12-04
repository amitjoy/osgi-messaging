package org.osgi.service.messaging.annotations;

import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_SPECIFICATION_VERSION;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_IMPLEMENTATION;

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.replyto.ReplyToManyPublisher;

/**
 * Define messaging Reply-To-Many-Publish feature capability for a bundle.
 *
 * <p>
 * For example:
 *
 * <pre>
 * &#64;ProvideMessagingReplyToManyPublishFeature
 * </pre>
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests or otherwise process the package.
 */
@Capability(//
        name = REPLY_TO_IMPLEMENTATION, //
        namespace = IMPLEMENTATION_NAMESPACE, //
        version = MESSAGING_SPECIFICATION_VERSION, //
        attribute = "many:List<String>='publish,subscribe'", //
        uses = { Message.class, ReplyToManyPublisher.class })
public @interface ProvideMessagingReplyToManyFeature {

}
