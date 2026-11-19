package com.aguapp.api.repository;
import com.aguapp.api.model.Reporte;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
public class ReporteRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT r.id, r.tipo_codigo, c.nombre AS colonia, r.notas,
                   r.latitud, r.longitud, r.creado_en,
                   EXISTS (SELECT 1 FROM verificacion_reporte v WHERE v.reporte_id = r.id) AS verificado
            FROM reporte r LEFT JOIN colonia c ON c.id = r.colonia_id
            """;
    private static final RowMapper<Reporte> MAPPER = (rs, row) -> new Reporte(
            rs.getString("id"), rs.getString("tipo_codigo"), rs.getString("colonia"), rs.getString("notas"),
            coordenada(rs.getBigDecimal("latitud")), coordenada(rs.getBigDecimal("longitud")),
            rs.getObject("creado_en", OffsetDateTime.class).atZoneSameInstant(
                    ZoneId.of("America/Mexico_City")).toLocalDate().toString(), rs.getBoolean("verificado"));
    public ReporteRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static Double coordenada(BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }
    public List<Reporte> listar(String tipo) {
        if (tipo == null || tipo.isBlank()) return jdbc.query(SELECT + " ORDER BY r.creado_en DESC, r.id", MAPPER);
        return jdbc.query(SELECT + " WHERE LOWER(r.tipo_codigo) = LOWER(?) ORDER BY r.creado_en DESC, r.id", MAPPER, tipo);
    }
    public Optional<Reporte> obtener(String id) {
        return jdbc.query(SELECT + " WHERE LOWER(r.id) = LOWER(?)", MAPPER, id).stream().findFirst();
    }
}
