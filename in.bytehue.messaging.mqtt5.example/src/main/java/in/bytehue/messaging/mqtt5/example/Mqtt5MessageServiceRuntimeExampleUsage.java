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

import static org.apache.commons.lang3.builder.ToStringStyle.JSON_STYLE;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.messaging.dto.MessagingRuntimeDTO;
import org.osgi.service.messaging.runtime.MessageServiceRuntime;

@Component(service = Object.class)
@GogoCommand(function = "runtime", scope = "test")
public final class Mqtt5MessageServiceRuntimeExampleUsage {

	@Reference(target = "(osgi.messaging.protocol=mqtt5)")
	private MessageServiceRuntime runtime;

	public String runtime() {
		final MessagingRuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		return ReflectionToStringBuilder.toString(runtimeDTO, JSON_STYLE);
	}

}
