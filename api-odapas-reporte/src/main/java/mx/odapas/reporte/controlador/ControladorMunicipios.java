package mx.odapas.reporte.controlador;

import mx.odapas.reporte.repositorio.RepositorioCatalogos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/municipios")
public class ControladorMunicipios extends CrudCatalogo {
    public ControladorMunicipios(RepositorioCatalogos repositorio) { super(repositorio, "municipios"); }
}

