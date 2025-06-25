package org.osgi.service.messaging.dto;

import org.osgi.dto.DTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

/** Represents a subscription instance DTO */
public class SubscriptionDTO extends DTO {

	/**
	 * The DTO for the corresponding {@code Subscription} service. This value is
	 * never {@code null}.
	 */
	public ServiceReferenceDTO serviceDTO;

	/** DTO that describes the channel for this subscription */
	public ChannelDTO channel;
	
	/** DTO that describes the quality of service for the subscription */
	public int qos;

}