package mx.odapas.reporte.controlador;

import jakarta.servlet.http.HttpServletRequest;
import mx.odapas.reporte.seguridad.Peticion;
import mx.odapas.reporte.servicio.ServicioAdministracion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Operación del administrador. [requiere sesión de administrador]
 *
 * Panel        GET  /api/admin/estadisticas
 * Reportes     GET  /api/admin/reportes?estado=&tipo=&zona=&q=&pagina=&tamano=
 *              PUT  /api/admin/reportes/{folio}/revision      {estado, observaciones}
 *              POST /api/admin/reportes/{folio}/clasificar
 *              GET  /api/admin/reportes/{folio}/elegibles     personal de la zona para la orden
 * Órdenes      GET  /api/admin/ordenes?estado=&zona=
 *              POST /api/admin/ordenes                        {reporteFolio, prioridad, fechaProgramada, responsableId, colaboradores[]}
 *              PUT  /api/admin/ordenes/{folio}/estado         {accion: cerrar|devolver|cancelar, observaciones}
 *              PUT  /api/admin/ordenes/{folio}/responsable    {responsableId, motivo}
 * Personal     GET  /api/admin/trabajadores
 *              POST /api/admin/trabajadores                   {nombre, email, telefono, numeroEmpleado, zonaId}
 *              POST /api/admin/trabajadores/{id}/invitacion
 *              PUT  /api/admin/trabajadores/{id}/zona         {zonaId}
 *              PUT  /api/admin/trabajadores/{id}/estado       {estado: activo|suspendido}
 * Zonas        GET  /api/admin/zonas   POST /api/admin/zonas   PUT /api/admin/zonas/{id}
 *              GET  /api/admin/colonias?q=   PUT /api/admin/colonias/{id}/zona {zonaId|null}
 * Bitácora     GET  /api/admin/bitacora?limite=
 */
@RestController
@RequestMapping("/api/admin")
public class ControladorAdministracion {
    private final ServicioAdministracion servicio;

    public ControladorAdministracion(ServicioAdministracion servicio) { this.servicio = servicio; }

    private int admin(HttpServletRequest r) { return Peticion.admin(r).id(); }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas(HttpServletRequest r) { admin(r); return servicio.estadisticas(); }

    @GetMapping("/reportes")
    public Map<String, Object> reportes(@RequestParam(required = false) String estado, @RequestParam(required = false) String tipo,
                                        @RequestParam(required = false) String zona, @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int tamano,
                                        HttpServletRequest r) {
        admin(r);
        return servicio.reportes(estado, tipo, zona, q, pagina, tamano);
    }

    @PutMapping("/reportes/{folio}/revision")
    public Map<String, Object> revisar(@PathVariable String folio, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.revisar(admin(r), folio, d, Peticion.ip(r));
    }

    @PostMapping("/reportes/{folio}/clasificar")
    public Map<String, Object> clasificar(@PathVariable String folio, HttpServletRequest r) {
        return servicio.clasificar(admin(r), folio, Peticion.ip(r));
    }

    @GetMapping("/reportes/{folio}/elegibles")
    public Map<String, Object> elegibles(@PathVariable String folio, HttpServletRequest r) {
        admin(r);
        return servicio.elegibles(folio);
    }

    @GetMapping("/ordenes")
    public List<Map<String, Object>> ordenes(@RequestParam(required = false) String estado, @RequestParam(required = false) String zona,
                                             HttpServletRequest r) {
        admin(r);
        return servicio.ordenes(estado, zona);
    }

    @PostMapping("/ordenes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearOrden(@RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.crearOrden(admin(r), d, Peticion.ip(r));
    }

    @PutMapping("/ordenes/{folio}/estado")
    public Map<String, Object> estadoOrden(@PathVariable String folio, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.estadoOrden(admin(r), folio, d, Peticion.ip(r));
    }

    @PutMapping("/ordenes/{folio}/responsable")
    public Map<String, Object> reasignar(@PathVariable String folio, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.reasignar(admin(r), folio, d, Peticion.ip(r));
    }

    @GetMapping("/trabajadores")
    public List<Map<String, Object>> trabajadores(HttpServletRequest r) { admin(r); return servicio.trabajadores(); }

    @PostMapping("/trabajadores")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> alta(@RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.altaTrabajador(admin(r), d, Peticion.ip(r));
    }

    @PostMapping("/trabajadores/{id}/invitacion")
    public Map<String, Object> invitacion(@PathVariable int id, HttpServletRequest r) {
        return servicio.reenviarInvitacion(admin(r), id, Peticion.ip(r));
    }

    @PutMapping("/trabajadores/{id}/zona")
    public Map<String, Object> zona(@PathVariable int id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.cambiarZona(admin(r), id, d, Peticion.ip(r));
    }

    @PutMapping("/trabajadores/{id}/estado")
    public Map<String, Object> estado(@PathVariable int id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.estadoTrabajador(admin(r), id, d, Peticion.ip(r));
    }

    @GetMapping("/zonas")
    public List<Map<String, Object>> zonas(HttpServletRequest r) { admin(r); return servicio.zonas(); }

    @PostMapping("/zonas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearZona(@RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.crearZona(admin(r), d, Peticion.ip(r));
    }

    @PutMapping("/zonas/{id}")
    public Map<String, Object> actualizarZona(@PathVariable int id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.actualizarZona(admin(r), id, d, Peticion.ip(r));
    }

    @GetMapping("/colonias")
    public List<Map<String, Object>> colonias(@RequestParam(required = false) String q, HttpServletRequest r) {
        admin(r);
        return servicio.colonias(q);
    }

    @PutMapping("/colonias/{id}/zona")
    public Map<String, Object> cobertura(@PathVariable int id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
        return servicio.asignarCobertura(admin(r), id, d, Peticion.ip(r));
    }

    @GetMapping("/bitacora")
    public List<Map<String, Object>> bitacora(@RequestParam(defaultValue = "150") int limite, HttpServletRequest r) {
        admin(r);
        return servicio.bitacora(limite);
    }
}
