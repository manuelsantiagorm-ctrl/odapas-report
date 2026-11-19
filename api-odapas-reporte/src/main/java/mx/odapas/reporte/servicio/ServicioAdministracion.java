package mx.odapas.reporte.servicio;

import mx.odapas.reporte.privacidad.ServicioCuenta;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

/**
 * Operación del administrador: revisión de reportes, órdenes de trabajo, personal, zonas,
 * estadísticas y bitácora. Las reglas de negocio críticas (una orden activa por reporte, un
 * responsable, personal de la zona, transiciones válidas) también las hace cumplir PostgreSQL.
 */
@Service
public class ServicioAdministracion {
    private static final List<String> PRIORIDADES = List.of("baja", "media", "alta", "urgente");
    private final JdbcTemplate jdbc;
    private final ServicioEnlaces enlaces;
    private final Bitacora bitacora;

    public ServicioAdministracion(JdbcTemplate jdbc, ServicioEnlaces enlaces, Bitacora bitacora) {
        this.jdbc = jdbc;
        this.enlaces = enlaces;
        this.bitacora = bitacora;
    }

    private static ResponseStatusException noEncontrado(String que) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, que + " no encontrado");
    }

    private static ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    // ------------------------------------------------------------------ Estadísticas

    public Map<String, Object> estadisticas() {
        var r = new LinkedHashMap<String, Object>();
        r.put("reportes", jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                  COUNT(*) FILTER (WHERE estado IN ('recibido','en_revision')) AS por_revisar,
                  COUNT(*) FILTER (WHERE estado='validado') AS validados_sin_orden,
                  COUNT(*) FILTER (WHERE estado IN ('asignado','en_proceso')) AS en_atencion,
                  COUNT(*) FILTER (WHERE estado='reparacion_realizada') AS por_cerrar,
                  COUNT(*) FILTER (WHERE estado='cerrada') AS cerrados,
                  COUNT(*) FILTER (WHERE estado IN ('rechazado','cancelado','cancelada')) AS no_procedentes,
                  COUNT(*) FILTER (WHERE zona IS NULL) AS sin_zona,
                  COUNT(*) FILTER (WHERE fecha_creacion > CURRENT_TIMESTAMP - INTERVAL '7 days') AS ultimos_7_dias
                FROM public.v_reportes"""));
        r.put("porTipo", jdbc.queryForList("""
                SELECT t.codigo,t.nombre,COUNT(r.id) AS total FROM public.tipo_reporte t
                LEFT JOIN public.reporte r ON r.tipo_reporte_id=t.id
                WHERE t.codigo NOT LIKE 'carga\\_%' GROUP BY t.id ORDER BY t.id"""));
        r.put("porEstado", jdbc.queryForList("SELECT estado,COUNT(*) AS total FROM public.v_reportes GROUP BY estado ORDER BY total DESC"));
        r.put("porDia", jdbc.queryForList("""
                SELECT d::date AS dia,(SELECT COUNT(*) FROM public.reporte r
                  WHERE (r.creado_en AT TIME ZONE 'America/Mexico_City')::date=d::date) AS total
                FROM generate_series((CURRENT_TIMESTAMP AT TIME ZONE 'America/Mexico_City')::date - 13,
                                     (CURRENT_TIMESTAMP AT TIME ZONE 'America/Mexico_City')::date, INTERVAL '1 day') d ORDER BY d"""));
        r.put("zonas", jdbc.queryForList("""
                WITH abiertos AS (SELECT zona,COUNT(*) AS n FROM public.v_reportes
                       WHERE estado NOT IN ('rechazado','cancelado','cerrada','cancelada') GROUP BY zona),
                     activas AS (SELECT zona_responsable_id AS zona_id,COUNT(*) AS n FROM public.orden_trabajo
                       WHERE finalizado_en IS NULL GROUP BY zona_responsable_id)
                SELECT z.codigo,z.nombre,COALESCE(a.n,0) AS abiertos,COALESCE(o.n,0) AS ordenes_activas,vz.trabajadores_disponibles
                FROM public.zona z JOIN public.v_zonas vz ON vz.id=z.id
                LEFT JOIN abiertos a ON a.zona=z.codigo LEFT JOIN activas o ON o.zona_id=z.id
                WHERE z.activa ORDER BY COALESCE(a.n,0) DESC,COALESCE(o.n,0) DESC,z.codigo LIMIT 8"""));
        r.put("personal", jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE u.estado='activo') AS activos,
                       COUNT(*) FILTER (WHERE u.estado='pendiente') AS invitados,
                       COUNT(*) FILTER (WHERE u.estado='suspendido') AS suspendidos
                FROM public.usuario u JOIN public.usuario_rol ur ON ur.usuario_id=u.id AND ur.rol='trabajador'"""));
        return r;
    }

    // ------------------------------------------------------------------ Reportes

    public Map<String, Object> reportes(String estado, String tipo, String zona, String q, int pagina, int tamano) {
        var where = new StringBuilder(" WHERE 1=1");
        var params = new ArrayList<Object>();
        if (estado != null && !estado.isBlank()) {
            if (estado.equals("pendientes")) where.append(" AND v.estado IN ('recibido','en_revision','validado')");
            else if (estado.equals("activos")) where.append(" AND v.estado NOT IN ('rechazado','cancelado','cerrada','cancelada')");
            else { where.append(" AND v.estado=?"); params.add(estado); }
        }
        if (tipo != null && !tipo.isBlank()) { where.append(" AND v.tipo=?"); params.add(tipo); }
        if (zona != null && !zona.isBlank()) {
            if (zona.equals("sin_zona")) where.append(" AND v.zona IS NULL");
            else { where.append(" AND v.zona=?"); params.add(zona.toUpperCase()); }
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (v.folio ILIKE ? OR v.colonia ILIKE ? OR v.descripcion ILIKE ?)");
            String p = "%" + q.trim().replace("%", "\\%").replace("_", "\\_") + "%";
            params.add(p); params.add(p); params.add(p);
        }
        tamano = Math.max(5, Math.min(tamano, 100));
        pagina = Math.max(1, pagina);
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM public.v_reportes v" + where, Integer.class, params.toArray());
        var datos = new ArrayList<>(params);
        datos.add(tamano);
        datos.add((pagina - 1) * tamano);
        var filas = jdbc.queryForList("SELECT v.folio,v.tipo,v.descripcion,v.colonia,v.calle,v.zona,v.latitud,v.longitud,v.estado," +
                "v.prioridad,v.fecha_creacion FROM public.v_reportes v" + where +
                " ORDER BY v.fecha_creacion DESC,v.folio LIMIT ? OFFSET ?", datos.toArray());
        return Map.of("total", total == null ? 0 : total, "pagina", pagina, "tamano", tamano, "datos", filas);
    }

    private int idReporte(String folio) {
        var ids = jdbc.queryForList("SELECT id FROM public.reporte WHERE UPPER(folio)=? FOR UPDATE", Integer.class, Datos.folio(folio));
        if (ids.isEmpty()) throw noEncontrado("Reporte");
        return ids.get(0);
    }

    /** Revisión: en_revision, validado, rechazado o cancelado (con motivo). Notifica al ciudadano. */
    @Transactional
    public Map<String, Object> revisar(int admin, String folio, Map<String, Object> d, String ip) {
        int id = idReporte(folio);
        String estado = Datos.opcion(d, "estado", List.of("en_revision", "validado", "rechazado", "cancelado"));
        String observaciones = Datos.textoOpcional(d, "observaciones", 1000);
        if (List.of("rechazado", "cancelado").contains(estado) && observaciones == null)
            throw Datos.invalido("Indica el motivo para rechazar o cancelar");
        if (estado.equals("cancelado") && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM public.orden_trabajo WHERE reporte_id=? AND finalizado_en IS NULL)", Boolean.class, id)))
            throw conflicto("Cancela primero la orden de trabajo activa de este reporte");
        Integer secuencia = jdbc.queryForObject("SELECT COALESCE(MAX(secuencia),0)+1 FROM public.historial_reporte WHERE reporte_id=?", Integer.class, id);
        // El trigger revisar_reporte valida la transición y que el actor sea administrador.
        jdbc.update("INSERT INTO public.historial_reporte(reporte_id,secuencia,estado,realizado_por,observaciones) VALUES(?,?,?::public.estado_reporte,?,?)",
                id, secuencia, estado, admin, observaciones);
        int autor = jdbc.queryForObject("SELECT ciudadano_id FROM public.reporte WHERE id=?", Integer.class, id);
        jdbc.update("INSERT INTO public.notificacion(destinatario_id,tipo,reporte_id) VALUES(?,'reporte_revisado',?)", autor, id);
        bitacora.registrar(admin, "revision_reporte", "reporte", folio.toUpperCase(), estado + (observaciones == null ? "" : ": " + observaciones), ip);
        return Map.of("folio", folio.toUpperCase(), "estado", estado);
    }

    /** Vuelve a clasificar la zona del reporte según la cobertura vigente de su colonia. */
    @Transactional
    public Map<String, Object> clasificar(int admin, String folio, String ip) {
        int id = idReporte(folio);
        var cobertura = jdbc.queryForList("""
                SELECT cz.id,z.codigo FROM public.reporte r JOIN public.direccion d ON d.id=r.direccion_incidente_id
                JOIN public.cobertura_zona cz ON cz.colonia_id=d.colonia_id JOIN public.zona z ON z.id=cz.zona_id
                WHERE r.id=? AND z.activa AND tstzrange(cz.fecha_inicio,cz.fecha_fin,'[)') @> CURRENT_TIMESTAMP""", id);
        if (cobertura.isEmpty())
            throw conflicto("La colonia del incidente no tiene zona activa. Asígnala primero en Zonas y cobertura.");
        int cz = ((Number) cobertura.get(0).get("id")).intValue();
        var actual = jdbc.queryForList("SELECT cobertura_zona_id FROM public.reporte_zona WHERE reporte_id=? AND finalizado_en IS NULL", Integer.class, id);
        if (!actual.isEmpty() && actual.get(0) == cz) return Map.of("folio", folio.toUpperCase(), "zona", cobertura.get(0).get("codigo"));
        jdbc.update("UPDATE public.reporte_zona SET finalizado_en=CURRENT_TIMESTAMP,motivo=COALESCE(motivo,'')||' · Reclasificado' " +
                "WHERE reporte_id=? AND finalizado_en IS NULL AND asignado_en<CURRENT_TIMESTAMP", id);
        jdbc.update("INSERT INTO public.reporte_zona(reporte_id,cobertura_zona_id,asignado_por,motivo) VALUES(?,?,?,'Clasificación por administrador')",
                id, cz, admin);
        bitacora.registrar(admin, "clasificar_reporte", "reporte", folio.toUpperCase(), "Zona " + cobertura.get(0).get("codigo"), ip);
        return Map.of("folio", folio.toUpperCase(), "zona", cobertura.get(0).get("codigo"));
    }

    // ------------------------------------------------------------------ Órdenes

    public List<Map<String, Object>> ordenes(String estado, String zona) {
        var where = new StringBuilder(" WHERE 1=1");
        var params = new ArrayList<Object>();
        if ("activas".equals(estado)) where.append(" AND o.finalizado_en IS NULL");
        else if (estado != null && !estado.isBlank()) { where.append(" AND o.estado::text=?"); params.add(estado); }
        if (zona != null && !zona.isBlank()) { where.append(" AND o.zona=?"); params.add(zona.toUpperCase()); }
        return jdbc.queryForList("""
                SELECT o.folio,o.reporte_folio,o.zona,o.prioridad::text AS prioridad,o.fecha_programada,o.estado::text AS estado,
                  o.creado_en,o.finalizado_en,tr.codigo AS tipo,c.nombre AS colonia,
                  (SELECT p.nombre_completo FROM public.asignacion_trabajo a JOIN public.usuario u ON u.id=a.trabajador_id
                   JOIN public.persona p ON p.id=u.persona_id JOIN public.orden_trabajo ot ON ot.id=a.orden_id
                   WHERE ot.folio=o.folio AND a.responsable_principal ORDER BY a.finalizado_en DESC NULLS FIRST,a.id DESC LIMIT 1) AS responsable
                FROM public.v_ordenes o JOIN public.reporte r ON r.folio=o.reporte_folio
                JOIN public.tipo_reporte tr ON tr.id=r.tipo_reporte_id JOIN public.direccion d ON d.id=r.direccion_incidente_id
                JOIN public.colonia c ON c.id=d.colonia_id""" + where +
                " ORDER BY (o.finalizado_en IS NULL) DESC, CASE o.prioridad WHEN 'urgente' THEN 0 WHEN 'alta' THEN 1 WHEN 'media' THEN 2 ELSE 3 END, o.creado_en DESC LIMIT 300",
                params.toArray());
    }

    /** Personal elegible para la orden de un reporte: activo y adscrito hoy a la zona del reporte. */
    public Map<String, Object> elegibles(String folio) {
        var zona = jdbc.queryForList("""
                SELECT z.id,z.codigo,z.nombre FROM public.reporte r JOIN public.reporte_zona rz ON rz.reporte_id=r.id AND rz.finalizado_en IS NULL
                JOIN public.cobertura_zona cz ON cz.id=rz.cobertura_zona_id JOIN public.zona z ON z.id=cz.zona_id
                WHERE UPPER(r.folio)=?""", Datos.folio(folio));
        if (zona.isEmpty()) return Map.of("zona", Map.of(), "trabajadores", List.of());
        var trabajadores = jdbc.queryForList("""
                SELECT u.id,p.nombre_completo AS nombre,t.numero_empleado,
                  (SELECT COUNT(*) FROM public.asignacion_trabajo a JOIN public.orden_trabajo o ON o.id=a.orden_id
                   WHERE a.trabajador_id=u.id AND a.finalizado_en IS NULL AND o.finalizado_en IS NULL) AS carga
                FROM public.trabajador_zona tz JOIN public.usuario u ON u.id=tz.trabajador_id
                JOIN public.trabajador t ON t.usuario_id=u.id JOIN public.persona p ON p.id=u.persona_id
                JOIN public.usuario_rol ur ON ur.usuario_id=u.id AND ur.rol='trabajador'
                WHERE tz.zona_id=? AND u.estado='activo' AND tstzrange(tz.fecha_inicio,tz.fecha_fin,'[)') @> CURRENT_TIMESTAMP
                ORDER BY carga,p.nombre_completo""", zona.get(0).get("id"));
        return Map.of("zona", zona.get(0), "trabajadores", trabajadores);
    }

    /** Crea la orden con responsable principal y colaboradores en una sola transacción. */
    @Transactional
    public Map<String, Object> crearOrden(int admin, Map<String, Object> d, String ip) {
        String folioReporte = Datos.folio(Datos.texto(d, "reporteFolio", 1, 30));
        int reporte = idReporte(folioReporte);
        String prioridad = Datos.opcion(d, "prioridad", PRIORIDADES);
        LocalDate programada = Datos.fechaOpcional(d, "fechaProgramada");
        if (programada != null && programada.isBefore(LocalDate.now().minusDays(1))) throw Datos.invalido("La fecha programada no puede estar en el pasado");
        int responsable = Datos.entero(d, "responsableId");
        var colaboradores = Datos.enteros(d, "colaboradores", 10);
        colaboradores.remove(Integer.valueOf(responsable));

        String ultimo = jdbc.queryForObject("SELECT estado::text FROM public.historial_reporte WHERE reporte_id=? ORDER BY secuencia DESC LIMIT 1", String.class, reporte);
        if (!"validado".equals(ultimo)) throw conflicto("Valida el reporte antes de crear la orden de trabajo");
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.orden_trabajo WHERE reporte_id=? AND finalizado_en IS NULL)", Boolean.class, reporte)))
            throw conflicto("Este reporte ya tiene una orden activa");
        var zona = jdbc.queryForList("""
                SELECT cz.zona_id FROM public.reporte_zona rz JOIN public.cobertura_zona cz ON cz.id=rz.cobertura_zona_id
                WHERE rz.reporte_id=? AND rz.finalizado_en IS NULL""", Integer.class, reporte);
        if (zona.isEmpty()) throw conflicto("El reporte no tiene zona. Asigna cobertura a su colonia y reclasifícalo.");

        String folio = jdbc.queryForObject("SELECT 'OT-'||LPAD(nextval('public.folio_orden_seq')::text,6,'0')", String.class);
        int orden = jdbc.queryForObject("""
                INSERT INTO public.orden_trabajo(folio,reporte_id,zona_responsable_id,prioridad,creado_por,fecha_programada)
                VALUES(?,?,?,?::public.prioridad_reporte,?,?) RETURNING id""", Integer.class,
                folio, reporte, zona.get(0), prioridad, admin, programada);
        // Los triggers validan que cada trabajador esté activo y adscrito a la zona de la orden.
        jdbc.update("INSERT INTO public.asignacion_trabajo(orden_id,trabajador_id,responsable_principal,asignado_por) VALUES(?,?,TRUE,?)", orden, responsable, admin);
        for (int c : colaboradores)
            jdbc.update("INSERT INTO public.asignacion_trabajo(orden_id,trabajador_id,responsable_principal,asignado_por) VALUES(?,?,FALSE,?)", orden, c, admin);
        jdbc.update("INSERT INTO public.historial_orden(orden_id,secuencia,estado,realizado_por,observaciones) VALUES(?,1,'asignada',?,?)",
                orden, admin, Datos.textoOpcional(d, "observaciones", 1000));
        bitacora.registrar(admin, "crear_orden", "orden", folio, "Reporte " + folioReporte + " · prioridad " + prioridad +
                " · responsable " + responsable + (colaboradores.isEmpty() ? "" : " · colaboradores " + colaboradores), ip);
        return Map.of("folio", folio, "reporteFolio", folioReporte);
    }

    private Map<String, Object> orden(String folio) {
        var filas = jdbc.queryForList("""
                SELECT o.id,o.reporte_id,o.finalizado_en,(SELECT estado::text FROM public.historial_orden h WHERE h.orden_id=o.id
                  ORDER BY secuencia DESC LIMIT 1) AS estado FROM public.orden_trabajo o WHERE UPPER(o.folio)=? FOR UPDATE""", Datos.folio(folio));
        if (filas.isEmpty()) throw noEncontrado("Orden");
        return filas.get(0);
    }

    /** cerrar (requiere reparación resuelta), devolver al responsable (en_proceso) o cancelar (con motivo). */
    @Transactional
    public Map<String, Object> estadoOrden(int admin, String folio, Map<String, Object> d, String ip) {
        var o = orden(folio);
        int id = ((Number) o.get("id")).intValue();
        String accion = Datos.opcion(d, "accion", List.of("cerrar", "devolver", "cancelar"));
        String observaciones = Datos.textoOpcional(d, "observaciones", 1000);
        String estado = switch (accion) { case "cerrar" -> "cerrada"; case "devolver" -> "en_proceso"; default -> "cancelada"; };
        if (!accion.equals("cerrar") && observaciones == null) throw Datos.invalido("Indica las observaciones o el motivo");
        if (accion.equals("devolver") && !"reparacion_realizada".equals(o.get("estado")))
            throw conflicto("Solo se puede devolver una orden con reparación realizada");
        if (accion.equals("cerrar") && !"reparacion_realizada".equals(o.get("estado")))
            throw conflicto("Solo se puede cerrar una orden con reparación realizada");
        if (accion.equals("cerrar") && !Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM public.reparacion r JOIN public.asignacion_trabajo a ON a.id=r.asignacion_id
                WHERE a.orden_id=? AND r.resultado='resuelto')""", Boolean.class, id)))
            throw conflicto("No se puede cerrar: la reparación no quedó resuelta. Devuélvela al responsable o cancela la orden.");
        Integer secuencia = jdbc.queryForObject("SELECT COALESCE(MAX(secuencia),0)+1 FROM public.historial_orden WHERE orden_id=?", Integer.class, id);
        jdbc.update("INSERT INTO public.historial_orden(orden_id,secuencia,estado,realizado_por,observaciones) VALUES(?,?,?::public.estado_orden,?,?)",
                id, secuencia, estado, admin, observaciones);
        if (accion.equals("devolver"))
            jdbc.update("""
                    INSERT INTO public.notificacion(destinatario_id,tipo,orden_id)
                    SELECT a.trabajador_id,'trabajo_devuelto',a.orden_id FROM public.asignacion_trabajo a
                    WHERE a.orden_id=? AND a.finalizado_en IS NULL""", id);
        if (accion.equals("cancelar"))
            jdbc.update("""
                    INSERT INTO public.notificacion(destinatario_id,tipo,orden_id)
                    SELECT DISTINCT d.x,'orden_cancelada'::public.tipo_notificacion,? FROM (
                      SELECT r.ciudadano_id AS x FROM public.reporte r WHERE r.id=?
                      UNION SELECT a.trabajador_id FROM public.asignacion_trabajo a WHERE a.orden_id=?) d""",
                    id, o.get("reporte_id"), id);
        bitacora.registrar(admin, "orden_" + accion, "orden", folio.toUpperCase(), observaciones, ip);
        return Map.of("folio", folio.toUpperCase(), "estado", estado);
    }

    /** Cambia al responsable principal conservando el historial de asignaciones. */
    @Transactional
    public Map<String, Object> reasignar(int admin, String folio, Map<String, Object> d, String ip) {
        var o = orden(folio);
        if (o.get("finalizado_en") != null) throw conflicto("La orden ya terminó");
        int id = ((Number) o.get("id")).intValue();
        int nuevo = Datos.entero(d, "responsableId");
        String motivo = Datos.texto(d, "motivo", 5, 500);
        jdbc.update("UPDATE public.asignacion_trabajo SET finalizado_en=CURRENT_TIMESTAMP,motivo_fin=? " +
                "WHERE orden_id=? AND finalizado_en IS NULL AND (responsable_principal OR trabajador_id=?)", "Reasignación: " + motivo, id, nuevo);
        jdbc.update("INSERT INTO public.asignacion_trabajo(orden_id,trabajador_id,responsable_principal,asignado_por) VALUES(?,?,TRUE,?)", id, nuevo, admin);
        bitacora.registrar(admin, "reasignar_orden", "orden", folio.toUpperCase(), "Nuevo responsable " + nuevo + ": " + motivo, ip);
        return Map.of("folio", folio.toUpperCase(), "responsableId", nuevo);
    }

    // ------------------------------------------------------------------ Personal

    public List<Map<String, Object>> trabajadores() {
        return jdbc.queryForList("""
                SELECT u.id,p.nombre_completo AS nombre,c.correo,c.telefono,t.numero_empleado,u.estado::text AS estado,t.registrado_en,
                  z.id AS zona_id,z.codigo AS zona,z.nombre AS zona_nombre,
                  (SELECT COUNT(*) FROM public.asignacion_trabajo a JOIN public.orden_trabajo o ON o.id=a.orden_id
                   WHERE a.trabajador_id=u.id AND a.finalizado_en IS NULL AND o.finalizado_en IS NULL) AS ordenes_activas
                FROM public.trabajador t JOIN public.usuario u ON u.id=t.usuario_id JOIN public.persona p ON p.id=u.persona_id
                JOIN public.contacto c ON c.persona_id=p.id
                LEFT JOIN public.trabajador_zona tz ON tz.trabajador_id=u.id AND tstzrange(tz.fecha_inicio,tz.fecha_fin,'[)') @> CURRENT_TIMESTAMP
                LEFT JOIN public.zona z ON z.id=tz.zona_id
                ORDER BY (u.estado='activo') DESC,p.nombre_completo LIMIT 500""");
    }

    private void zonaActiva(int zona) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.zona WHERE id=? AND activa)", Boolean.class, zona)))
            throw Datos.invalido("Selecciona una zona activa");
    }

    /**
     * Alta de trabajador por el administrador (no existe registro público de personal).
     * Si el correo ya tiene cuenta, se agrega el perfil laboral sin duplicar credenciales.
     */
    @Transactional
    public Map<String, Object> altaTrabajador(int admin, Map<String, Object> d, String ip) {
        String correo = ServicioCuenta.correo(d);
        String nombre = Datos.texto(d, "nombre", 2, 150);
        String telefono = Datos.texto(d, "telefono", 5, 20);
        String empleado = Datos.texto(d, "numeroEmpleado", 1, 30).toUpperCase();
        int zona = Datos.entero(d, "zonaId");
        zonaActiva(zona);
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.trabajador WHERE numero_empleado=?)", Boolean.class, empleado)))
            throw conflicto("Ya existe un trabajador con ese número de empleado");
        var existente = jdbc.queryForList("SELECT u.id,u.estado::text AS estado FROM public.usuario u JOIN public.contacto c ON c.persona_id=u.persona_id WHERE c.correo=?", correo);
        int usuario;
        String url = null;
        if (!existente.isEmpty()) {
            usuario = ((Number) existente.get(0).get("id")).intValue();
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.trabajador WHERE usuario_id=?)", Boolean.class, usuario)))
                throw conflicto("Ese correo ya pertenece a un trabajador");
            if (!"activo".equals(existente.get(0).get("estado"))) throw conflicto("La cuenta con ese correo no está activa");
        } else {
            int persona = jdbc.queryForObject("INSERT INTO public.persona(nombre_completo) VALUES(?) RETURNING id", Integer.class, nombre);
            jdbc.update("INSERT INTO public.contacto(persona_id,correo,telefono) VALUES(?,?,?)", persona, correo, telefono);
            usuario = jdbc.queryForObject("INSERT INTO public.usuario(persona_id,estado) VALUES(?,'pendiente') RETURNING id", Integer.class, persona);
        }
        jdbc.update("INSERT INTO public.usuario_rol(usuario_id,rol) VALUES(?,'trabajador') ON CONFLICT DO NOTHING", usuario);
        jdbc.update("INSERT INTO public.trabajador(usuario_id,numero_empleado,registrado_por) VALUES(?,?,?)", usuario, empleado, admin);
        jdbc.update("INSERT INTO public.trabajador_zona(trabajador_id,zona_id,asignado_por,fecha_inicio) VALUES(?,?,?,CURRENT_TIMESTAMP)", usuario, zona, admin);
        if (existente.isEmpty()) url = enlaces.emitir(usuario, "activar_trabajador", admin, ServicioEnlaces.VIGENCIA_ACTIVAR);
        bitacora.registrar(admin, "alta_trabajador", "usuario", String.valueOf(usuario), empleado + " · zona " + zona, ip);
        var r = new LinkedHashMap<String, Object>(enlaces.respuesta(existente.isEmpty()
                ? "Trabajador registrado. Envíale la invitación de activación (vigencia 24 horas)."
                : "Se agregó el perfil de trabajador a la cuenta existente; usa la misma contraseña.", url));
        r.put("id", usuario);
        return r;
    }

    @Transactional
    public Map<String, Object> reenviarInvitacion(int admin, int usuario, String ip) {
        var estado = jdbc.queryForList("SELECT u.estado::text FROM public.usuario u JOIN public.trabajador t ON t.usuario_id=u.id WHERE u.id=?", String.class, usuario);
        if (estado.isEmpty()) throw noEncontrado("Trabajador");
        if (!"pendiente".equals(estado.get(0))) throw conflicto("La cuenta ya fue activada");
        String url = enlaces.emitir(usuario, "activar_trabajador", admin, ServicioEnlaces.VIGENCIA_ACTIVAR);
        bitacora.registrar(admin, "reenviar_invitacion", "usuario", String.valueOf(usuario), null, ip);
        return enlaces.respuesta("Se generó una nueva invitación; la anterior quedó sin efecto.", url);
    }

    /** Cierra la adscripción vigente e inicia otra; las órdenes previas se revisan aparte. */
    @Transactional
    public Map<String, Object> cambiarZona(int admin, int usuario, Map<String, Object> d, String ip) {
        int zona = Datos.entero(d, "zonaId");
        zonaActiva(zona);
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.trabajador WHERE usuario_id=?)", Boolean.class, usuario)))
            throw noEncontrado("Trabajador");
        var actual = jdbc.queryForList("SELECT zona_id FROM public.trabajador_zona WHERE trabajador_id=? AND fecha_fin IS NULL FOR UPDATE", Integer.class, usuario);
        if (!actual.isEmpty() && actual.get(0) == zona) throw conflicto("El trabajador ya está en esa zona");
        jdbc.update("UPDATE public.trabajador_zona SET fecha_fin=CURRENT_TIMESTAMP WHERE trabajador_id=? AND fecha_fin IS NULL AND fecha_inicio<CURRENT_TIMESTAMP", usuario);
        jdbc.update("INSERT INTO public.trabajador_zona(trabajador_id,zona_id,asignado_por,fecha_inicio) VALUES(?,?,?,CURRENT_TIMESTAMP)", usuario, zona, admin);
        Integer pendientes = jdbc.queryForObject("""
                SELECT COUNT(*) FROM public.asignacion_trabajo a JOIN public.orden_trabajo o ON o.id=a.orden_id
                WHERE a.trabajador_id=? AND a.finalizado_en IS NULL AND o.finalizado_en IS NULL""", Integer.class, usuario);
        bitacora.registrar(admin, "cambio_zona", "usuario", String.valueOf(usuario), "Nueva zona " + zona, ip);
        return Map.of("mensaje", pendientes != null && pendientes > 0
                ? "Zona actualizada. Conserva " + pendientes + " orden(es) activa(s) de su zona anterior: revísalas o reasígnalas."
                : "Zona actualizada.", "ordenesPendientes", pendientes == null ? 0 : pendientes);
    }

    @Transactional
    public Map<String, Object> estadoTrabajador(int admin, int usuario, Map<String, Object> d, String ip) {
        String estado = Datos.opcion(d, "estado", List.of("activo", "suspendido"));
        if (usuario == admin) throw conflicto("No puedes cambiar el estado de tu propia cuenta");
        var actual = jdbc.queryForList("SELECT u.estado::text AS estado,u.password_hash IS NOT NULL AS clave FROM public.usuario u " +
                "JOIN public.trabajador t ON t.usuario_id=u.id WHERE u.id=? FOR UPDATE", usuario);
        if (actual.isEmpty()) throw noEncontrado("Trabajador");
        if (estado.equals("activo") && !Boolean.TRUE.equals(actual.get(0).get("clave")))
            throw conflicto("El trabajador aún no activa su cuenta; reenvía la invitación");
        if (estado.equals("suspendido")) {
            Integer responsable = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM public.asignacion_trabajo a JOIN public.orden_trabajo o ON o.id=a.orden_id
                    WHERE a.trabajador_id=? AND a.responsable_principal AND a.finalizado_en IS NULL AND o.finalizado_en IS NULL""", Integer.class, usuario);
            if (responsable != null && responsable > 0)
                throw conflicto("Es responsable de " + responsable + " orden(es) activa(s). Reasígnalas antes de suspenderlo.");
            jdbc.update("UPDATE public.asignacion_trabajo a SET finalizado_en=CURRENT_TIMESTAMP,motivo_fin='Trabajador suspendido' " +
                    "WHERE a.trabajador_id=? AND a.finalizado_en IS NULL", usuario);
            jdbc.update("UPDATE public.sesion_usuario SET revocada_en=CURRENT_TIMESTAMP WHERE usuario_id=? AND revocada_en IS NULL", usuario);
        }
        jdbc.update("UPDATE public.usuario SET estado=?::public.estado_usuario WHERE id=?", estado, usuario);
        bitacora.registrar(admin, estado.equals("activo") ? "reactivar_trabajador" : "suspender_trabajador", "usuario", String.valueOf(usuario), null, ip);
        return Map.of("id", usuario, "estado", estado);
    }

    // ------------------------------------------------------------------ Zonas y cobertura

    public List<Map<String, Object>> zonas() {
        return Datos.jsonEnLista(jdbc.queryForList("""
                SELECT z.id,z.codigo,z.nombre,z.descripcion,z.activa,vz.trabajadores_disponibles,
                  COALESCE((SELECT json_agg(json_build_object('id',c.id,'nombre',c.nombre) ORDER BY c.nombre)
                    FROM public.cobertura_zona cz JOIN public.colonia c ON c.id=cz.colonia_id
                    WHERE cz.zona_id=z.id AND tstzrange(cz.fecha_inicio,cz.fecha_fin,'[)') @> CURRENT_TIMESTAMP)::text,'[]') AS colonias
                FROM public.zona z JOIN public.v_zonas vz ON vz.id=z.id ORDER BY z.activa DESC,z.codigo"""), "colonias");
    }

    @Transactional
    public Map<String, Object> crearZona(int admin, Map<String, Object> d, String ip) {
        String codigo = Datos.texto(d, "codigo", 1, 20).toUpperCase();
        if (!codigo.matches("[A-Z0-9-]+")) throw Datos.invalido("El código solo admite letras, números y guiones");
        var z = jdbc.queryForMap("INSERT INTO public.zona(codigo,nombre,descripcion) VALUES(?,?,?) RETURNING id,codigo,nombre",
                codigo, Datos.texto(d, "nombre", 2, 100), Datos.textoOpcional(d, "descripcion", 500));
        bitacora.registrar(admin, "crear_zona", "zona", codigo, null, ip);
        return z;
    }

    @Transactional
    public Map<String, Object> actualizarZona(int admin, int id, Map<String, Object> d, String ip) {
        boolean activa = !Boolean.FALSE.equals(d.get("activa"));
        if (!activa) {
            Integer pendientes = jdbc.queryForObject("SELECT COUNT(*) FROM public.orden_trabajo WHERE zona_responsable_id=? AND finalizado_en IS NULL", Integer.class, id);
            if (pendientes != null && pendientes > 0)
                throw conflicto("La zona tiene " + pendientes + " orden(es) activa(s). Termínalas o transfiérelas antes de desactivarla.");
        }
        var filas = jdbc.queryForList("UPDATE public.zona SET nombre=?,descripcion=?,activa=? WHERE id=? RETURNING id,codigo,nombre,activa",
                Datos.texto(d, "nombre", 2, 100), Datos.textoOpcional(d, "descripcion", 500), activa, id);
        if (filas.isEmpty()) throw noEncontrado("Zona");
        bitacora.registrar(admin, "actualizar_zona", "zona", String.valueOf(filas.get(0).get("codigo")), activa ? "activa" : "inactiva", ip);
        return filas.get(0);
    }

    public List<Map<String, Object>> colonias(String q) {
        String patron = "%" + (q == null ? "" : q.trim().replace("%", "\\%").replace("_", "\\_")) + "%";
        return jdbc.queryForList("""
                SELECT c.id,c.nombre,m.nombre AS municipio,z.id AS zona_id,z.codigo AS zona
                FROM public.colonia c JOIN public.municipios m ON m.id=c.municipio_id
                LEFT JOIN public.cobertura_zona cz ON cz.colonia_id=c.id AND tstzrange(cz.fecha_inicio,cz.fecha_fin,'[)') @> CURRENT_TIMESTAMP
                LEFT JOIN public.zona z ON z.id=cz.zona_id
                WHERE c.nombre ILIKE ? ORDER BY (z.id IS NULL) DESC,c.nombre LIMIT 60""", patron);
    }

    /** Una colonia pertenece a una sola zona activa a la vez (la base impide solapamientos). */
    @Transactional
    public Map<String, Object> asignarCobertura(int admin, int colonia, Map<String, Object> d, String ip) {
        Integer zona = Datos.enteroOpcional(d, "zonaId");
        if (zona != null) zonaActiva(zona);
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.colonia WHERE id=?)", Boolean.class, colonia)))
            throw noEncontrado("Colonia");
        var actual = jdbc.queryForList("SELECT zona_id FROM public.cobertura_zona WHERE colonia_id=? AND fecha_fin IS NULL FOR UPDATE", Integer.class, colonia);
        if (!actual.isEmpty() && Objects.equals(actual.get(0), zona)) return Map.of("coloniaId", colonia, "zonaId", zona);
        jdbc.update("UPDATE public.cobertura_zona SET fecha_fin=CURRENT_TIMESTAMP WHERE colonia_id=? AND fecha_fin IS NULL AND fecha_inicio<CURRENT_TIMESTAMP", colonia);
        if (zona != null)
            jdbc.update("INSERT INTO public.cobertura_zona(zona_id,colonia_id,fecha_inicio) VALUES(?,?,CURRENT_TIMESTAMP)", zona, colonia);
        bitacora.registrar(admin, "cobertura_colonia", "colonia", String.valueOf(colonia), zona == null ? "sin zona" : "zona " + zona, ip);
        var r = new LinkedHashMap<String, Object>();
        r.put("coloniaId", colonia);
        r.put("zonaId", zona);
        return r;
    }

    // ------------------------------------------------------------------ Bitácora

    public List<Map<String, Object>> bitacora(int limite) {
        limite = Math.max(10, Math.min(limite, 500));
        return jdbc.queryForList("""
                SELECT * FROM (
                  SELECT b.creado_en AS fecha,'operación' AS origen,b.accion,b.entidad,b.referencia,b.detalle,p.nombre_completo AS actor,b.ip
                  FROM public.bitacora_operacion b LEFT JOIN public.usuario u ON u.id=b.actor_id LEFT JOIN public.persona p ON p.id=u.persona_id
                  UNION ALL
                  SELECT h.fecha,'reporte','reporte_'||h.estado::text,'reporte',r.folio,h.observaciones,p.nombre_completo,NULL
                  FROM public.historial_reporte h JOIN public.reporte r ON r.id=h.reporte_id
                  JOIN public.usuario u ON u.id=h.realizado_por JOIN public.persona p ON p.id=u.persona_id
                  UNION ALL
                  SELECT h.fecha,'orden','orden_'||h.estado::text,'orden',o.folio,h.observaciones,p.nombre_completo,NULL
                  FROM public.historial_orden h JOIN public.orden_trabajo o ON o.id=h.orden_id
                  JOIN public.usuario u ON u.id=h.realizado_por JOIN public.persona p ON p.id=u.persona_id
                  UNION ALL
                  SELECT e.creado_en,'privacidad',e.evento,'usuario',e.usuario_id::text,NULL,p.nombre_completo,NULL
                  FROM public.evento_privacidad e JOIN public.usuario u ON u.id=e.actor_id JOIN public.persona p ON p.id=u.persona_id
                ) t ORDER BY fecha DESC LIMIT ?""", limite);
    }
}
