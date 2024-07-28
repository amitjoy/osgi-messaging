package org.osgi.service.messaging.acknowledge;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents the context for acknowledging a message in a messaging system.
 * 
 * <p>
 * This interface provides methods to access the state of message acknowledgment 
 * and the handler responsible for processing acknowledgment. Implementations 
 * of this interface are used to manage and track the acknowledgment status of messages.
 * </p>
 *
 * @since 1.0
 */
@ProviderType
public interface AcknowledgeMessageContext {

    /**
     * Returns the acknowledgment state of the message.
     * 
     * <p>
     * The acknowledgment state indicates whether a message has been acknowledged,
     * negatively acknowledged, or remains in an unacknowledged state. The returned
     * {@link AcknowledgeType} can provide this information, allowing the system
     * to handle messages accordingly.
     * </p>
     *
     * @return the {@link AcknowledgeType} representing the current state of the message
     *         acknowledgment; never {@code null}.
     */
    AcknowledgeType getAcknowledgeState();

    /**
     * Returns the acknowledgment handler associated with the message.
     * 
     * <p>
     * The acknowledgment handler is responsible for managing the acknowledgment process,
     * which may involve confirming message receipt, handling negative acknowledgments,
     * or performing other custom acknowledgment-related actions.
     * </p>
     *
     * @return the {@link AcknowledgeHandler} instance responsible for handling message 
     *         acknowledgment; never {@code null}.
     */
    AcknowledgeHandler getAcknowledgeHandler();

}