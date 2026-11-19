package com.aguapp.api.controller;

import com.aguapp.api.model.Reporte;
import com.aguapp.api.repository.ReporteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller 2: Reportes de fugas y falta de agua.
 * Todas las rutas de esta clase empiezan con /api/reportes
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteRepository repository;
    public ReporteController(ReporteRepository repository) { this.repository = repository; }

    /**
     * GET http://localhost:8080/api/reportes
     * GET http://localhost:8080/api/reportes?tipo=fuga   (filtro opcional)
     * Devuelve todos los reportes, o solo los del tipo indicado.
     */
    @GetMapping
    public List<Reporte> listarReportes(@RequestParam(required = false) String tipo) {
        return repository.listar(tipo);
    }

    /**
     * GET http://localhost:8080/api/reportes/r1
     * Devuelve un reporte por su id, o 404 si no existe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reporte> obtenerReporte(@PathVariable String id) {
        return repository.obtener(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
