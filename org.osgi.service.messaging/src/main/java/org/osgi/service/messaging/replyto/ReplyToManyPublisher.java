package org.osgi.service.messaging.replyto;

import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.util.pushstream.PushStream;

public interface ReplyToManyPublisher {

    /**
     * Subscribe for multiple responses on a reply-to request. This call is similar to the simple
     * subscription to a topic. This request message contains payload and parameters,
     * like e.g. correlation id or response channel for the request, response setup.
     *
     * @param requestMessage the request message
     * @return the {@link PushStream} for the answer stream
     */
    PushStream<Message> publishWithReplyMany(Message requestMessage);

    /**
     * Publish a request and await multiple answers for that request.
     *
     * This call is similar to the simple subscription on a
     * topic. This request message contains payload and parameters, like e.g. correlation id
     * or response channel for the request, response setup.
     *
     * @param requestMessage the request message
     * @param replyToContext the properties in the context for the request and response setup
     * @return the {@link PushStream} for the answer stream
     */
    PushStream<Message> publishWithReplyMany(Message requestMessage, MessageContext replyToContext);

}