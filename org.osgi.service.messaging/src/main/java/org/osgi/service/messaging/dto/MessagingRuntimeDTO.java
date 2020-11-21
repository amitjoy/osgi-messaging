package org.osgi.service.messaging.dto;

import org.osgi.dto.DTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

/** Represents the messaging instance DTO */
public class MessagingRuntimeDTO extends DTO {

    /**
     * The DTO for the corresponding {@code MessageServiceRuntime}.
     * This value is never {@code null}.
     */
    public ServiceReferenceDTO serviceDTO;

    /** The connection URI */
    public String connectionURI;

    /** Implementation provider name */
    public String providerName;

    /** The supported protocols */
    public String[] protocols;

    /** The instance id */
    public String instanceId;

    /** The set of features, that are provided by this implementation */
    public String[] features;

    /** DTO for all subscriptions */
    public SubscriptionDTO[] subscriptions;

    /** DTO for all reply-to subscriptions */
    public ReplyToSubscriptionDTO[] replyToSubscriptions;

}