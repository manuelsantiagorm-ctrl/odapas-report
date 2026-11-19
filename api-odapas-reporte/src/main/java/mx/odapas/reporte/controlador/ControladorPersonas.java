package mx.odapas.reporte.controlador;

import mx.odapas.reporte.repositorio.RepositorioCatalogos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personas")
public class ControladorPersonas extends CrudCatalogo {
    public ControladorPersonas(RepositorioCatalogos repositorio) { super(repositorio, "persona"); }
}

