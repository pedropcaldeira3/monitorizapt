package pt.monitorizapt.service;

/**
 * Data Transfer Object (Record) to send sensor state to the UI Table.
 */
public record SensorSnapshot(
        String id,
        String localizacao,
        String tipo,
        double valor,
        String unidade,
        String alerta,
        long timestamp
) {}