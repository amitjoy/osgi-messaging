package org.osgi.service.messaging.replyto;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.util.pushstream.PushStream;

public interface ReplyToManySubscriptionHandler {

	/**
	 * Creates {@link PushStream} of response {@link Message}s for an incoming
	 * request {@link Message}. The response builder is pre-configured. Properties
	 * like the channel and correlation are already set correctly to the builder.
	 *
	 * @param requestMessage  the {@link Message}
	 * @param responseBuilder the builder for the response message
	 * @return the response {@link PushStream}, must not be null
	 */
	PushStream<Message> handleResponses(Message requestMessage, MessageContextBuilder responseBuilder);

}