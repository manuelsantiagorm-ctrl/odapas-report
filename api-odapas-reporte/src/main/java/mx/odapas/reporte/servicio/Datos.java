package mx.odapas.reporte.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Validación de los cuerpos JSON. Todo valor del cliente se valida antes de llegar a SQL. */
public final class Datos {
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    /** Convierte en lista real los campos que PostgreSQL devuelve como texto JSON (json_agg::text). */
    public static <T extends Map<String, Object>> List<T> jsonEnLista(List<T> filas, String campo) {
        for (var f : filas) {
            Object v = f.get(campo);
            if (v instanceof String texto) {
                try { f.put(campo, JSON.readValue(texto, List.class)); } catch (Exception e) { f.put(campo, List.of()); }
            }
        }
        return filas;
    }

    private Datos() {}

    public static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static String texto(Map<String, Object> d, String campo, int min, int max) {
        Object v = d == null ? null : d.get(campo);
        if (!(v instanceof String s) || s.trim().length() < min || s.trim().length() > max)
            throw invalido("Revisa " + campo + " (" + min + " a " + max + " caracteres)");
        return s.trim();
    }

    public static String textoOpcional(Map<String, Object> d, String campo, int max) {
        Object v = d == null ? null : d.get(campo);
        if (v == null || (v instanceof String s && s.isBlank())) return null;
        if (!(v instanceof String s) || s.trim().length() > max) throw invalido("Revisa " + campo + " (máximo " + max + " caracteres)");
        return s.trim();
    }

    public static int entero(Map<String, Object> d, String campo) {
        Object v = d == null ? null : d.get(campo);
        if (v instanceof String s && s.matches("[0-9]{1,9}")) v = Integer.parseInt(s);
        if (!(v instanceof Number n) || n.doubleValue() != n.intValue() || n.intValue() < 1)
            throw invalido("Revisa " + campo);
        return n.intValue();
    }

    public static Integer enteroOpcional(Map<String, Object> d, String campo) {
        Object v = d == null ? null : d.get(campo);
        return v == null || "".equals(v) ? null : entero(d, campo);
    }

    public static List<Integer> enteros(Map<String, Object> d, String campo, int max) {
        Object v = d == null ? null : d.get(campo);
        var lista = new ArrayList<Integer>();
        if (v == null) return lista;
        if (!(v instanceof List<?> l) || l.size() > max) throw invalido("Revisa " + campo);
        for (Object o : l) {
            if (!(o instanceof Number n) || n.doubleValue() != n.intValue() || n.intValue() < 1) throw invalido("Revisa " + campo);
            if (!lista.contains(n.intValue())) lista.add(n.intValue());
        }
        return lista;
    }

    public static BigDecimal coordenada(Map<String, Object> d, String campo, double limite) {
        Object v = d == null ? null : d.get(campo);
        if (v == null || "".equals(v)) return null;
        if (!(v instanceof Number n) || Double.isNaN(n.doubleValue()) || Math.abs(n.doubleValue()) > limite)
            throw invalido("Coordenada inválida: " + campo);
        return BigDecimal.valueOf(n.doubleValue()).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    public static String opcion(Map<String, Object> d, String campo, List<String> permitidos) {
        Object v = d == null ? null : d.get(campo);
        if (!(v instanceof String s) || !permitidos.contains(s))
            throw invalido("Revisa " + campo + ": " + String.join(", ", permitidos));
        return s;
    }

    public static LocalDate fechaOpcional(Map<String, Object> d, String campo) {
        Object v = d == null ? null : d.get(campo);
        if (v == null || "".equals(v)) return null;
        try {
            return LocalDate.parse((String) v);
        } catch (RuntimeException e) {
            throw invalido("Fecha inválida en " + campo + " (use AAAA-MM-DD)");
        }
    }

    public static String folio(String folio) {
        if (folio == null || !folio.matches("[A-Za-z0-9-]{1,30}")) throw invalido("Folio inválido");
        return folio.toUpperCase();
    }
}
