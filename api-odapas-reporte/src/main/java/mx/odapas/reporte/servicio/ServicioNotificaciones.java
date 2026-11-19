package mx.odapas.reporte.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Bandeja de avisos internos persistentes. Cada usuario solo ve y marca los suyos. */
@Service
public class ServicioNotificaciones {
    private final JdbcTemplate jdbc;

    public ServicioNotificaciones(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> listar(int usuario, boolean soloNoLeidas) {
        return jdbc.queryForList("""
                SELECT n.id,n.tipo::text AS tipo,n.creado_en,n.leido_en,
                  COALESCE(r.folio,r2.folio) AS reporte_folio, o.folio AS orden_folio,
                  CASE n.tipo::text
                    WHEN 'nuevo_reporte_zona' THEN 'Nuevo reporte en la zona: '||r.folio
                    WHEN 'reporte_revisado' THEN 'Tu reporte '||r.folio||' fue revisado'
                    WHEN 'trabajo_asignado' THEN 'Se te asignó la orden '||o.folio
                    WHEN 'avance_reparacion' THEN 'La orden '||o.folio||' está en atención'
                    WHEN 'reparacion_realizada' THEN 'Reparación realizada en '||o.folio||' (pendiente de revisión)'
                    WHEN 'trabajo_cerrado' THEN 'Orden '||o.folio||' cerrada'
                    WHEN 'trabajo_devuelto' THEN 'La orden '||o.folio||' fue devuelta con observaciones'
                    WHEN 'orden_cancelada' THEN 'La orden '||o.folio||' fue cancelada'
                    ELSE 'Aviso' END AS titulo
                FROM public.notificacion n
                LEFT JOIN public.reporte r ON r.id=n.reporte_id
                LEFT JOIN public.orden_trabajo o ON o.id=n.orden_id
                LEFT JOIN public.reporte r2 ON r2.id=o.reporte_id
                WHERE n.destinatario_id=? AND (? = FALSE OR n.leido_en IS NULL)
                ORDER BY n.creado_en DESC,n.id DESC LIMIT 100""", usuario, soloNoLeidas);
    }

    public int noLeidas(int usuario) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM public.notificacion WHERE destinatario_id=? AND leido_en IS NULL", Integer.class, usuario);
        return n == null ? 0 : n;
    }

    public void marcar(int usuario, int id) {
        int filas = jdbc.update("UPDATE public.notificacion SET leido_en=GREATEST(CURRENT_TIMESTAMP,creado_en) " +
                "WHERE id=? AND destinatario_id=? AND leido_en IS NULL", id, usuario);
        if (filas == 0 && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM public.notificacion WHERE id=? AND destinatario_id=?)", Boolean.class, id, usuario)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso no encontrado");
    }

    public int marcarTodas(int usuario) {
        return jdbc.update("UPDATE public.notificacion SET leido_en=GREATEST(CURRENT_TIMESTAMP,creado_en) " +
                "WHERE destinatario_id=? AND leido_en IS NULL", usuario);
    }
}
