package org.osgi.service.messaging.dto;

import org.osgi.dto.DTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

/**
 * Represents a subscription for the reply to request DTO
 */
public class ReplyToSubscriptionDTO extends DTO {

	/**
	 * The DTO for the corresponding {@code Subscription} service. This value is
	 * never {@code null}.
	 */
	public ServiceReferenceDTO serviceDTO;

	/** DTO that describes the channel for the request subscription */
	public ChannelDTO requestChannel;

	/** DTO that describes the channel for publishing the response */
	public ChannelDTO responseChannel;

	/**
	 * The DTO of the registered handler, that executes the logic for this
	 * subscription
	 */
	public ServiceReferenceDTO handlerService;

	/** Flag that shows, if correlation id generation is active or not */
	public boolean generateCorrelationId;

	/** Flag that shows, if reply channel generation is active or not */
	public boolean generateReplyChannel;
	
	/** DTO that describes the quality of service for the subscription */
	public int qos;

}