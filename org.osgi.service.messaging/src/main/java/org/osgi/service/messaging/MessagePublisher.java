package org.osgi.service.messaging;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MessagePublisher {

	/**
	 * Publish the given {@link Message} to the given topic contained in the message
	 * context of the message
	 *
	 * @param message the {@link Message} to publish
	 */
	void publish(Message message);

	/**
	 * Publish the given {@link Message} to the given topic
	 *
	 * @param message the {@link Message} to publish
	 * @param channel the topic to publish the message to
	 */
	void publish(Message message, String channel);

	/**
	 * Publish the given {@link Message} using the given {@link MessageContext}. The
	 * context parameter will override all context information, that come with the
	 * message’s Message#getContext information
	 *
	 * @param message the {@link Message} to send
	 * @param context the {@link MessageContext} to be used
	 */
	void publish(Message message, MessageContext context);

}