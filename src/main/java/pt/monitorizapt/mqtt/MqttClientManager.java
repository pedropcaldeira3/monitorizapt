package pt.monitorizapt.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import pt.monitorizapt.domain.SensorLocalizacao;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages the MQTT connection, subscriptions, and publishing.
 * Wraps the Eclipse Paho client to provide easier usage within the app.
 */
public class MqttClientManager {
    private final String brokerUrl;
    // Unique Client ID to avoid conflicts on the public broker
    private final String clientId = "MonitorizaPT-" + UUID.randomUUID();

    private final Map<String, Consumer<String>> commandHandlers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();

    private MqttClient client;

    public MqttClientManager(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    /**
     * Connects asynchronously to ensure the UI does not freeze.
     */
    public CompletableFuture<Void> connectAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                connectInternal();
            } catch (MqttException e) {
                System.err.println("Failed to connect: " + e.getMessage());
                notifyConnection(false);
            }
        });
    }

    private synchronized void connectInternal() throws MqttException {
        if (client != null && client.isConnected()) {
            return;
        }

        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("MQTT Connected to " + serverURI);
                notifyConnection(true);
                reapplySubscriptions();
            }

            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("MQTT Connection lost: " + cause.getMessage());
                notifyConnection(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Handled by individual listeners
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Not used for QoS 0
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        client.connect(options);
    }

    private void reapplySubscriptions() {
        commandHandlers.forEach((topic, handler) -> subscribeInternal(topic, handler));
    }

    public void publish(String topic, String payload) {
        try {
            if (client == null || !client.isConnected()) {
                connectAsync();
                return;
            }
            client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 0, false);
        } catch (MqttException e) {
            System.err.println("Error publishing: " + e.getMessage());
        }
    }

    public void registerCommandHandler(SensorLocalizacao localizacao, Consumer<String> handler) {
        String topic = localizacao.topicoComandos();
        commandHandlers.put(topic, handler);
        subscribeInternal(topic, handler);
    }

    private void subscribeInternal(String topic, Consumer<String> handler) {
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            client.subscribe(topic, (t, msg) -> {
                String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
                handler.accept(payload);
            });
            System.out.println("Subscribed to: " + topic);
        } catch (MqttException e) {
            System.err.println("Error subscribing to " + topic + ": " + e.getMessage());
        }
    }

    public CompletableFuture<Boolean> testConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (isConnected()) return true;
            try {
                connectInternal();
                return true;
            } catch (MqttException e) {
                return false;
            }
        });
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void registerConnectionListener(Consumer<Boolean> listener) {
        connectionListeners.add(listener);
    }

    private void notifyConnection(boolean connected) {
        connectionListeners.forEach(listener -> listener.accept(connected));
    }

    public synchronized void shutdown() {
        if (client != null) {
            try {
                client.close(true);
            } catch (MqttException ignored) { }
        }
    }
}