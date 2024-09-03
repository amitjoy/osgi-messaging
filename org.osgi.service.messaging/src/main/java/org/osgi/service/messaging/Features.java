package org.osgi.service.messaging;

/**
 * Interface defining constants for various features and extensions
 * of the Messaging specification.
 *
 * These constants represent the names of specific features and extensions 
 * that can be utilized within the messaging framework, providing a standard 
 * way to refer to them in code.
 *
 * @since 1.0
 */
public interface Features {

	/**
	 * The name of the the {@code Message Context Builder} feature of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String MESSAGE_CONTEXT_BUILDER = "messageContextBuilder";

	/**
	 * The name of the the {@code Reply-To} feature of the Messaging specification
	 *
	 * @since 1.0
	 */
	String REPLY_TO = "replyTo";

	/**
	 * The name of the the {@code Reply-To-May-Publish} feature of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String REPLY_TO_MANY_PUBLISH = "replyToManyPublish";

	/**
	 * The name of the the {@code Reply-To-May-Subscribe} feature of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String REPLY_TO_MANY_SUBSCRIBE = "replyToManySubscribe";

	/**
	 * The name of the the {@code Correlation ID Generation} feature of the
	 * Messaging specification
	 *
	 * @since 1.0
	 */
	String GENERATE_CORRELATION_ID = "generateCorrelationId";

	/**
	 * The name of the the {@code Reply Channel Generation} feature of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String GENERATE_REPLY_CHANNEL = "generateReplyChannel";

	/**
	 * The name of the the {@code Acknowledge} feature of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String ACKNOWLEDGE = "acknowledge";

	/**
	 * The name of the the {@code Guaranteed Ordering} extension of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String EXTENSION_GUARANTEED_ORDERING = "guaranteedOrdering";

	/**
	 * The name of the the {@code Guaranteed Delivery} extension of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String EXTENSION_GUARANTEED_DELIVERY = "guaranteedDelivery";

	/**
	 * The name of the the {@code Automatic Acknowledgement} extension of the
	 * Messaging specification
	 *
	 * @since 1.0
	 */
	String EXTENSION_AUTO_ACKNOWLEDGE = "autoAcknowledge";

	/**
	 * The name of the the {@code Last Will} extension of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String EXTENSION_LAST_WILL = "lastWill";

	/**
	 * The name of the the {@code Quality of Service} extension of the Messaging
	 * specification
	 *
	 * @since 1.0
	 */
	String EXTENSION_QOS = "qos";

}