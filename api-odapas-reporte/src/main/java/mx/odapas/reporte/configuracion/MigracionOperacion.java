package mx.odapas.reporte.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Aplica la ampliación operativa (db/08_operacion.sql) al iniciar la API.
 * Es aditiva e idempotente: crea secuencias de folio, la bitácora y nuevos tipos
 * de aviso solo si no existen. Nunca borra datos.
 */
@Component
@Order(-90)
public class MigracionOperacion implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(MigracionOperacion.class);
    private final DataSource fuente;

    public MigracionOperacion(DataSource fuente) { this.fuente = fuente; }

    @Override
    public void run(ApplicationArguments args) {
        var script = new ResourceDatabasePopulator(new ClassPathResource("db/08_operacion.sql"));
        script.setSeparator("@@");
        script.setSqlScriptEncoding("UTF-8");
        script.execute(fuente);
        LOG.info("Ampliación operativa verificada (folios, bitácora y avisos).");
    }
}
