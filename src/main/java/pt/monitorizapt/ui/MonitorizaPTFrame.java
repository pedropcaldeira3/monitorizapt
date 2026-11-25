package pt.monitorizapt.ui;

import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.service.SensorFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MonitorizaPTFrame extends JFrame {

    private final SensorFactory controller;
    private final MqttClientManager mqttClientManager;
    private final SensorTableModel tableModel = new SensorTableModel();

    // Initial text
    private final JLabel estadoMqttLabel = new JLabel("Estado MQTT: A LIGAR...");
    private final JButton testarBrokerButton = new JButton("Testar Conexão");
    private final JComboBox<SensorLocalizacao> localizacaoCombo = new JComboBox<>(SensorLocalizacao.values());
    private final JTextField intervaloField = new JTextField("3333", 6);
    private final JTextArea logArea = new JTextArea();

    // Colors (Green and Red for status bar)
    private final Color COR_VERDE = new Color(0, 153, 51);
    private final Color COR_VERMELHA = new Color(204, 0, 0);

    public MonitorizaPTFrame(SensorFactory controller, MqttClientManager mqttClientManager) {
        super("MonitorizaPT - IoT Dashboard");
        this.controller = controller;
        this.mqttClientManager = mqttClientManager;

        configurarJanela();
        registrarCallbacks();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setLayout(new BorderLayout(0, 0));
        setLocationRelativeTo(null);

        // Clean white background
        getContentPane().setBackground(Color.WHITE);

        // --- NORTH PANEL (Status Bar) ---
        JPanel painelNorte = new JPanel(new BorderLayout());
        painelNorte.setBackground(Color.DARK_GRAY); // Starts gray
        painelNorte.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        estadoMqttLabel.setOpaque(false);
        estadoMqttLabel.setForeground(Color.WHITE); // White text on colored bar
        estadoMqttLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        testarBrokerButton.setFocusPainted(false);

        painelNorte.add(estadoMqttLabel, BorderLayout.WEST);
        painelNorte.add(testarBrokerButton, BorderLayout.EAST);
        add(painelNorte, BorderLayout.NORTH);

        // --- CENTRAL TABLE ---
        JTable tabela = new JTable(tableModel);
        tabela.setRowHeight(30);
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabela.setGridColor(Color.LIGHT_GRAY);

        // Simple and Visible Header
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.getTableHeader().setBackground(new Color(240, 240, 240));
        tabela.getTableHeader().setForeground(Color.BLACK);
        tabela.getTableHeader().setOpaque(true);

        // TABLE COLOR LOGIC (false vs ALERT)
        tabela.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);

                boolean isAlert = Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));

                if (isAlert) {
                    // ALERT MODE: Red background, White text, writes "ALERT"
                    setBackground(Color.RED);
                    setForeground(Color.WHITE);
                    setText("ALERT");
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    // NORMAL MODE: White background, Black text, writes "false"
                    if (isSelected) {
                        setBackground(table.getSelectionBackground());
                    } else {
                        setBackground(Color.WHITE);
                    }
                    setForeground(Color.BLACK);
                    setText("false");
                    setFont(table.getFont());
                }
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabela);
        add(scrollPane, BorderLayout.CENTER);

        // --- SOUTH PANEL (Buttons and Logs) ---
        JPanel painelSul = new JPanel(new BorderLayout(10, 10));
        painelSul.setBackground(new Color(245, 245, 245));
        painelSul.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        controls.setOpaque(false);

        // Black Labels (Visible)
        JLabel lblLoc = new JLabel("Location:");
        lblLoc.setForeground(Color.BLACK);
        JLabel lblInt = new JLabel("Interval (ms):");
        lblInt.setForeground(Color.BLACK);

        controls.add(lblLoc);
        controls.add(localizacaoCombo);
        controls.add(lblInt);
        controls.add(intervaloField);

        // --- BUTTON CORRECTION ---
        // We use normal buttons (without forcing strange colors) to ensure text readability.
        // The text will be BLACK.

        JButton btnIniciar = new JButton("START");
        btnIniciar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnIniciar.setForeground(Color.BLACK); // Ensures visibility
        btnIniciar.addActionListener(e -> iniciarSensor());
        controls.add(btnIniciar);

        JButton btnParar = new JButton("STOP");
        btnParar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnParar.setForeground(Color.BLACK); // Ensures visibility
        btnParar.addActionListener(e -> pararSensor());
        controls.add(btnParar);

        JButton btnLimpar = new JButton("Clear Logs");
        btnLimpar.setForeground(Color.BLACK);
        btnLimpar.addActionListener(e -> logArea.setText(""));
        controls.add(btnLimpar);

        painelSul.add(controls, BorderLayout.NORTH);

        // Log Area
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(Color.BLACK);

        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setPreferredSize(new Dimension(800, 150));
        scrollLog.setBorder(BorderFactory.createTitledBorder("System Logs"));

        painelSul.add(scrollLog, BorderLayout.CENTER);

        add(painelSul, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.shutdown();
            }
        });
    }

    private void registrarCallbacks() {
        controller.registerSnapshotObserver(s -> SwingUtilities.invokeLater(() -> tableModel.upsert(s)));

        controller.registerLogObserver(log -> SwingUtilities.invokeLater(() -> {
            logArea.append(log + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }));

        mqttClientManager.registerConnectionListener(conectado -> SwingUtilities.invokeLater(() -> {
            JPanel painelNorte = (JPanel) estadoMqttLabel.getParent();
            if (conectado) {
                // CHANGE HERE: Green Bar and Text in Portuguese
                painelNorte.setBackground(COR_VERDE);
                estadoMqttLabel.setText("Estado MQTT: VERDE");
            } else {
                // CHANGE HERE: Red Bar and Text in Portuguese
                painelNorte.setBackground(COR_VERMELHA);
                estadoMqttLabel.setText("Estado MQTT: VERMELHO");
            }
            painelNorte.repaint();
        }));

        testarBrokerButton.addActionListener(e -> mqttClientManager.testConnectionAsync());
    }

    private void iniciarSensor() {
        SensorLocalizacao loc = (SensorLocalizacao) localizacaoCombo.getSelectedItem();
        long intervalo = 3333;
        try {
            intervalo = Long.parseLong(intervaloField.getText());
        } catch (NumberFormatException ignored) {
            JOptionPane.showMessageDialog(this, "Invalid Interval!");
            return;
        }
        controller.ativarLocalizacao(loc, intervalo);
    }

    private void pararSensor() {
        SensorLocalizacao loc = (SensorLocalizacao) localizacaoCombo.getSelectedItem();
        controller.desativarLocalizacao(loc);
    }
}