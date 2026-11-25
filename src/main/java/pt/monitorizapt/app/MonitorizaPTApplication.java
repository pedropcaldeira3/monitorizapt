package pt.monitorizapt.app;

import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.service.SensorFactory;
import pt.monitorizapt.ui.MonitorizaPTFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class MonitorizaPTApplication {

    private MonitorizaPTApplication() {}

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            // Broker URL is configurable: -Dmonitorizapt.broker=... or MONITORIZAPT_BROKER
            // (defaults to a local broker; the course used a broker provided by the school)
            String brokerUrl = System.getProperty("monitorizapt.broker",
                    System.getenv().getOrDefault("MONITORIZAPT_BROKER", "tcp://localhost:1883"));

            System.out.println("Starting MonitorizaPT - Connecting to: " + brokerUrl);

            MqttClientManager mqttClientManager = new MqttClientManager(brokerUrl);
            SensorFactory controller = new SensorFactory(mqttClientManager);

            MonitorizaPTFrame frame = new MonitorizaPTFrame(controller, mqttClientManager);
            frame.setVisible(true);
        });
    }
}
