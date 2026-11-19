package mx.odapas.reporte.repositorio;

import mx.odapas.reporte.modelo.Zona;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class RepositorioZonas {
    private final JdbcTemplate jdbc;
    private static final String CONSULTA = """
            SELECT codigo, nombre, descripcion, activa, colonias, trabajadores_disponibles
            FROM public.v_zonas
            """;
    private static final RowMapper<Zona> MAPEO = (resultado, fila) -> new Zona(
            resultado.getString("codigo"), resultado.getString("nombre"), resultado.getString("descripcion"),
            resultado.getBoolean("activa"), colonias(resultado.getArray("colonias")),
            resultado.getInt("trabajadores_disponibles"));

    public RepositorioZonas(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Zona> listar() { return jdbc.query(CONSULTA + " ORDER BY codigo", MAPEO); }

    public Optional<Zona> obtener(String codigo) {
        return jdbc.query(CONSULTA + " WHERE LOWER(codigo)=LOWER(?)", MAPEO, codigo).stream().findFirst();
    }

    private static List<String> colonias(Array valores) throws SQLException {
        if (valores == null) return List.of();
        try {
            return List.of((String[]) valores.getArray());
        } finally {
            valores.free();
        }
    }
}
