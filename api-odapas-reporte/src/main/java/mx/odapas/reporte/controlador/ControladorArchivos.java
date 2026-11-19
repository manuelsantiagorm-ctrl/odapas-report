package mx.odapas.reporte.controlador;

import jakarta.servlet.http.HttpServletRequest;
import mx.odapas.reporte.seguridad.Peticion;
import mx.odapas.reporte.servicio.ServicioArchivos;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** GET /api/archivos/{nombre} — descarga una foto si el usuario tiene permiso sobre ella. [requiere sesión] */
@RestController
@RequestMapping("/api/archivos")
public class ControladorArchivos {
    private final ServicioArchivos archivos;

    public ControladorArchivos(ServicioArchivos archivos) { this.archivos = archivos; }

    @GetMapping("/{nombre}")
    public ResponseEntity<Resource> leer(@PathVariable String nombre, HttpServletRequest r) {
        var ruta = archivos.leer(nombre, Peticion.identidad(r));
        var tipo = nombre.endsWith(".png") ? MediaType.IMAGE_PNG
                : nombre.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).cacheControl(CacheControl.noStore())
                .header("Content-Disposition", "inline").body(new FileSystemResource(ruta));
    }
}
