package mx.odapas.reporte.servicio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mx.odapas.reporte.privacidad.ServicioCuenta.Identidad;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Reportes ciudadanos: creación, fotografías, detalle con historial y mapa público.
 * El folio, la zona, el autor y el estado los decide el servidor; el cliente nunca los impone.
 */
@Service
public class ServicioReportes {
    public static final int MAX_FOTOS_REPORTE = 3;
    private final JdbcTemplate jdbc;
    private final ServicioArchivos archivos;
    private final ObjectMapper json;

    public ServicioReportes(JdbcTemplate jdbc, ServicioArchivos archivos, ObjectMapper json) {
        this.jdbc = jdbc;
        this.archivos = archivos;
        this.json = json;
    }

    private String aJson(Object o) {
        try { return json.writeValueAsString(o); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    /** Catálogo de tipos visibles al ciudadano (los de carga artificial no se muestran). */
    public List<Map<String, Object>> tipos() {
        return jdbc.queryForList("SELECT id,codigo,nombre FROM public.tipo_reporte WHERE codigo NOT LIKE 'carga\\_%' ORDER BY id");
    }

    @Transactional
    public Map<String, Object> crear(Identidad quien, Map<String, Object> d) {
        var estado = jdbc.queryForMap("""
                SELECT EXISTS(SELECT 1 FROM public.ciudadano WHERE usuario_id=?) AS ciudadano,
                       c.correo_verificado_en IS NOT NULL AS verificado
                FROM public.usuario u JOIN public.contacto c ON c.persona_id=u.persona_id WHERE u.id=?""", quien.id(), quien.id());
        if (!Boolean.TRUE.equals(estado.get("ciudadano")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo una cuenta ciudadana puede crear reportes");
        if (!Boolean.TRUE.equals(estado.get("verificado")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verifica tu correo antes de enviar reportes (Mi cuenta → Verificar correo)");

        String codigo = Datos.texto(d, "tipo", 1, 30);
        var tipo = jdbc.queryForList("SELECT id FROM public.tipo_reporte WHERE codigo=?", Integer.class, codigo);
        if (tipo.isEmpty()) throw Datos.invalido("Tipo de reporte inexistente");
        String descripcion = Datos.texto(d, "descripcion", 10, 2000);

        // Evita duplicados por doble clic o reintento: mismo autor y misma descripción en 2 minutos.
        var repetido = jdbc.queryForList("SELECT folio FROM public.reporte WHERE ciudadano_id=? AND descripcion=? " +
                "AND creado_en > CURRENT_TIMESTAMP - INTERVAL '2 minutes'", String.class, quien.id(), descripcion);
        if (!repetido.isEmpty()) return detalle(quien, repetido.get(0));

        var lat = Datos.coordenada(d, "latitud", 90);
        var lng = Datos.coordenada(d, "longitud", 180);
        if ((lat == null) != (lng == null)) throw Datos.invalido("Indica latitud y longitud juntas, o ninguna");
        String referencias = Datos.textoOpcional(d, "referencias", 500);

        int direccion;
        if (Boolean.TRUE.equals(d.get("usarDomicilio"))) {
            var dom = jdbc.queryForList("""
                    SELECT d.colonia_id,d.calle,d.numero_exterior,d.numero_interior,d.codigo_postal
                    FROM public.ciudadano c JOIN public.direccion d ON d.id=c.direccion_actual_id WHERE c.usuario_id=?""", quien.id());
            if (dom.isEmpty()) throw Datos.invalido("No tienes domicilio registrado; captura la ubicación del incidente");
            var x = dom.get(0);
            // Copia histórica: si después cambias tu domicilio, el reporte conserva esta ubicación.
            direccion = jdbc.queryForObject("""
                    INSERT INTO public.direccion(colonia_id,calle,numero_exterior,numero_interior,codigo_postal,referencias,latitud,longitud)
                    VALUES(?,?,?,?,?,?,?,?) RETURNING id""", Integer.class,
                    x.get("colonia_id"), x.get("calle"), x.get("numero_exterior"), x.get("numero_interior"), x.get("codigo_postal"),
                    referencias, lat, lng);
        } else {
            int colonia = Datos.entero(d, "coloniaId");
            if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.colonia WHERE id=?)", Boolean.class, colonia)))
                throw Datos.invalido("Colonia inexistente");
            String cp = Datos.texto(d, "codigoPostal", 5, 5);
            if (!cp.matches("[0-9]{5}")) throw Datos.invalido("Código postal inválido");
            direccion = jdbc.queryForObject("""
                    INSERT INTO public.direccion(colonia_id,calle,numero_exterior,numero_interior,codigo_postal,referencias,latitud,longitud)
                    VALUES(?,?,?,?,?,?,?,?) RETURNING id""", Integer.class,
                    colonia, Datos.texto(d, "calle", 1, 150), Datos.texto(d, "numeroExterior", 1, 20),
                    Datos.textoOpcional(d, "numeroInterior", 20), cp, referencias, lat, lng);
        }
        String folio = jdbc.queryForObject("SELECT 'R-'||LPAD(nextval('public.folio_reporte_seq')::text,6,'0')", String.class);
        // El trigger reporte_creado registra el historial "recibido", clasifica la zona y genera los avisos.
        jdbc.update("INSERT INTO public.reporte(folio,ciudadano_id,tipo_reporte_id,direccion_incidente_id,descripcion) VALUES(?,?,?,?,?)",
                folio, quien.id(), tipo.get(0), direccion, descripcion);
        return detalle(quien, folio);
    }

    @Transactional
    public Map<String, Object> agregarFotos(Identidad quien, String folio, List<MultipartFile> fotos) {
        folio = Datos.folio(folio);
        var filas = jdbc.queryForList("SELECT id FROM public.reporte WHERE UPPER(folio)=? AND ciudadano_id=? FOR UPDATE", Integer.class, folio, quien.id());
        if (filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        int id = filas.get(0);
        Integer actuales = jdbc.queryForObject("SELECT COUNT(*) FROM public.fotografia_reporte WHERE reporte_id=?", Integer.class, id);
        if (fotos == null || fotos.isEmpty()) throw Datos.invalido("Adjunta al menos una fotografía");
        if (actuales + fotos.size() > MAX_FOTOS_REPORTE)
            throw Datos.invalido("Máximo " + MAX_FOTOS_REPORTE + " fotografías por reporte");
        for (var foto : fotos)
            jdbc.update("INSERT INTO public.fotografia_reporte(reporte_id,foto_json) VALUES(?,?::jsonb)", id, aJson(archivos.guardar(foto)));
        return detalle(quien, folio);
    }

    /** Puede verlo el autor, un administrador o un trabajador asignado a una orden del reporte. */
    public Map<String, Object> detalle(Identidad quien, String folio) {
        folio = Datos.folio(folio);
        var base = jdbc.queryForList("""
                SELECT r.id,r.folio,r.ciudadano_id,tr.codigo AS tipo,tr.nombre AS tipo_nombre,r.descripcion,r.creado_en,
                       c.nombre AS colonia,m.nombre AS municipio,d.calle,d.numero_exterior,d.numero_interior,d.codigo_postal,
                       d.referencias,d.latitud,d.longitud,v.estado,v.zona,v.prioridad
                FROM public.reporte r JOIN public.tipo_reporte tr ON tr.id=r.tipo_reporte_id
                JOIN public.direccion d ON d.id=r.direccion_incidente_id JOIN public.colonia c ON c.id=d.colonia_id
                JOIN public.municipios m ON m.id=c.municipio_id JOIN public.v_reportes v ON v.id=r.id
                WHERE UPPER(r.folio)=?""", folio);
        if (base.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        var r = new LinkedHashMap<>(base.get(0));
        int id = ((Number) r.get("id")).intValue();
        boolean autor = ((Number) r.get("ciudadano_id")).intValue() == quien.id();
        boolean asignado = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM public.orden_trabajo o JOIN public.asignacion_trabajo a ON a.orden_id=o.id
                WHERE o.reporte_id=? AND a.trabajador_id=?)""", Boolean.class, id, quien.id()));
        if (!autor && !quien.admin() && !asignado) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        r.remove("ciudadano_id");

        r.put("historial", jdbc.queryForList(quien.admin() ? """
                SELECT h.secuencia,h.estado::text AS estado,h.observaciones,h.fecha,p.nombre_completo AS actor
                FROM public.historial_reporte h JOIN public.usuario u ON u.id=h.realizado_por JOIN public.persona p ON p.id=u.persona_id
                WHERE h.reporte_id=? ORDER BY h.secuencia""" : """
                SELECT h.secuencia,h.estado::text AS estado,h.observaciones,h.fecha FROM public.historial_reporte h
                WHERE h.reporte_id=? ORDER BY h.secuencia""", id));
        r.put("fotos", jdbc.queryForList("SELECT f.id,f.foto_json->>'url' AS url,f.foto_json->>'nombre' AS nombre,f.creado_en " +
                "FROM public.fotografia_reporte f WHERE f.reporte_id=? ORDER BY f.id", id));

        var ordenes = jdbc.queryForList("""
                SELECT o.id,o.folio,o.prioridad::text AS prioridad,o.fecha_programada,o.creado_en,o.finalizado_en,z.codigo AS zona,
                  (SELECT h.estado::text FROM public.historial_orden h WHERE h.orden_id=o.id ORDER BY h.secuencia DESC LIMIT 1) AS estado
                FROM public.orden_trabajo o JOIN public.zona z ON z.id=o.zona_responsable_id
                WHERE o.reporte_id=? ORDER BY o.creado_en""", id);
        for (var o : ordenes) {
            int orden = ((Number) o.get("id")).intValue();
            o.put("historial", jdbc.queryForList("SELECT h.secuencia,h.estado::text AS estado,h.observaciones,h.fecha FROM public.historial_orden h " +
                    "WHERE h.orden_id=? ORDER BY h.secuencia", orden));
            o.put("equipo", jdbc.queryForList(quien.admin() || asignado ? """
                    SELECT a.trabajador_id AS id,p.nombre_completo AS nombre,t.numero_empleado,a.responsable_principal,a.recibido_en,a.finalizado_en
                    FROM public.asignacion_trabajo a JOIN public.trabajador t ON t.usuario_id=a.trabajador_id
                    JOIN public.usuario u ON u.id=a.trabajador_id JOIN public.persona p ON p.id=u.persona_id
                    WHERE a.orden_id=? ORDER BY a.responsable_principal DESC,a.id""" : """
                    SELECT a.responsable_principal,a.recibido_en,a.finalizado_en FROM public.asignacion_trabajo a
                    WHERE a.orden_id=? ORDER BY a.responsable_principal DESC,a.id""", orden));
            o.put("reparaciones", Datos.jsonEnLista(jdbc.queryForList("""
                    SELECT rp.id,rp.diagnostico,rp.trabajo_realizado,rp.resultado::text AS resultado,rp.iniciado_en,rp.terminado_en,
                      COALESCE((SELECT json_agg(json_build_object('url',f.foto_json->>'url','fase',f.fase::text) ORDER BY f.id)
                                FROM public.fotografia_reparacion f WHERE f.reparacion_id=rp.id)::text,'[]') AS fotos
                    FROM public.reparacion rp JOIN public.asignacion_trabajo a ON a.id=rp.asignacion_id
                    WHERE a.orden_id=? ORDER BY rp.id""", orden), "fotos"));
        }
        r.put("ordenes", ordenes);
        if (quien.admin() || asignado) {
            // Contacto del ciudadano solo para personal autorizado (necesario para atender en campo).
            r.put("ciudadano", jdbc.queryForMap("""
                    SELECT p.nombre_completo AS nombre,c.telefono FROM public.reporte r JOIN public.usuario u ON u.id=r.ciudadano_id
                    JOIN public.persona p ON p.id=u.persona_id JOIN public.contacto c ON c.persona_id=p.id WHERE r.id=?""", id));
        }
        return r;
    }

    /** Mapa público: sin folio, nombre, teléfono, correo ni domicilio del ciudadano. */
    public List<Map<String, Object>> mapaPublico() {
        return jdbc.queryForList("""
                SELECT tipo,colonia,estado,zona,latitud,longitud,fecha_creacion
                FROM public.v_reportes WHERE latitud IS NOT NULL
                  AND estado NOT IN ('rechazado','cancelado','cerrada','cancelada')
                ORDER BY fecha_creacion DESC LIMIT 300""");
    }

    public Map<String, Object> resumenPublico() {
        return jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE estado NOT IN ('rechazado','cancelado','cerrada','cancelada')) AS abiertos,
                       COUNT(*) FILTER (WHERE estado IN ('asignado','en_proceso','reparacion_realizada')) AS en_atencion,
                       COUNT(*) FILTER (WHERE estado='cerrada') AS resueltos,
                       COUNT(*) AS total FROM public.v_reportes""");
    }
}
