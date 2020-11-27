/*******************************************************************************
 * Copyright 2020 Amit Kumar Mondal
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
package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.Mqtt5MessageConstants.MQTT_PROTOCOL;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.messaging.acknowledge.AcknowledgeHandler;
import org.osgi.service.messaging.propertytypes.MessagingFeature;

@Component(service = { AcknowledgeHandler.class, MessageAcknowledgeHandlerProvider.class })
@MessagingFeature(name = "mqtt5-procol-specific-acknowledge-handler", protocol = MQTT_PROTOCOL)
public final class MessageAcknowledgeHandlerProvider implements AcknowledgeHandler {

    // TODO think about if we have any message restriction for this handler?
    @Override
    public boolean acknowledge() {
        return true;
    }

    @Override
    public boolean reject() {
        return false;
    }

}
