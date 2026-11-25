package pt.monitorizapt.domain;

/**
 * Immutable record to hold sensor reading data.
 */
public record DadosSensor(double valor, String unidade, String alerta, long timestamp, String tipoDescricao) {}