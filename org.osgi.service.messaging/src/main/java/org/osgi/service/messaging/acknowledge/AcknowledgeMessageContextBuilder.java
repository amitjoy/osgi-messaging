package org.osgi.service.messaging.acknowledge;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;

/**
 * Builder for the {@link MessageContext} that is capable to handle direct
 * acknowledging/rejection of messages.
 *
 * This builder supports the programmatic way of declaring ack/reject behavior
 * as well a using services.
 */
@ProviderType
public interface AcknowledgeMessageContextBuilder {

	/**
	 * A consumer that is called to do custom acknowledge logic.
	 *
	 * It gets the {@link Message} as parameter. To trigger acknowledgement or
	 * rejection, callers can access the {@link AcknowledgeHandler} using the
	 * {@link AcknowledgeMessageContext} of the message.
	 *
	 * This handler is only called, if no filter configuration is provided.
	 *
	 * If postAcknowledge or/and postReject consumers are defined, they will be
	 * called after this handler.
	 *
	 * @param acknowledgeHandler the consumer that provides the {@link Message} as
	 *                           parameter
	 * @return the {@link AcknowledgeMessageContextBuilder} instance
	 */
	AcknowledgeMessageContextBuilder handleAcknowledge(Consumer<Message> acknowledgeHandler);

	/**
	 * A service with {@link java.util.function.Consumer} as interface and parameter
	 * type {@link Message}, that matches the provided target filter, is called to
	 * do custom acknowledge logic.
	 *
	 * It gets the {@link Message} as parameter. To trigger acknowledgement or
	 * rejection, callers can access the {@link AcknowledgeHandler} using the
	 * {@link AcknowledgeMessageContext} of the message.
	 *
	 * This handler is called after filtering, if provided.
	 *
	 * If postAcknowledge or/and postReject consumers are defined, they will be
	 * called after this handler.
	 *
	 * @param acknowledgeHandlerTarget the target filter for the consumer service
	 * @return the {@link AcknowledgeMessageContextBuilder} instance
	 */
	AcknowledgeMessageContextBuilder handleAcknowledge(String acknowledgeHandlerTarget);

	/**
	 * Defines a {@link Predicate} to test receiving message to either acknowledge
	 * or reject.
	 *
	 * If the test of the predicate is <code>true</code>, the message will be
	 * acknowledged, otherwise rejected using the implementation specific logic.
	 *
	 * If postAcknowledge or/and postReject consumers are set, they will be called
	 * after that.
	 *
	 * The filter is called before a handleAcknowledge definition, if provided.
	 *
	 * @param acknowledgeFilter the predicate to test the message
	 * @return the {@link AcknowledgeMessageContextBuilder} instance
	 */
	AcknowledgeMessageContextBuilder filterAcknowledge(Predicate<Message> acknowledgeFilter);

	/**
	 * A service with {@link java.util.function.Predicate} as interface and
	 * parameter type {@link Message}, that matches the provided target filter, is
	 * called to test receiving message to either acknowledge or reject.
	 *
	 * If the test of the predicate is <code>true</code>, the message will be
	 * acknowledged, otherwise rejected using the implementation specific logic.
	 *
	 * If postAcknowledge or/and postReject consumers are set, they will be called
	 * after that. The filter is called before a handleAcknowledge definition, if
	 * provided.
	 *
	 * @param acknowledgeFilterTarget the target filter for the Predicate service
	 * @return the {@link AcknowledgeMessageContextBuilder} instance
	 */
	AcknowledgeMessageContextBuilder filterAcknowledge(String acknowledgeFilterTarget);

	/**
	 * A consumer that is called to do custom logic after acknowledging messages.
	 *
	 * That consumer is called either, if a acknowledge filter returns
	 * <code>true</code> or a acknowledge handler calls the acknowledge method.
	 *
	 * Depending on the message's {@link AcknowledgeType}, this callback can handle
	 * acknowledgement or rejection.
	 *
	 * @param ackowledgeConsumer the consumer that handles post acknowledge
	 * @return the {@link AcknowledgeMessageContextBuilder} instance
	 */
	AcknowledgeMessageContextBuilder postAcknowledge(Consumer<Message> acknowledgeConsumer);

	/**
	 * A service with interface {@link Consumer} and parameter type {@link Message}
	 * is called, if the service properties matches the provided target filter.
	 *
	 * Then the consumer is called to do custom logic after acknowledging messages.
	 *
	 * That consumer is called either, if a acknowledge filter returns
	 * <code>true</code> or a acknowledge handler calls the acknowledge method.
	 *
	 * Depending on the message's {@link AcknowledgeType}, this callback can handle
	 * acknowledgement or rejection.
	 *
	 * @param ackowledgeConsumer the consumer that handles post acknowledge
	 * @return the {@link AcknowledgeMessageContextBuilder} instance
	 */
	AcknowledgeMessageContextBuilder postAcknowledge(String ackowledgeConsumerTarget);

	/**
	 * Returns the message context builder instance, to create a context or message
	 *
	 * @return the message context builder instance
	 */
	MessageContextBuilder messageContextBuilder();

}