package org.osgi.service.messaging;

import org.osgi.util.pushstream.PushStream;

public interface MessageSubscription {

    /**
     * Subscribe the {@link PushStream} to the given topic
     *
     * @param topic the topic string to subscribe to
     * @return a {@link PushStream} instance for the subscription
     */
    PushStream<Message> subscribe(String channel);

    /**
     * Subscribe the {@link PushStream} to the given topic with a certain quality of service
     *
     * @param topic the message topic to subscribe
     * @param context the optional properties in the context
     * @return a {@link PushStream} instance for the given topic
     */
    PushStream<Message> subscribe(MessageContext context);

}