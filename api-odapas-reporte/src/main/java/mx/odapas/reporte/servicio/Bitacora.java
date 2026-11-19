package mx.odapas.reporte.servicio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Registra operaciones administrativas y de seguridad en public.bitacora_operacion. */
@Service
public class Bitacora {
    private final JdbcTemplate jdbc;

    public Bitacora(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void registrar(Integer actor, String accion, String entidad, String referencia, String detalle, String ip) {
        jdbc.update("INSERT INTO public.bitacora_operacion(actor_id,accion,entidad,referencia,detalle,ip) VALUES(?,?,?,?,?,?)",
                actor, accion, entidad, referencia,
                detalle == null ? null : detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle, ip);
    }
}
