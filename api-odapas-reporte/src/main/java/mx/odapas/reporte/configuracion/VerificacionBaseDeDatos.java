package mx.odapas.reporte.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VerificacionBaseDeDatos implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(VerificacionBaseDeDatos.class);
    private final JdbcTemplate jdbc;

    public VerificacionBaseDeDatos(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments argumentos) {
        jdbc.queryForList("SELECT codigo, colonias, trabajadores_disponibles FROM public.v_zonas LIMIT 0");
        jdbc.queryForList("SELECT folio, fecha_creacion, latitud, longitud FROM public.v_reportes LIMIT 0");
        LOG.info("Conexión PostgreSQL verificada. Base: {}", jdbc.queryForObject("SELECT current_database()", String.class));
    }
}
