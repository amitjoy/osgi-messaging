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

import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.acknowledge.AcknowledgeMessageContext;

@Component(property = "myConsumer=true")
public final class Mqtt5Consumer implements Consumer<Message> {

	@Override
	public void accept(final Message m) {

		final AcknowledgeMessageContext amc = (AcknowledgeMessageContext) m.getContext();
		switch (amc.getAcknowledgeState()) {
		case ACKNOWLEDGED:
			System.out.println("Log Acknowledged Message");
			break;
		case REJECTED:
			System.out.println("Log Rejected Message");
			break;
		default:
			System.out.println("Log Message state " + amc.getAcknowledgeState());
			break;
		}

	}

}
