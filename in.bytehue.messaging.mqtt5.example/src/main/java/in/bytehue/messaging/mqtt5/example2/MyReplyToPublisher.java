/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
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
package in.bytehue.messaging.mqtt5.example2;

import java.nio.ByteBuffer;

import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.replyto.ReplyToPublisher;

@GogoCommand(scope = "foo", function = "send")
@Component(service = MyReplyToPublisher.class)
public final class MyReplyToPublisher {

	@interface Config {
		String channel() default "channel_foo";

		String replyToChannel() default "channel_bar";
	}

	@Reference
	private ReplyToPublisher publisher;

	@Reference
	private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

	@Activate
	private Config config;

	public void send(final String value) {
		final MessageContextBuilder mcb = mcbFactory.getService();
		try {
			// @formatter:off
            final Message message = mcb.channel(config.channel())
                                       .replyTo(config.replyToChannel())
                                       .content(ByteBuffer.wrap(value.getBytes()))
                                       .buildMessage();
            // @formatter:on
			publisher.publishWithReply(message).onSuccess(System.out::println);
		} finally {
			mcbFactory.ungetService(mcb);
		}
	}

}
