package org.osgi.service.messaging.acknowledge;

/**
 * Acknowledge types a message can have.
 *
 * RECEIVED - the massage is received, but neither acknowledged nor rejected
 * ACKOWLEDGED - the message was acknowledged
 * REJECTED - the message was rejected
 * UNSUPPORTED - acknowledgement is not supported
 * UNKNOWN - an unknown state
 */
public enum AcknowledgeType {

    RECEIVED,
    ACKOWLEDGED,
    REJECTED,
    UNSUPPORTED,
    UNKNOWN;

}