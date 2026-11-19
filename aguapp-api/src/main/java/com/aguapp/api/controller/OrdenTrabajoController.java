package com.aguapp.api.controller;

import com.aguapp.api.model.OrdenTrabajo;
import com.aguapp.api.repository.OrdenTrabajoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ordenes-trabajo")
public class OrdenTrabajoController {
    private final OrdenTrabajoRepository repository;
    public OrdenTrabajoController(OrdenTrabajoRepository repository) { this.repository = repository; }
    @GetMapping public List<OrdenTrabajo> listar(@RequestParam(required = false) String estado,
                                                @RequestParam(required = false) String reparadorId) {
        return repository.listar(estado, reparadorId);
    }
    @GetMapping("/{id}") public ResponseEntity<OrdenTrabajo> obtener(@PathVariable String id) {
        return repository.obtener(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
