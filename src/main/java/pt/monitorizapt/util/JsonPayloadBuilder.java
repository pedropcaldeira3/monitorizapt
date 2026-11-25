package pt.monitorizapt.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import pt.monitorizapt.domain.DadosSensor;
import pt.monitorizapt.domain.Sensor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to build the JSON payload exactly as requested.
 * Handles the SHA-256 hash generation manually to guarantee Hex output.
 */
public class JsonPayloadBuilder {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public String buildPayload(Sensor sensor, DadosSensor dados) {
        JsonObject object = new JsonObject();

        object.addProperty("campus", "IP Lisboa");
        object.addProperty("sensor", sensor.getIDUnico());
        object.addProperty("ID Unico", Math.abs(sensor.getIDUnico().hashCode()));
        object.addProperty("Owner", sensor.getOwner());
        object.addProperty("tipo", sensor.getTipo().getDescricao());
        object.addProperty("valor", roundValue(dados.valor()));
        object.addProperty("unidade", dados.unidade());

        boolean alertState = "ALERTA".equalsIgnoreCase(dados.alerta()) || "true".equalsIgnoreCase(dados.alerta());
        object.addProperty("alerta", alertState);

        object.addProperty("timestamp", dados.timestamp());

        // Calculate hash on the JSON string without spaces
        String jsonNoHash = GSON.toJson(object);
        String stringForHash = jsonNoHash.replace(" ", "");

        String calculatedHash = calculateHexHash(stringForHash);
        object.addProperty("hash_validacao", calculatedHash);

        return GSON.toJson(object);
    }

    private double roundValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String calculateHexHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            // Convert byte-by-byte to Hex String manually
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "hash_error";
        }
    }
}