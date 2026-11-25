package pt.monitorizapt.domain;

/**
 * Defines available sensor types and their units.
 */
public enum SensorTipo {
    TEMPERATURA("Temperatura", "°C"),
    HUMIDADE("Humidade", "%"),
    QUALIDADE_AR("Qualidade do Ar", "IAQ");

    private final String descricao;
    private final String unidade;

    SensorTipo(String descricao, String unidade) {
        this.descricao = descricao;
        this.unidade = unidade;
    }

    public String getDescricao() { return descricao; }
    public String getUnidade() { return unidade; }
}