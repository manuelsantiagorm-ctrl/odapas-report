package mx.odapas.reporte.controlador;

import mx.odapas.reporte.privacidad.DocumentosLegales;
import mx.odapas.reporte.servicio.ServicioReportes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Información pública (sin sesión y sin datos personales).
 *   GET /api/publico/documentos     Términos y aviso de privacidad vigentes
 *   GET /api/publico/colonias?q=    Colonias para formularios (máx. 50 si se busca)
 *   GET /api/publico/tipos-reporte  Tipos de incidente
 *   GET /api/publico/mapa           Incidentes abiertos con coordenadas (sin folio ni datos del ciudadano)
 *   GET /api/publico/resumen        Contadores generales
 */
@RestController
@RequestMapping("/api/publico")
public class ControladorPublico {
    private final JdbcTemplate jdbc;
    private final ServicioReportes reportes;

    public ControladorPublico(JdbcTemplate jdbc, ServicioReportes reportes) {
        this.jdbc = jdbc;
        this.reportes = reportes;
    }

    @GetMapping("/documentos")
    public Map<String, Object> documentos() { return DocumentosLegales.documentos(); }

    @GetMapping("/colonias")
    public List<Map<String, Object>> colonias(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            String patron = "%" + q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            return jdbc.queryForList("SELECT c.id,c.nombre,m.nombre AS municipio FROM public.colonia c " +
                    "JOIN public.municipios m ON m.id=c.municipio_id WHERE c.nombre ILIKE ? ORDER BY c.nombre LIMIT 50", patron);
        }
        return jdbc.queryForList("SELECT c.id,c.nombre,m.nombre AS municipio FROM public.colonia c JOIN public.municipios m ON m.id=c.municipio_id ORDER BY c.nombre");
    }

    @GetMapping("/tipos-reporte")
    public List<Map<String, Object>> tipos() { return reportes.tipos(); }

    @GetMapping("/mapa")
    public List<Map<String, Object>> mapa() { return reportes.mapaPublico(); }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() { return reportes.resumenPublico(); }
}
