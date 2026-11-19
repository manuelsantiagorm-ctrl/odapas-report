package mx.odapas.reporte.controlador;

import mx.odapas.reporte.repositorio.RepositorioCatalogos;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

public abstract class CrudCatalogo {
    private final RepositorioCatalogos repositorio;
    private final String tabla;
    protected CrudCatalogo(RepositorioCatalogos repositorio, String tabla) {
        this.repositorio = repositorio; this.tabla = tabla;
    }
    @GetMapping
    public List<Map<String,Object>> listar() { return repositorio.listar(tabla); }
    @GetMapping("/{id}")
    public Map<String,Object> obtener(@PathVariable int id) { return repositorio.obtener(tabla, id); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> crear(@RequestBody Map<String,Object> datos) { return repositorio.crear(tabla, datos); }
    @PutMapping("/{id}")
    public Map<String,Object> actualizar(@PathVariable int id, @RequestBody Map<String,Object> datos) {
        return repositorio.actualizar(tabla, id, datos);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) { repositorio.eliminar(tabla, id); }
}

