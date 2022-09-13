package org.osgi.service.messaging.replyto;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.util.promise.Promise;

@ProviderType
public interface ReplyToPublisher {

	/**
	 * Subscribe for single response for a reply-to request
	 *
	 * @param requestMessage the request message
	 * @return the {@link Promise} that is resolved, when the answer message has
	 *         arrived
	 */
	Promise<Message> publishWithReply(Message requestMessage);

	/**
	 * Subscribe for single response for a reply-to request
	 *
	 * @param message        the request message
	 * @param replyToContext the optional properties in the context for the request
	 *                       and response channels
	 * @return the {@link Promise} that is resolved, when the answer has arrived
	 */
	Promise<Message> publishWithReply(Message requestMessage, MessageContext replyToContext);

}