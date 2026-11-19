package mx.odapas.reporte.controlador;

import mx.odapas.reporte.modelo.Zona;
import mx.odapas.reporte.repositorio.RepositorioZonas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador 1: Zonas de atencion.
 * Todas las rutas de esta clase empiezan con /api/zonas
 *
 * Consulta zonas, colonias y personal de la base PostgreSQL.
 */
@RestController
@RequestMapping("/api/zonas")
public class ControladorZonas {

    private final RepositorioZonas repositorio;

    public ControladorZonas(RepositorioZonas repositorio) { this.repositorio = repositorio; }
    /**
     * GET http://localhost:8080/api/zonas
     * Devuelve todas las zonas de atencion con las colonias que cubren.
     */
    @GetMapping
    public List<Zona> listarZonas() {
        return repositorio.listar();
    }

    /**
     * GET http://localhost:8080/api/zonas/ZN
     * Devuelve una zona por su codigo. Si no existe responde 404 (no encontrado).
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<Zona> obtenerZona(@PathVariable String codigo) {
        return repositorio.obtener(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
