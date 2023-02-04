/*******************************************************************************
 * Copyright 2020-2023 Amit Kumar Mondal
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
package in.bytehue.messaging.mqtt5.remote.example;

import java.nio.ByteBuffer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.messaging.Message;

import in.bytehue.messaging.mqtt5.api.MqttMessageContextBuilder;
import in.bytehue.messaging.mqtt5.remote.api.MqttApplication;
import in.bytehue.messaging.mqtt5.remote.propertytypes.MqttApplicationId;

@Component
@MqttApplicationId("APP-V1")
public final class MyApplication implements MqttApplication {

	@Override
	public Message doGET( //
			final String resource, //
			final Message requestMessage, //
			final MqttMessageContextBuilder messageBuilder) throws Exception {

		return messageBuilder.content(ByteBuffer.wrap("AMIT".getBytes())).buildMessage();
	}

}
