package org.osgi.service.messaging;

/**
 * Interface that provides builder methods for creating a {@link Message} 
 * or a {@link MessageContext} instance.
 */
public interface MessageContextProvider {

	/**
	 * Creates and returns a new {@link MessageContext} instance.
	 *
	 * @return a new message context instance
	 */
	MessageContext buildContext();

	/**
	 * Creates and returns a new {@link Message} instance that contains 
	 * the current context.
	 *
	 * @return a new message instance
	 */
	Message buildMessage();

}