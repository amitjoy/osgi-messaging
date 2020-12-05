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
package in.bytehue.messaging.mqtt5.api;

/**
 * This is a marker interface which consumers can implement to provide services with properties
 * that can be used as conditional target to the MQTT client. That means, consumer can provide
 * filters that should be satisfied before MQTT client is up and running.
 *
 * <p>
 * As an example, it can be used the following way:
 *
 * <p>
 * {@code (|(&((bar=0)(bar=1)))(&((bar=1)(bar=0))))}
 *
 * <p>
 * Besides calculating the aggregate values from the properties of the whiteboard services,
 * it also accepts filters that have calculated fields.
 *
 * <p>
 * For example, if you need at least three services that have a bar property then you can filter
 * on {@code #bar}. There are a number of calculated values:
 *
 * <p>
 * <ul>
 * <li>{@code #key} – Calculates the number of key properties</li>
 * <li>{@code [avg]key} – Calculates the average of all key properties</li>
 * <li>{@code [min]key} – Calculates the minimum of all key properties</li>
 * <li>{@code [max]key} – Calculates the maximum of all key properties</li>
 * <li>{@code [sum]key} – Calculates the sum of all key properties</li>
 * <li>{@code [unq]key} – Calculates the number of unique key properties</li>
 * </ul>
 *
 * <p>
 * Example: {@code ([sum]bar>=3)}
 *
 * <p>
 * Assume we have a service R that represents a link to a remote service. For performance and
 * reliability reasons, we require at least 3 of those services to be present before we can start
 * the MQTT client. Additionally, these services must come from at least 2 different regions. For
 * this reason, we define a property region that can take the values south, east, north, and west.
 *
 * <p>
 * Example: {@code (&(#>=3)([unq]region>=2))}
 *
 * <p>
 * This is useful is certain circumstances where MQTT client requires few services to be up and running
 * before the client is connected to the server. For example, you could provide the following services
 * for the client to use before it connects to the server -
 *
 * <p>
 * <ul>
 * <li>{@code javax.net.ssl.TrustManagerFactory}</li>
 * <li>{@code com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener}</li>
 * <li>{@code com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener}</li>
 * <li>{@code com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism}</li>
 * <li>{@code com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5IncomingQos1Interceptor}</li>
 * <li>{@code com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos2.Mqtt5IncomingQos2Interceptor}</li>
 * <li>{@code com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos1.Mqtt5OutgoingQos1Interceptor}</li>
 * <li>{@code com.hivemq.client.mqtt.mqtt5.advanced.interceptor.qos2.Mqtt5OutgoingQos2Interceptor}</li>
 * </ul>
 *
 * <p>
 * This will ensure that your services will be up and running before the client gets activated. This also
 * guarantees that the start order of the bundles is not at all required in this scenario.
 */
public interface TargetCondition {

}
