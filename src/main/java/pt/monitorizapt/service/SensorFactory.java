package pt.monitorizapt.service;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.Sensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorUpdateListener;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.sensors.SensorHumidade;
import pt.monitorizapt.sensors.SensorQualidadeAr;
import pt.monitorizapt.sensors.SensorTemperatura;
import pt.monitorizapt.util.JsonPayloadBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service class responsible for instantiating and managing all sensors.
 */
public class SensorFactory {

    // CORREÇÃO AQUI: Mudou de "Europe/Lisboa" para "Europe/Lisbon"
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.of("Europe/Lisbon"));

    private final Map<SensorLocalizacao, SensorAbstrato> sensoresPorLocalizacao = new EnumMap<>(SensorLocalizacao.class);
    private final List<Consumer<SensorSnapshot>> snapshotObservers = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logObservers = new CopyOnWriteArrayList<>();

    private final JsonPayloadBuilder payloadBuilder = new JsonPayloadBuilder();
    private final MqttClientManager mqttClientManager;

    public SensorFactory(MqttClientManager mqttClientManager) {
        this.mqttClientManager = mqttClientManager;

        criarSensores();

        // Start all sensor threads
        sensoresPorLocalizacao.values().forEach(Sensor::iniciar);

        // Register remote commands
        for (SensorLocalizacao localizacao : SensorLocalizacao.values()) {
            mqttClientManager.registerCommandHandler(localizacao, comando -> processarComandoRemoto(localizacao, comando));
        }

        mqttClientManager.connectAsync();
    }

    private void criarSensores() {
        for (SensorLocalizacao localizacao : SensorLocalizacao.values()) {
            SensorAbstrato sensor = criarSensorPorLocalizacao(localizacao);
            sensor.registrarListener(criarListener());
            sensoresPorLocalizacao.put(localizacao, sensor);
        }
    }

    private SensorAbstrato criarSensorPorLocalizacao(SensorLocalizacao localizacao) {
        return switch (localizacao) {
            case LISBOA_CAMPUS, COIMBRA_CENTRO, EVORA_UNIVERSIDADE ->
                    new SensorTemperatura(localizacao, payloadBuilder, mqttClientManager);
            case LISBOA_BAIXA, FARO_MARINA ->
                    new SensorHumidade(localizacao, payloadBuilder, mqttClientManager);
            case PORTO_MATOSINHOS, BRAGA_SAMEIRO ->
                    new SensorQualidadeAr(localizacao, payloadBuilder, mqttClientManager);
        };
    }

    private SensorUpdateListener criarListener() {
        return (sensor, dados, payload) -> {
            SensorSnapshot snapshot = new SensorSnapshot(
                    sensor.getIDUnico(),
                    sensor.getLocalizacao().getDescricao(),
                    sensor.getTipo().getDescricao(),
                    dados.valor(),
                    dados.unidade(),
                    dados.alerta(),
                    dados.timestamp()
            );

            // Notify UI
            snapshotObservers.forEach(observer -> observer.accept(snapshot));
            log(String.format("Sensor %s published %.2f %s", sensor.getIDUnico(), dados.valor(), dados.unidade()));
        };
    }

    public void registerSnapshotObserver(Consumer<SensorSnapshot> observer) {
        snapshotObservers.add(observer);
    }

    public void registerLogObserver(Consumer<String> observer) {
        logObservers.add(observer);
    }

    public void ativarLocalizacao(SensorLocalizacao localizacao, long intervaloMillis) {
        SensorAbstrato sensor = sensoresPorLocalizacao.get(localizacao);
        if (sensor != null) {
            sensor.setIntervaloMillis(intervaloMillis);
            sensor.ativar();
            log("Activated " + sensor.getIDUnico());
        }
    }

    public void desativarLocalizacao(SensorLocalizacao localizacao) {
        SensorAbstrato sensor = sensoresPorLocalizacao.get(localizacao);
        if (sensor != null) {
            sensor.desativar();
            log("Deactivated " + sensor.getIDUnico());
        }
    }

    private void processarComandoRemoto(SensorLocalizacao localizacao, String comandoJson) {
        SensorAbstrato sensor = sensoresPorLocalizacao.get(localizacao);
        if (sensor != null) {
            sensor.processarComando(comandoJson);
            log("Remote command received for " + sensor.getIDUnico());
        }
    }

    private void log(String mensagem) {
        String linha = LOG_FORMATTER.format(Instant.now()) + " [INFO] " + mensagem;
        logObservers.forEach(observer -> observer.accept(linha));
    }

    public void shutdown() {
        sensoresPorLocalizacao.values().forEach(Sensor::desligar);
        mqttClientManager.shutdown();
    }
}