package mx.odapas.reporte.repositorio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Repository
public class RepositorioCatalogos {
    private final JdbcTemplate jdbc;
    public RepositorioCatalogos(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    // Identificadores exclusivamente internos; los valores del cliente siempre son parametros SQL.
    private static final Map<String, List<String>> CAMPOS = Map.of(
        "persona", List.of("nombre_completo"),
        "municipios", List.of("nombre", "estado"),
        "colonia", List.of("municipio_id", "nombre"),
        "tipo_reporte", List.of("codigo", "nombre"),
        "contacto", List.of("persona_id", "correo", "telefono"));
    private List<String> campos(String tabla) {
        var campos = CAMPOS.get(tabla);
        if (campos == null) throw new IllegalArgumentException("Catalogo desconocido");
        return campos;
    }
    public List<Map<String,Object>> listar(String tabla) {
        campos(tabla);
        return jdbc.queryForList("SELECT * FROM public." + tabla + " ORDER BY id");
    }
    public Map<String,Object> obtener(String tabla, int id) {
        campos(tabla);
        var filas = jdbc.queryForList("SELECT * FROM public." + tabla + " WHERE id = ?", id);
        if (filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado");
        return filas.get(0);
    }
    private Object[] validar(String tabla, Map<String,Object> datos) {
        var campos = campos(tabla);
        if (datos == null || !datos.keySet().equals(new HashSet<>(campos)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campos requeridos: " + String.join(", ", campos));
        var valores = new ArrayList<Object>();
        for (String campo : campos) {
            Object valor = datos.get(campo);
            if (campo.endsWith("_id")) {
                if (!(valor instanceof Number n) || n.doubleValue() != n.intValue() || n.intValue() <= 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " debe ser un entero positivo");
                valores.add(((Number) valor).intValue());
            } else {
                if (!(valor instanceof String s) || s.isBlank())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " es obligatorio");
                String texto = ((String) valor).trim();
                int limite = campo.equals("telefono") ? 20 : campo.equals("codigo") ? 30 :
                    (campo.equals("nombre_completo") || campo.equals("correo")) ? 150 : 100;
                if (texto.length() > limite)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " supera " + limite + " caracteres");
                valores.add(campo.equals("correo") ? texto.toLowerCase(Locale.ROOT) : texto);
            }
        }
        return valores.toArray();
    }
    public Map<String,Object> crear(String tabla, Map<String,Object> datos) {
        Object[] valores = validar(tabla, datos);
        var campos = campos(tabla);
        return jdbc.queryForMap("INSERT INTO public." + tabla + " (" + String.join(",", campos) +
            ") VALUES (" + String.join(",", Collections.nCopies(campos.size(), "?")) + ") RETURNING *", valores);
    }
    public Map<String,Object> actualizar(String tabla, int id, Map<String,Object> datos) {
        Object[] valores = validar(tabla, datos);
        var parametros = new ArrayList<>(Arrays.asList(valores));
        parametros.add(id);
        String asignaciones = String.join(",", campos(tabla).stream().map(c -> c + " = ?").toList());
        var filas = jdbc.queryForList("UPDATE public." + tabla + " SET " + asignaciones + " WHERE id = ? RETURNING *",
            parametros.toArray());
        if (filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado");
        return filas.get(0);
    }
    public void eliminar(String tabla, int id) {
        campos(tabla);
        if (jdbc.update("DELETE FROM public." + tabla + " WHERE id = ?", id) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado");
    }
}

