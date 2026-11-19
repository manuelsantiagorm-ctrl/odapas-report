package mx.odapas.reporte.controlador;

import mx.odapas.reporte.repositorio.RepositorioCatalogos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tipos-reporte")
public class ControladorTiposReporte extends CrudCatalogo {
    public ControladorTiposReporte(RepositorioCatalogos repositorio) { super(repositorio, "tipo_reporte"); }
}

