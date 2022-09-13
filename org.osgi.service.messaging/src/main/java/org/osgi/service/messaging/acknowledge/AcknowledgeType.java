package org.osgi.service.messaging.acknowledge;

/**
 * Acknowledge types a message can have.
 *
 * RECEIVED - the massage is received, but neither acknowledged nor rejected
 * ACKNOWLEDGED - the message was acknowledged REJECTED - the message was
 * rejected UNSUPPORTED - acknowledgement is not supported UNKNOWN - an unknown
 * state
 */
public enum AcknowledgeType {

	RECEIVED, ACKNOWLEDGED, REJECTED, UNSUPPORTED, UNKNOWN;

}