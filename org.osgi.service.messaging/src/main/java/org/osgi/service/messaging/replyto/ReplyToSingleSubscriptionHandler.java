package org.osgi.service.messaging.replyto;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.util.promise.Promise;

public interface ReplyToSingleSubscriptionHandler {

    /**
     * Creates a {@link Promise} for response {@link Message} for the incoming request {@link Message}.
     *
     * The promise will be resolved, as soon a the execution completed successfully.
     *
     * Errors during the handling are delegated using the promise fail, behavior
     * The response message context builder is pre-configured.
     *
     * Properties like the channel and correlation are already set correctly to the builder.
     *
     * @param requestMessage the {@link Message}
     * @param responseBuilder the builder for the response message
     * @return the response {@link Message}, must not be null
     */
    Promise<Message> handleResponse(Message requestMessage, MessageContextBuilder responseBuilder);

}