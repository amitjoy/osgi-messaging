/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.api;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The {@link MqttCommandExtension} interface is used to provide additional
 * information for display in MQTT Gogo commands.
 *
 * @since 1.0
 */
@ConsumerType
public interface MqttCommandExtension {

    /**
     * Retrieves the name of the row to be displayed in the command.
     *
     * @return the row name
     */
    String rowName();

    /**
     * Retrieves the value of the row to be displayed in the command.
     *
     * @return the row value
     */
    String rowValue();

}
