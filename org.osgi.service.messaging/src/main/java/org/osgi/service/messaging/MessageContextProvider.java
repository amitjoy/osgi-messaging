package org.osgi.service.messaging;

/**
 * Interface to provide builder methods to create a {@link Message} or a
 * {@link MessageContext} instance
 */
public interface MessageContextProvider {

	/**
	 * Builds the message context
	 *
	 * @return the message context instance
	 */
	MessageContext buildContext();

	/**
	 * Builds the message with a containing context
	 *
	 * @return the message instance
	 */
	Message buildMessage();

}