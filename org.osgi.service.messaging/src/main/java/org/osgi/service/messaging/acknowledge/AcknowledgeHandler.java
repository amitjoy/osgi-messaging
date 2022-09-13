package org.osgi.service.messaging.acknowledge;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Handler interface to acknowledge or reject a message.
 *
 * Messaging provider implementations use this interface to provide logic to
 * acknowledge a message using their underlying protocol.
 *
 * This interface is not meant to be implemented by users.
 */
@ProviderType
public interface AcknowledgeHandler {

	/**
	 * Acknowledge the message. The return values reflects, if the action was
	 * successful or not.
	 *
	 * A acknowledge can fail, if the message was already rejected.
	 *
	 * @return <code>true</code>, if acknowledge was successful
	 */
	boolean acknowledge();

	/**
	 * Reject the message. The return values reflects, if the action was successful
	 * or not.
	 *
	 * A rejection can fail, if the message was already acknowledged.
	 *
	 * @return <code>true</code>, if rejection was successful
	 */
	boolean reject();

}