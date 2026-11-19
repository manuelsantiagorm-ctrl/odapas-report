package com.aguapp.api.model;

/**
 * Reporte ciudadano de un problema con el agua.
 * tipo: "fuga", "baja" (baja presión) o "corte".
 */
public record Reporte(
        String id,
        String tipo,
        String colonia,
        String notas,
        Double lat,
        Double lng,
        String fecha,
        boolean verificado
) {
}
