package mx.odapas.reporte.controlador;

import jakarta.servlet.http.HttpServletRequest;
import mx.odapas.reporte.seguridad.Peticion;
import mx.odapas.reporte.servicio.Datos;
import mx.odapas.reporte.servicio.ServicioReportes;
import mx.odapas.reporte.servicio.ServicioTrabajador;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Panel del trabajador reparador. [requiere sesión con perfil de trabajador]
 *   GET  /api/trabajador/resumen                     Mi zona, conteos e incidentes de la zona
 *   GET  /api/trabajador/ordenes                     Mis órdenes (vigentes primero)
 *   GET  /api/trabajador/ordenes/{folio}             Detalle de una orden asignada (incluye contacto del ciudadano)
 *   POST /api/trabajador/ordenes/{folio}/recibir     Confirmar recepción
 *   POST /api/trabajador/ordenes/{folio}/iniciar     {diagnostico}
 *   POST /api/trabajador/ordenes/{folio}/fotos?fase=antes|despues   multipart "fotos"
 *   POST /api/trabajador/ordenes/{folio}/terminar    {trabajoRealizado, resultado, justificacionSinFoto?}
 */
@RestController
@RequestMapping("/api/trabajador")
public class ControladorTrabajador {
    private final ServicioTrabajador servicio;
    private final ServicioReportes reportes;
    private final JdbcTemplate jdbc;

    public ControladorTrabajador(ServicioTrabajador servicio, ServicioReportes reportes, JdbcTemplate jdbc) {
        this.servicio = servicio;
        this.reportes = reportes;
        this.jdbc = jdbc;
    }

    private int yo(HttpServletRequest r) { return Peticion.trabajador(r).id(); }

    @GetMapping("/resumen")
    public Map<String, Object> resumen(HttpServletRequest r) { return servicio.resumen(yo(r)); }

    @GetMapping("/ordenes")
    public List<Map<String, Object>> ordenes(HttpServletRequest r) { return servicio.ordenes(yo(r)); }

    @GetMapping("/ordenes/{folio}")
    public Map<String, Object> detalle(@PathVariable String folio, HttpServletRequest r) {
        int yo = yo(r);
        var reporte = jdbc.queryForList("""
                SELECT r.folio FROM public.orden_trabajo o JOIN public.reporte r ON r.id=o.reporte_id
                WHERE UPPER(o.folio)=? AND EXISTS(SELECT 1 FROM public.asignacion_trabajo a WHERE a.orden_id=o.id AND a.trabajador_id=?)""",
                String.class, Datos.folio(folio), yo);
        if (reporte.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada");
        return reportes.detalle(Peticion.identidad(r), reporte.get(0));
    }

    @PostMapping("/ordenes/{folio}/recibir")
    public Map<String, Object> recibir(@PathVariable String folio, HttpServletRequest r) { return servicio.recibir(yo(r), folio); }

    @PostMapping("/ordenes/{folio}/iniciar")
    public Map<String, Object> iniciar(@PathVariable String folio, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.iniciar(yo(r), folio, d);
    }

    @PostMapping(value = "/ordenes/{folio}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> fotos(@PathVariable String folio, @RequestParam String fase,
                                     @RequestParam("fotos") List<MultipartFile> fotos, HttpServletRequest r) {
        return servicio.fotos(yo(r), folio, fase, fotos);
    }

    @PostMapping("/ordenes/{folio}/terminar")
    public Map<String, Object> terminar(@PathVariable String folio, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.terminar(yo(r), folio, d);
    }
}
