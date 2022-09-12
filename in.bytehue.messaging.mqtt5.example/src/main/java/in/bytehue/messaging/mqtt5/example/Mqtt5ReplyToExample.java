/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import java.nio.ByteBuffer;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.replyto.ReplyToPublisher;

@Component
public final class Mqtt5ReplyToExample {

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private ReplyToPublisher mqttPublisher;

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

	public void publishReplyToMessage() {
		final MessageContextBuilder mcb = mcbFactory.getService();
		try {
			// @formatter:off
            final Message request =
                    mcb.channel("/demo")
                       .correlationId("test123")
                       .replyTo("demo_response")
                       .content(ByteBuffer.wrap("Hello Word!".getBytes()))
                       .buildMessage();
            // @formatter:on
			mqttPublisher.publishWithReply(request).onSuccess(System.out::println);
		} finally {
			mcbFactory.ungetService(mcb);
		}
	}

}
