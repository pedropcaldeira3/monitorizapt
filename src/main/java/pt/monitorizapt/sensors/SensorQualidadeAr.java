package pt.monitorizapt.sensors;

import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.SensorAbstrato;
import pt.monitorizapt.domain.SensorLocalizacao;
import pt.monitorizapt.domain.SensorTipo;
import pt.monitorizapt.mqtt.MqttClientManager;
import pt.monitorizapt.util.JsonPayloadBuilder;

import java.util.concurrent.ThreadLocalRandom;

public class SensorQualidadeAr extends SensorAbstrato {

    public SensorQualidadeAr(SensorLocalizacao localizacao,
                             JsonPayloadBuilder payloadBuilder,
                             MqttClientManager mqttClientManager) {
        super(SensorTipo.QUALIDADE_AR, localizacao, payloadBuilder, mqttClientManager);
    }

    @Override
    protected DadosSensor gerarDadosEspecificos() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double valor;
        String alerta = "false";

        // Logic: 15% chance of Poor Air Quality (> 50)
        if (random.nextDouble() < 0.15) {
            valor = random.nextDouble(51.0, 150.0);
            alerta = "true";
        } else {
            valor = random.nextDouble(5.0, 45.0);
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