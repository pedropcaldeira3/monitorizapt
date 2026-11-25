package pt.monitorizapt.sensors;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

import java.util.concurrent.ThreadLocalRandom;

public class SensorHumidade extends SensorAbstrato {

    public SensorHumidade(SensorLocalizacao localizacao,
                          JsonPayloadBuilder payloadBuilder,
                          MqttClientManager mqttClientManager) {
        super(SensorTipo.HUMIDADE, localizacao, payloadBuilder, mqttClientManager);
    }

    @Override
    protected DadosSensor gerarDadosEspecificos() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double valor;
        String alerta = "false";

        // Logic: 10% chance of High Humidity (> 80%) which triggers alert
        if (random.nextDouble() < 0.10) {
            valor = random.nextDouble(80.1, 98.0);
            alerta = "true";
        } else {
            valor = random.nextDouble(40.0, 75.0);
        }

        return new DadosSensor(
                valor,
                getTipo().getUnidade(),
                alerta,
                System.currentTimeMillis(),
                getTipo().getDescricao()
        );
    }
}
