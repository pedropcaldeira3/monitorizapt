package pt.monitorizapt.sensors;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

import java.util.concurrent.ThreadLocalRandom;

public class SensorTemperatura extends SensorAbstrato {

    public SensorTemperatura(SensorLocalizacao localizacao,
                             JsonPayloadBuilder payloadBuilder,
                             MqttClientManager mqttClientManager) {
        super(SensorTipo.TEMPERATURA, localizacao, payloadBuilder, mqttClientManager);
    }

    @Override
    protected DadosSensor gerarDadosEspecificos() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double valor;
        String alerta = "false";

        // 5% chance of "nonsense data" (-45.0 C) required by project
        if (random.nextDouble() < 0.05) {
            valor = -45.0;
            alerta = "true";
        }
        else if (random.nextDouble() < 0.15) {
            valor = random.nextDouble(30.1, 42.0);
            alerta = "true";
        }
        else {
            valor = random.nextDouble(15.0, 29.0);
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