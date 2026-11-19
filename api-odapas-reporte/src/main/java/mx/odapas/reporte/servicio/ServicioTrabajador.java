package mx.odapas.reporte.servicio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Panel del trabajador reparador. Solo ve y modifica órdenes donde tiene una asignación;
 * el responsable principal registra el avance (la base también lo exige).
 */
@Service
public class ServicioTrabajador {
    public static final int MAX_FOTOS_FASE = 4;
    private final JdbcTemplate jdbc;
    private final ServicioArchivos archivos;
    private final ObjectMapper json;

    public ServicioTrabajador(JdbcTemplate jdbc, ServicioArchivos archivos, ObjectMapper json) {
        this.jdbc = jdbc;
        this.archivos = archivos;
        this.json = json;
    }

    public Map<String, Object> resumen(int trabajador) {
        var r = new LinkedHashMap<String, Object>();
        var zona = jdbc.queryForList("""
                SELECT z.id,z.codigo,z.nombre,z.descripcion,COALESCE(array_to_json(vz.colonias)::text,'[]') AS colonias FROM public.trabajador_zona tz
                JOIN public.zona z ON z.id=tz.zona_id JOIN public.v_zonas vz ON vz.id=z.id
                WHERE tz.trabajador_id=? AND tstzrange(tz.fecha_inicio,tz.fecha_fin,'[)') @> CURRENT_TIMESTAMP""", trabajador);
        Datos.jsonEnLista(zona, "colonias");
        r.put("zona", zona.isEmpty() ? null : zona.get(0));
        r.put("perfil", jdbc.queryForMap("""
                SELECT p.nombre_completo AS nombre,t.numero_empleado FROM public.trabajador t JOIN public.usuario u ON u.id=t.usuario_id
                JOIN public.persona p ON p.id=u.persona_id WHERE t.usuario_id=?""", trabajador));
        r.put("conteos", jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE a.finalizado_en IS NULL AND o.finalizado_en IS NULL) AS activas,
                       COUNT(*) FILTER (WHERE a.finalizado_en IS NULL AND o.finalizado_en IS NULL AND a.recibido_en IS NULL) AS sin_recibir,
                       COUNT(*) FILTER (WHERE o.finalizado_en IS NOT NULL) AS terminadas
                FROM public.asignacion_trabajo a JOIN public.orden_trabajo o ON o.id=a.orden_id WHERE a.trabajador_id=?""", trabajador));
        // Incidentes de la zona: información operativa limitada, sin datos del ciudadano.
        r.put("incidentesZona", zona.isEmpty() ? List.of() : jdbc.queryForList("""
                SELECT folio,tipo,colonia,estado,latitud,longitud,fecha_creacion FROM public.v_reportes
                WHERE zona=? AND estado NOT IN ('rechazado','cancelado','cerrada','cancelada')
                ORDER BY fecha_creacion DESC LIMIT 50""", zona.get(0).get("codigo")));
        return r;
    }

    public List<Map<String, Object>> ordenes(int trabajador) {
        return jdbc.queryForList("""
                SELECT o.folio,r.folio AS reporte_folio,tr.codigo AS tipo,tr.nombre AS tipo_nombre,r.descripcion,
                  c.nombre AS colonia,d.calle||' '||d.numero_exterior AS calle,d.latitud,d.longitud,
                  o.prioridad::text AS prioridad,o.fecha_programada,o.creado_en,o.finalizado_en,
                  (SELECT h.estado::text FROM public.historial_orden h WHERE h.orden_id=o.id ORDER BY h.secuencia DESC LIMIT 1) AS estado,
                  a.responsable_principal,a.recibido_en,a.finalizado_en AS asignacion_finalizada
                FROM public.asignacion_trabajo a JOIN public.orden_trabajo o ON o.id=a.orden_id
                JOIN public.reporte r ON r.id=o.reporte_id JOIN public.tipo_reporte tr ON tr.id=r.tipo_reporte_id
                JOIN public.direccion d ON d.id=r.direccion_incidente_id JOIN public.colonia c ON c.id=d.colonia_id
                WHERE a.trabajador_id=? AND a.id=(SELECT MAX(a2.id) FROM public.asignacion_trabajo a2 WHERE a2.orden_id=o.id AND a2.trabajador_id=?)
                ORDER BY (o.finalizado_en IS NULL AND a.finalizado_en IS NULL) DESC,
                  CASE o.prioridad WHEN 'urgente' THEN 0 WHEN 'alta' THEN 1 WHEN 'media' THEN 2 ELSE 3 END,o.creado_en DESC
                LIMIT 200""", trabajador, trabajador);
    }

    /** Asignación vigente del trabajador en la orden (bloqueada para actualizar). */
    private Map<String, Object> asignacion(int trabajador, String folio) {
        var filas = jdbc.queryForList("""
                SELECT a.id,a.orden_id,a.responsable_principal,a.recibido_en,
                  (SELECT h.estado::text FROM public.historial_orden h WHERE h.orden_id=a.orden_id ORDER BY h.secuencia DESC LIMIT 1) AS estado
                FROM public.asignacion_trabajo a JOIN public.orden_trabajo o ON o.id=a.orden_id
                WHERE UPPER(o.folio)=? AND a.trabajador_id=? AND a.finalizado_en IS NULL AND o.finalizado_en IS NULL
                FOR UPDATE OF a""", Datos.folio(folio), trabajador);
        if (filas.isEmpty())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes una asignación vigente en esa orden");
        return filas.get(0);
    }

    private Map<String, Object> responsable(int trabajador, String folio) {
        var a = asignacion(trabajador, folio);
        if (!Boolean.TRUE.equals(a.get("responsable_principal")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el responsable principal registra el avance de la orden");
        return a;
    }

    private Integer reparacionAbierta(int asignacion) {
        var r = jdbc.queryForList("SELECT id FROM public.reparacion WHERE asignacion_id=? AND terminado_en IS NULL ORDER BY id DESC LIMIT 1", Integer.class, asignacion);
        return r.isEmpty() ? null : r.get(0);
    }

    @Transactional
    public Map<String, Object> recibir(int trabajador, String folio) {
        var a = asignacion(trabajador, folio);
        jdbc.update("UPDATE public.asignacion_trabajo SET recibido_en=COALESCE(recibido_en,GREATEST(CURRENT_TIMESTAMP,asignado_en)) WHERE id=?", a.get("id"));
        return Map.of("folio", folio.toUpperCase(), "mensaje", "Recepción confirmada");
    }

    /** Inicia la atención con el diagnóstico (o abre una nueva intervención si la orden fue devuelta). */
    @Transactional
    public Map<String, Object> iniciar(int trabajador, String folio, Map<String, Object> d) {
        var a = responsable(trabajador, folio);
        String diagnostico = Datos.texto(d, "diagnostico", 5, 2000);
        int asignacion = ((Number) a.get("id")).intValue();
        String estado = (String) a.get("estado");
        if (!List.of("asignada", "en_proceso").contains(estado))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La orden está en estado " + estado + " y no admite iniciar trabajo");
        if (reparacionAbierta(asignacion) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay una intervención abierta; regístrala como terminada");
        jdbc.update("UPDATE public.asignacion_trabajo SET recibido_en=COALESCE(recibido_en,GREATEST(CURRENT_TIMESTAMP,asignado_en)) WHERE id=?", asignacion);
        jdbc.update("INSERT INTO public.reparacion(asignacion_id,diagnostico,iniciado_en) VALUES(?,?,GREATEST(CURRENT_TIMESTAMP,(SELECT asignado_en FROM public.asignacion_trabajo WHERE id=?)))",
                asignacion, diagnostico, asignacion);
        if ("asignada".equals(estado)) {
            int orden = ((Number) a.get("orden_id")).intValue();
            Integer secuencia = jdbc.queryForObject("SELECT COALESCE(MAX(secuencia),0)+1 FROM public.historial_orden WHERE orden_id=?", Integer.class, orden);
            jdbc.update("INSERT INTO public.historial_orden(orden_id,secuencia,estado,realizado_por,observaciones) VALUES(?,?,'en_proceso',?,?)",
                    orden, secuencia, trabajador, "Diagnóstico: " + (diagnostico.length() > 900 ? diagnostico.substring(0, 900) : diagnostico));
        }
        return Map.of("folio", folio.toUpperCase(), "estado", "en_proceso");
    }

    @Transactional
    public Map<String, Object> fotos(int trabajador, String folio, String fase, List<MultipartFile> fotos) {
        if (!List.of("antes", "despues").contains(fase)) throw Datos.invalido("La fase debe ser antes o despues");
        var a = asignacion(trabajador, folio);
        Integer reparacion = reparacionAbierta(((Number) a.get("id")).intValue());
        if (reparacion == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Inicia la atención con el diagnóstico antes de subir fotografías");
        Integer actuales = jdbc.queryForObject("SELECT COUNT(*) FROM public.fotografia_reparacion WHERE reparacion_id=? AND fase=?::public.fase_fotografia",
                Integer.class, reparacion, fase);
        if (fotos == null || fotos.isEmpty()) throw Datos.invalido("Adjunta al menos una fotografía");
        if (actuales + fotos.size() > MAX_FOTOS_FASE) throw Datos.invalido("Máximo " + MAX_FOTOS_FASE + " fotografías por fase");
        for (var f : fotos) {
            try {
                jdbc.update("INSERT INTO public.fotografia_reparacion(reparacion_id,fase,foto_json) VALUES(?,?::public.fase_fotografia,?::jsonb)",
                        reparacion, fase, json.writeValueAsString(archivos.guardar(f)));
            } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
        }
        return Map.of("folio", folio.toUpperCase(), "fase", fase, "total", actuales + fotos.size());
    }

    /**
     * Registra el trabajo realizado y el resultado. Exige al menos una foto "después"
     * o una justificación escrita. Deja la orden en "reparación realizada" para revisión del administrador.
     */
    @Transactional
    public Map<String, Object> terminar(int trabajador, String folio, Map<String, Object> d) {
        var a = responsable(trabajador, folio);
        if (!"en_proceso".equals(a.get("estado")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inicia la atención antes de registrar el resultado");
        int asignacion = ((Number) a.get("id")).intValue();
        Integer reparacion = reparacionAbierta(asignacion);
        if (reparacion == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay una intervención abierta; registra el diagnóstico primero");
        String trabajo = Datos.texto(d, "trabajoRealizado", 5, 1800);
        String resultado = Datos.opcion(d, "resultado", List.of("resuelto", "parcial", "no_resuelto"));
        Integer fotosDespues = jdbc.queryForObject("SELECT COUNT(*) FROM public.fotografia_reparacion WHERE reparacion_id=? AND fase='despues'", Integer.class, reparacion);
        String justificacion = Datos.textoOpcional(d, "justificacionSinFoto", 190);
        if ((fotosDespues == null || fotosDespues == 0)) {
            if (justificacion == null || justificacion.length() < 10)
                throw Datos.invalido("Sube una foto de \"después\" o explica por qué no fue posible (mínimo 10 caracteres)");
            trabajo = trabajo + " · Sin foto posterior: " + justificacion;
        }
        jdbc.update("UPDATE public.reparacion SET trabajo_realizado=?,resultado=?::public.resultado_reparacion," +
                "terminado_en=GREATEST(CURRENT_TIMESTAMP,iniciado_en) WHERE id=?", trabajo, resultado, reparacion);
        int orden = ((Number) a.get("orden_id")).intValue();
        Integer secuencia = jdbc.queryForObject("SELECT COALESCE(MAX(secuencia),0)+1 FROM public.historial_orden WHERE orden_id=?", Integer.class, orden);
        jdbc.update("INSERT INTO public.historial_orden(orden_id,secuencia,estado,realizado_por,observaciones) VALUES(?,?,'reparacion_realizada',?,?)",
                orden, secuencia, trabajador, "Resultado: " + resultado);
        return Map.of("folio", folio.toUpperCase(), "estado", "reparacion_realizada", "resultado", resultado);
    }
}
