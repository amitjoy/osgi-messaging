package org.osgi.service.messaging.replyto;

import org.osgi.service.messaging.Message;

public interface ReplyToSubscriptionHandler {

	/**
	 * Just handles the incoming request {@link Message}.
	 *
	 * @param requestMessage the {@link Message}
	 */
	void handleResponse(Message requestMessage);

}