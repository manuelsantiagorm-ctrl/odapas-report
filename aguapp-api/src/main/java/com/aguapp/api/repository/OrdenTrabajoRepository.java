package com.aguapp.api.repository;

import com.aguapp.api.model.OrdenTrabajo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrdenTrabajoRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT ot.id, ot.folio, ot.reporte_id, o.nombre AS organismo,
                   ot.prioridad_codigo, h.estado_codigo, ot.descripcion, ot.fecha_programada, ot.creado_en
            FROM orden_trabajo ot JOIN organismo o ON o.id = ot.organismo_id
            LEFT JOIN historial_orden h ON h.orden_id = ot.id
              AND h.secuencia = (SELECT MAX(h2.secuencia) FROM historial_orden h2 WHERE h2.orden_id = ot.id)
            """;
    private static final RowMapper<OrdenTrabajo> MAPPER = (rs, row) -> new OrdenTrabajo(
            rs.getString("id"), rs.getString("folio"), rs.getString("reporte_id"), rs.getString("organismo"),
            rs.getString("prioridad_codigo"), rs.getString("estado_codigo"), rs.getString("descripcion"),
            rs.getObject("fecha_programada", LocalDate.class), rs.getObject("creado_en", OffsetDateTime.class));
    public OrdenTrabajoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<OrdenTrabajo> listar(String estado, String reparadorId) {
        String sql = SELECT + " WHERE 1 = 1";
        List<Object> parametros = new ArrayList<>();
        if (estado != null && !estado.isBlank()) {
            sql += " AND LOWER(h.estado_codigo) = LOWER(?)";
            parametros.add(estado);
        }
        if (reparadorId != null && !reparadorId.isBlank()) {
            sql += " AND EXISTS (SELECT 1 FROM asignacion_orden a WHERE a.orden_id = ot.id"
                    + " AND LOWER(a.reparador_id) = LOWER(?) AND a.finalizado_en IS NULL)";
            parametros.add(reparadorId);
        }
        return jdbc.query(sql + " ORDER BY ot.creado_en DESC, ot.id", MAPPER, parametros.toArray());
    }
    public Optional<OrdenTrabajo> obtener(String id) {
        return jdbc.query(SELECT + " WHERE LOWER(ot.id) = LOWER(?)", MAPPER, id).stream().findFirst();
    }
}
