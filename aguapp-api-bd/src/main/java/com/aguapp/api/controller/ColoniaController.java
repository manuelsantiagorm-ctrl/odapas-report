package com.aguapp.api.controller;

import com.aguapp.api.model.Colonia;
import com.aguapp.api.repository.ColoniaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller 1: Colonias.
 * Todas las rutas de esta clase empiezan con /api/colonias
 */
@RestController
@RequestMapping("/api/colonias")
public class ColoniaController {

    private final ColoniaRepository repository;

    public ColoniaController(ColoniaRepository repository) { this.repository = repository; }

    /**
     * GET http://localhost:8080/api/colonias
     * Devuelve la lista completa de colonias.
     */
    @GetMapping
    public List<Colonia> listarColonias() {
        return repository.listar();
    }

    /**
     * GET http://localhost:8080/api/colonias/c_centro
     * Devuelve una sola colonia por su id, o 404 si no existe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Colonia> obtenerColonia(@PathVariable String id) {
        return repository.obtener(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
