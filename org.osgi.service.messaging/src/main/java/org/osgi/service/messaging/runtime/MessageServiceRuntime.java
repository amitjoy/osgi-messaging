package org.osgi.service.messaging.runtime;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;

/**
 * The MessageServiceRuntime service represents the runtime information of a
 * Message Service instance of an implementation.
 *
 * <p>
 * It provides access to DTOs representing the current state of the connection.
 */
@ProviderType
public interface MessageServiceRuntime {

	/**
	 * Return the messaging instance DTO containing the connection state and
	 * subscriptions
	 *
	 * @return the runtime DTO
	 */
	MessagingRuntimeDTO getRuntimeDTO();

}