package com.aguapp.api.repository;

import com.aguapp.api.model.Reparador;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ReparadorRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT u.id, u.nombre, o.nombre AS organismo
            FROM reparador r JOIN empleado e ON e.usuario_id = r.usuario_id
            JOIN usuario u ON u.id = r.usuario_id JOIN organismo o ON o.id = e.organismo_id
            WHERE e.estado_alta = 'aprobado' AND u.activo = TRUE AND o.activo = TRUE
            AND EXISTS (SELECT 1 FROM usuario_rol ur WHERE ur.usuario_id = u.id AND ur.rol_codigo = 'reparador')
            """;
    private static final RowMapper<Reparador> MAPPER = (rs, row) -> new Reparador(
            rs.getString("id"), rs.getString("nombre"), rs.getString("organismo"));
    public ReparadorRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Reparador> listar() { return jdbc.query(SELECT + " ORDER BY u.id", MAPPER); }
    public Optional<Reparador> obtener(String id) {
        return jdbc.query(SELECT + " AND LOWER(u.id) = LOWER(?)", MAPPER, id).stream().findFirst();
    }
}
