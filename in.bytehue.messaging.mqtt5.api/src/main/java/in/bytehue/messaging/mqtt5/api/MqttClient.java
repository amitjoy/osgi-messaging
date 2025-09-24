package in.bytehue.messaging.mqtt5.api;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MqttClient {

	CompletableFuture<Void> connect();

	CompletableFuture<Void> disconnect();

	boolean isConnected();
}
