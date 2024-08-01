package org.osgi.service.messaging.replyto;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.util.pushstream.PushStream;

/**
 * The {@code ReplyToManyPublisher} interface provides a way to publish a request
 * message and receive multiple asynchronous responses. It enables sending a request 
 * to a messaging system and obtaining a stream of responses via a {@link PushStream}.
 * This interface supports both simple message requests and requests with additional 
 * context for more detailed control over request and response handling.
 */
@ProviderType
public interface ReplyToManyPublisher {

	/**
	 * Publishes a request message and subscribes for multiple responses on a
	 * reply-to request. This method is similar to subscribing to a topic but 
	 * allows for multiple responses. The request message contains payload and 
	 * parameters, such as a correlation ID or response channel for request and 
	 * response setup.
	 *
	 * @param requestMessage the request message to be sent
	 * @return the {@link PushStream} that represents the stream of multiple responses
	 */
	PushStream<Message> publishWithReplyMany(Message requestMessage);

	/**
	 * Publishes a request message with additional context and subscribes for
	 * multiple responses. This method is similar to subscribing to a topic, but 
	 * it provides an opportunity to include a specific context for request and 
	 * response handling. The request message contains payload and parameters, 
	 * such as a correlation ID or response channel for request and response setup.
	 *
	 * @param requestMessage the request message to be sent
	 * @param replyToContext the context properties to use for request and response setup
	 * @return the {@link PushStream} that represents the stream of multiple responses
	 */
	PushStream<Message> publishWithReplyMany(Message requestMessage, MessageContext replyToContext);

}