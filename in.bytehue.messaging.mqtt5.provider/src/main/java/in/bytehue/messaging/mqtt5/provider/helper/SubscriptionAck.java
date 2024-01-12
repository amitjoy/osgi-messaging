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
package in.bytehue.messaging.mqtt5.provider.helper;

import org.osgi.service.messaging.Message;
import org.osgi.util.pushstream.PushStream;

public final class SubscriptionAck {

	private final String id;
	private final PushStream<Message> stream;

	private SubscriptionAck(final PushStream<Message> stream, final String id) {
		this.id = id;
		this.stream = stream;
	}

	public static SubscriptionAck of(final PushStream<Message> stream, final String id) {
		return new SubscriptionAck(stream, id);
	}

	public String id() {
		return id;
	}

	public PushStream<Message> stream() {
		return stream;
	}

}
