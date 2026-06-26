/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
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
 * <p>
 * Implementations of this interface are picked up by the MQTT Gogo command
 * and rendered as additional rows in the command output table.
 * </p>
 *
 * @see MqttMessageConstants
 * @since 1.0
 */
@ConsumerType
public interface MqttCommandExtension {

    /**
     * Retrieves the name of the row to be displayed in the Gogo command output.
     * This serves as the label for the information being displayed.
     *
     * @return the row name, must not be {@code null}
     */
    String rowName();

    /**
     * Retrieves the value of the row to be displayed in the Gogo command output.
     * This is the data associated with the row name.
     *
     * @return the row value, must not be {@code null}
     */
    String rowValue();

}
