package pt.monitorizapt.domain;

/**
 * Enum defining the fixed locations required by the project.
 * IMPORTANT: The strings must match the PDF exactly to generate correct MQTT topics.
 */
public enum SensorLocalizacao {
    LISBOA_CAMPUS("Lisboa - Campus IPLuso"),
    LISBOA_BAIXA("Lisboa - Baixa"),
    PORTO_MATOSINHOS("Porto - Matosinhos"),
    COIMBRA_CENTRO("Coimbra - Centro"),
    FARO_MARINA("Faro - Marina"),
    BRAGA_SAMEIRO("Braga - Sameiro"),
    EVORA_UNIVERSIDADE("Évora - Universidade");

    private final String descricao;

    SensorLocalizacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public String idSegmento() {
        // Example: "Lisboa - Campus IPLuso" -> "LISBOA_-_CAMPUS_IPLUSO"
        return descricao.replace(" ", "_").toUpperCase();
    }

    public String topicoDados() {
        // Format: envira/pt/sensores/dados/Lisboa_Campus_IPLuso
        return "envira/pt/sensores/dados/" + descricao.replace(" ", "_");
    }

    public String topicoComandos() {
        // Format: envira/pt/sensores/comandos/Lisboa_Campus_IPLuso
        return "envira/pt/sensores/comandos/" + descricao.replace(" ", "_");
    }
}