package pt.monitorizapt.domain;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

/**
 * Base abstract class for all sensors.
 * Handles the threading lifecycle, timing (3333ms), and MQTT publishing logic.
 */
public abstract class SensorAbstrato implements Sensor, Runnable {

    // Requirement: Exactly 3333ms interval
    private static final long DEFAULT_INTERVAL = 3333L;

    // Owner identification included in every payload (configurable with -Dmonitorizapt.owner=...)
    private static final String OWNER_ID = System.getProperty("monitorizapt.owner", "Pedro_Caldeira");

    private static final Gson GSON = new Gson();

    private final SensorTipo tipo;
    private final SensorLocalizacao localizacaoFixa;
    private final JsonPayloadBuilder payloadBuilder;
    private final MqttClientManager mqttClientManager;

    private final List<SensorUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean loopActive = new AtomicBoolean(false);

    private volatile boolean active;

    // CORREÇÃO AQUI: Nome da variável uniformizado para Português
    private volatile long intervaloMillis = DEFAULT_INTERVAL;

    private volatile DadosSensor lastReading;
    private Thread worker;

    protected SensorAbstrato(SensorTipo tipo,
                             SensorLocalizacao localizacaoFixa,
                             JsonPayloadBuilder payloadBuilder,
                             MqttClientManager mqttClientManager) {
        this.tipo = tipo;
        this.localizacaoFixa = localizacaoFixa;
        this.payloadBuilder = payloadBuilder;
        this.mqttClientManager = mqttClientManager;
    }

    protected abstract DadosSensor gerarDadosEspecificos();

    @Override
    public final DadosSensor lerDados() {
        lastReading = gerarDadosEspecificos();
        return lastReading;
    }

    @Override
    public final void publicarMQTT(String json) {
        if (mqttClientManager != null) {
            mqttClientManager.publish(localizacaoFixa.topicoDados(), json);
        }
    }

    @Override
    public void processarComando(String comandoJSON) {
        try {
            JsonObject object = GSON.fromJson(comandoJSON, JsonObject.class);
            if (object == null || !object.has("acao")) {
                return;
            }

            String action = object.get("acao").getAsString().toUpperCase(Locale.ROOT);
            switch (action) {
                case "ATIVAR" -> ativar();
                case "DESATIVAR" -> desativar();
                default -> { }
            }

            if (object.has("intervalo")) {
                long newInterval = object.get("intervalo").getAsLong();
                if (newInterval >= 1000L) {
                    setIntervaloMillis(newInterval);
                }
            }
        } catch (JsonParseException | IllegalStateException ignored) {
            // Ignore malformed JSON
        }
    }

    @Override
    public final String getOwner() {
        return OWNER_ID;
    }

    @Override
    public final SensorTipo getTipo() {
        return tipo;
    }

    @Override
    public final SensorLocalizacao getLocalizacao() {
        return localizacaoFixa;
    }

    @Override
    public final boolean isAtivo() {
        return active;
    }

    @Override
    public final void ativar() {
        this.active = true;
    }

    @Override
    public final void desativar() {
        this.active = false;
    }

    @Override
    public final long getIntervaloMillis() {
        return intervaloMillis;
    }

    @Override
    public final void setIntervaloMillis(long novoIntervalo) {
        // CORREÇÃO AQUI: Garante que usa a variável correta
        this.intervaloMillis = Math.max(1000L, novoIntervalo);
    }

    @Override
    public final void registrarListener(SensorUpdateListener listener) {
        listeners.add(listener);
    }

    @Override
    public final void removerListener(SensorUpdateListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void iniciar() {
        if (loopActive.compareAndSet(false, true)) {
            worker = new Thread(this, getIDUnico() + "-loop");
            worker.setDaemon(true);
            worker.start();
        }
    }

    @Override
    public void desligar() {
        loopActive.set(false);
        if (worker != null) {
            worker.interrupt();
        }
    }

    @Override
    public String getIDUnico() {
        return "PT-SENSOR-" + localizacaoFixa.idSegmento();
    }

    @Override
    public void run() {
        while (true) {
            if (!loopActive.get()) {
                break;
            }
            try {
                if (active) {
                    DadosSensor reading = lerDados();
                    String payload = payloadBuilder.buildPayload(this, reading);

                    publicarMQTT(payload);
                    notificar(reading, payload);
                }
                // Mandatory pause
                // CORREÇÃO AQUI: Agora usa a variável com o nome certo
                Thread.sleep(intervaloMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                // Keep loop alive
            }
        }
    }

    protected final void notificar(DadosSensor dados, String payload) {
        for (SensorUpdateListener listener : listeners) {
            listener.onDadosPublicados(this, dados, payload);
        }
    }
}
