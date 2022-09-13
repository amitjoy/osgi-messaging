package org.osgi.service.messaging.dto;

import org.osgi.dto.DTO;

/**
 * A {@link DTO} that defines a channel with the possibility to provide
 * additional channel information like routing keys.
 */
public class ChannelDTO extends DTO {

	/** The name of the channel */
	public String name;

	/** A possible extension to a channel like a routing key */
	public String extension;

	/** <code>true</code>, if the channel is connected */
	public boolean connected;

}