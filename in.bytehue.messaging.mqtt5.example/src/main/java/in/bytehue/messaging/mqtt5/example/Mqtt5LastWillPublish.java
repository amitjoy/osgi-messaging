/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.example;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.LAST_WILL_DELAY_INTERVAL;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.Extension.MESSAGE_EXPIRY_INTERVAL;
import static org.osgi.service.messaging.Features.EXTENSION_LAST_WILL;
import static org.osgi.service.messaging.Features.EXTENSION_QOS;

import java.nio.ByteBuffer;

import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;

@GogoCommand(scope = "example", function = "lastWillPublish")
@Component(service = Mqtt5LastWillPublish.class, immediate = true)
public final class Mqtt5LastWillPublish {

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private MessagePublisher publisher;

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

	public void lastWillPublish() {
		final MessageContextBuilder mcb = mcbFactory.getService();
		try {
			// @formatter:off
            publisher.publish(
                    mcb.content(ByteBuffer.wrap("CLOSED_CONNECTION".getBytes()))
                       .channel("last/will/topc/example")
                       .extensionEntry(EXTENSION_QOS, 2)
                       .extensionEntry(EXTENSION_LAST_WILL, true)
                       .extensionEntry(LAST_WILL_DELAY_INTERVAL, 30L)
                       .extensionEntry(MESSAGE_EXPIRY_INTERVAL, 30L)
                       .buildMessage());
            // @formatter:on
		} finally {
			mcbFactory.ungetService(mcb);
		}
	}

}
