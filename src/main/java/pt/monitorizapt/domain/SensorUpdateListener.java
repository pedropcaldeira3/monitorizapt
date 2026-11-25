package pt.monitorizapt.domain;

public interface SensorUpdateListener {
    void onDadosPublicados(Sensor sensor, DadosSensor dados, String payloadJson);
}