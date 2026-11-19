package mx.odapas.reporte.controlador;

import jakarta.servlet.http.HttpServletRequest;
import mx.odapas.reporte.seguridad.Peticion;
import mx.odapas.reporte.servicio.ServicioNotificaciones;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Bandeja de avisos del usuario autenticado. [requiere sesión]
 *   GET /api/notificaciones?soloNoLeidas=true
 *   GET /api/notificaciones/contador
 *   PUT /api/notificaciones/{id}/leida
 *   PUT /api/notificaciones/leer-todas
 */
@RestController
@RequestMapping("/api/notificaciones")
public class ControladorNotificaciones {
    private final ServicioNotificaciones servicio;

    public ControladorNotificaciones(ServicioNotificaciones servicio) { this.servicio = servicio; }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(defaultValue = "false") boolean soloNoLeidas, HttpServletRequest r) {
        return servicio.listar(Peticion.identidad(r).id(), soloNoLeidas);
    }

    @GetMapping("/contador")
    public Map<String, Integer> contador(HttpServletRequest r) {
        return Map.of("noLeidas", servicio.noLeidas(Peticion.identidad(r).id()));
    }

    @PutMapping("/{id}/leida")
    public Map<String, String> leida(@PathVariable int id, HttpServletRequest r) {
        servicio.marcar(Peticion.identidad(r).id(), id);
        return Map.of("mensaje", "Aviso marcado como leído");
    }

    @PutMapping("/leer-todas")
    public Map<String, Integer> todas(HttpServletRequest r) {
        return Map.of("marcadas", servicio.marcarTodas(Peticion.identidad(r).id()));
    }
}
