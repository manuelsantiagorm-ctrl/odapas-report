package mx.odapas.reporte.controlador;

import mx.odapas.reporte.repositorio.RepositorioCatalogos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/colonias")
public class ControladorColonias extends CrudCatalogo {
    public ControladorColonias(RepositorioCatalogos repositorio) { super(repositorio, "colonia"); }
}

