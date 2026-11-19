package com.aguapp.api.controller;

import com.aguapp.api.model.Reparador;
import com.aguapp.api.repository.ReparadorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reparadores")
public class ReparadorController {
    private final ReparadorRepository repository;
    public ReparadorController(ReparadorRepository repository) { this.repository = repository; }
    @GetMapping public List<Reparador> listar() { return repository.listar(); }
    @GetMapping("/{id}") public ResponseEntity<Reparador> obtener(@PathVariable String id) {
        return repository.obtener(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
