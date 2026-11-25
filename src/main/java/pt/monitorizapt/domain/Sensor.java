package pt.monitorizapt.domain;

public interface Sensor {
    void iniciar();
    void desligar();
    DadosSensor lerDados();
    void publicarMQTT(String json);
    void processarComando(String comandoJSON);

    String getIDUnico();
    String getOwner();
    SensorTipo getTipo();
    SensorLocalizacao getLocalizacao();

    boolean isAtivo();
    void ativar();
    void desativar();

    long getIntervaloMillis();
    void setIntervaloMillis(long intervalo);

    void registrarListener(SensorUpdateListener listener);
    void removerListener(SensorUpdateListener listener);
}