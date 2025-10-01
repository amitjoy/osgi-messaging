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
package in.bytehue.messaging.mqtt5.api;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The {@link MqttClient} service provides an interface for managing MQTT 5.0
 * client connections to an MQTT broker.
 *
 * <p>
 * This service abstracts the connection lifecycle and provides methods to
 * connect, disconnect, and query the connection state. It acts as a facade over
 * the underlying HiveMQ MQTT client implementation.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> Access to this service requires the
 * {@code ServicePermission[MqttClient, GET]} permission.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by consumers.
 * @noextend This interface is not intended to be extended by consumers.
 *
 * @ThreadSafe
 * @since 1.0
 */
@ProviderType
public interface MqttClient {

	/**
	 * Initiates a connection to the MQTT broker.
	 * 
	 * <p>
	 * This method is typically handled automatically by the OSGi component
	 * lifecycle. Manual invocation may be useful for reconnection scenarios or
	 * custom connection management.
	 * </p>
	 *
	 * @return a {@link CompletableFuture} that completes when the connection is
	 *         established, or completes exceptionally if the connection fails
	 * @throws IllegalStateException if the client is already connected
	 */
	CompletableFuture<Void> connect();

	/**
	 * Disconnects from the MQTT broker gracefully.
	 * 
	 * <p>
	 * This method sends a proper DISCONNECT packet to the broker and closes the
	 * connection. It blocks until the disconnection is complete.
	 * </p>
	 *
	 * @return a {@link CompletableFuture} that completes when the disconnection is
	 *         complete
	 * @throws IllegalStateException if the client is not connected
	 */
	CompletableFuture<Void> disconnect();

	/**
	 * Checks whether the client is currently connected to the MQTT broker.
	 *
	 * @return {@code true} if the client is connected, {@code false} otherwise
	 */
	boolean isConnected();
}
