/*******************************************************************************
 * Copyright 2020-2024 Amit Kumar Mondal
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
import java.nio.charset.StandardCharsets;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.util.pushstream.PushStream;

@Component(service = Mqtt5PubSubExample.class, immediate = true)
public final class Mqtt5PubSubExample {

	private PushStream<Message> stream;

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private MessagePublisher publisher;

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private MessageSubscription subscriber;

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private ComponentServiceObjects<MessageContextBuilder> mcbFactory;

	@Deactivate
	void deactivate() {
		stream.close();
	}

	public String sub(final String channel) {
		stream = subscriber.subscribe(channel);
		stream.forEach(m -> {
			System.out.println("Message Received");
			System.out.println(StandardCharsets.UTF_8.decode(m.payload()).toString());
		});
		return "Subscribed to " + channel;
	}

	public String pub(final String channel, final String data) {
		final MessageContextBuilder mcb = mcbFactory.getService();
		try {
			// @formatter:off
            publisher.publish(
                    mcb.content(ByteBuffer.wrap(data.getBytes()))
                       .channel(channel)
                       .buildMessage());
            // @formatter:on
		} finally {
			mcbFactory.ungetService(mcb);
		}
		return "Published to " + channel;
	}

}
