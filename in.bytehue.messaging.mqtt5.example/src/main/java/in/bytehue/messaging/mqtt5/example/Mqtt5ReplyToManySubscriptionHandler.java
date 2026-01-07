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
package in.bytehue.messaging.mqtt5.example;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.propertytypes.ReplyToSubscription;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

@Component
@ReplyToSubscription(target = "(&(osgi.messaging.protocol=mqtt5)(osgi.messaging.name=mqtt5-hivemq-adapter)(osgi.messaging.feature=replyTo)))", channel = "a/b", replyChannel = "c/d")
public final class Mqtt5ReplyToManySubscriptionHandler implements ReplyToManySubscriptionHandler {

	final PushStreamProvider provider = new PushStreamProvider();
	final SimplePushEventSource<Message> source = provider.createSimpleEventSource(Message.class);

	volatile String requestPayLoad;

	@Reference(target = "(foo1=bar1)")
	private Predicate<Message> filter;

	private Thread thread;

	@Override
	public PushStream<Message> handleResponses(final Message requestMessage,
			final MessageContextBuilder responseBuilder) {
		thread = new Thread(() -> {
			try {
				while (!filter.test(requestMessage)) {
					final Message responseMessage = responseBuilder.content(ByteBuffer.wrap("AMIT".getBytes()))
							.buildMessage();
					source.publish(responseMessage);
					try {
						TimeUnit.SECONDS.sleep(1L);
					} catch (final InterruptedException e) {
						source.error(e);
					}
				}
			} finally {
				thread.interrupt();
			}
		});
		thread.start();
		source.endOfStream();
		return provider.createStream(source);
	}

}
