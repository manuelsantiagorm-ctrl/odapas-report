package com.aguapp.api.repository;

import com.aguapp.api.model.Colonia;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ColoniaRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Colonia> MAPPER = (rs, row) -> new Colonia(
            rs.getString("id"), rs.getString("nombre"), rs.getString("estado_servicio_codigo"));

    public ColoniaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Colonia> listar() {
        return jdbc.query("SELECT id, nombre, estado_servicio_codigo FROM colonia ORDER BY id", MAPPER);
    }

    public Optional<Colonia> obtener(String id) {
        return jdbc.query("SELECT id, nombre, estado_servicio_codigo FROM colonia WHERE LOWER(id) = LOWER(?)",
                MAPPER, id).stream().findFirst();
    }
}
