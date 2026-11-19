package mx.odapas.reporte.configuracion;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
@Component
@Order(-100)
public class MigracionPrivacidad implements ApplicationRunner {
 private final DataSource fuente;
 private final TransactionTemplate transaccion;
 public MigracionPrivacidad(DataSource fuente, PlatformTransactionManager gestor) {
  this.fuente=fuente; this.transaccion=new TransactionTemplate(gestor);
 }
 public void run(ApplicationArguments args) {
  transaccion.executeWithoutResult(s -> {
   new ResourceDatabasePopulator(new ClassPathResource("db/06_privacidad.sql")).execute(fuente);
   var jdbc=new org.springframework.jdbc.core.JdbcTemplate(fuente);
   var documentos=java.util.Map.of("privacidad",mx.odapas.reporte.privacidad.DocumentosLegales.PRIVACIDAD,
    "terminos",mx.odapas.reporte.privacidad.DocumentosLegales.TERMINOS);
   documentos.forEach((tipo,contenido)->{
    String version=mx.odapas.reporte.privacidad.DocumentosLegales.VERSION;
    jdbc.update("INSERT INTO public.documento_legal(documento,version,contenido) VALUES(?,?,?) ON CONFLICT DO NOTHING",tipo,version,contenido);
    String guardado=jdbc.queryForObject("SELECT contenido FROM public.documento_legal WHERE documento=? AND version=?",String.class,tipo,version);
    if(!contenido.equals(guardado)) throw new IllegalStateException("El texto legal cambió sin actualizar su versión: "+tipo);
   });
   if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.aceptacion_legal'::regclass AND conname='aceptacion_documento_fk')",Boolean.class)))
    jdbc.execute("ALTER TABLE public.aceptacion_legal ADD CONSTRAINT aceptacion_documento_fk FOREIGN KEY(documento,version) REFERENCES public.documento_legal(documento,version)");
  });
 }
}

