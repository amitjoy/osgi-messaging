package org.osgi.service.messaging;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A service interface for publishing messages to specific topics.
 * This interface allows publishing messages with or without specifying a target topic
 * and provides flexibility to override message context settings.
 */
@ProviderType
public interface MessagePublisher {

    /**
     * Publishes the given {@link Message} to the topic specified in the message's context.
     *
     * @param message the {@link Message} to publish
     */
    void publish(Message message);

    /**
     * Publishes the given {@link Message} to the specified topic.
     *
     * @param message the {@link Message} to publish
     * @param channel the topic to publish the message to
     */
    void publish(Message message, String channel);

    /**
     * Publishes the given {@link Message} using the specified {@link MessageContext}.
     * This context parameter overrides any context information provided by the message itself.
     *
     * @param message the {@link Message} to publish
     * @param context the {@link MessageContext} to be used for publishing
     */
    void publish(Message message, MessageContext context);

}
