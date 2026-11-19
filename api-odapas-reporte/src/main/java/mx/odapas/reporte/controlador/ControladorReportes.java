package mx.odapas.reporte.controlador;

import jakarta.servlet.http.HttpServletRequest;
import mx.odapas.reporte.modelo.Reporte;
import mx.odapas.reporte.repositorio.RepositorioReportes;
import mx.odapas.reporte.seguridad.Peticion;
import mx.odapas.reporte.servicio.ServicioReportes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Reportes ciudadanos de fugas, falta de agua y baja presión. [requiere sesión]
 *   GET  /api/reportes                    Mis reportes (administrador: todos). Filtros ?tipo= &zona=
 *   GET  /api/reportes/{folio}            Resumen de un reporte propio (administrador: cualquiera)
 *   POST /api/reportes                    Crear reporte (ciudadano con correo verificado)
 *   GET  /api/reportes/{folio}/detalle    Detalle con historial, fotos y órdenes
 *   POST /api/reportes/{folio}/fotos      Adjuntar hasta 3 fotos (multipart, campo "fotos")
 */
@RestController
@RequestMapping("/api/reportes")
public class ControladorReportes {
    private final RepositorioReportes repositorio;
    private final ServicioReportes servicio;

    public ControladorReportes(RepositorioReportes repositorio, ServicioReportes servicio) {
        this.repositorio = repositorio;
        this.servicio = servicio;
    }

    @GetMapping
    public List<Reporte> listarReportes(@RequestParam(required = false) String tipo,
                                        @RequestParam(required = false) String zona,
                                        HttpServletRequest request) {
        var identidad = Peticion.identidad(request);
        return identidad.admin() ? repositorio.listar(tipo, zona) : repositorio.listarPropios(tipo, zona, identidad.id());
    }

    @GetMapping("/{folio}")
    public ResponseEntity<Reporte> obtenerReporte(@PathVariable String folio, HttpServletRequest request) {
        var identidad = Peticion.identidad(request);
        return (identidad.admin() ? repositorio.obtener(folio) : repositorio.obtenerPropio(folio, identidad.id()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@RequestBody Map<String, Object> datos, HttpServletRequest request) {
        return servicio.crear(Peticion.identidad(request), datos);
    }

    @GetMapping("/{folio}/detalle")
    public Map<String, Object> detalle(@PathVariable String folio, HttpServletRequest request) {
        return servicio.detalle(Peticion.identidad(request), folio);
    }

    @PostMapping(value = "/{folio}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> fotos(@PathVariable String folio, @RequestParam("fotos") List<MultipartFile> fotos,
                                     HttpServletRequest request) {
        return servicio.agregarFotos(Peticion.identidad(request), folio, fotos);
    }
}
