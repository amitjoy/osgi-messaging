package org.osgi.service.messaging.replyto;

import org.osgi.service.messaging.Message;

/**
 * The {@code ReplyToSubscriptionHandler} interface defines a handler for processing
 * incoming response messages in a reply-to messaging pattern. Implementations of this 
 * interface are responsible for handling and processing the {@link Message} received 
 * as a response to a previously sent request.
 */
public interface ReplyToSubscriptionHandler {

	/**
	 * Handles the incoming response {@link Message}.
	 *
	 * @param requestMessage the {@link Message} to be processed
	 */
	void handleResponse(Message requestMessage);

}