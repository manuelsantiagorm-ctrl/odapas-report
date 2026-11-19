package mx.odapas.reporte.controlador;

import mx.odapas.reporte.repositorio.RepositorioCatalogos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contactos")
public class ControladorContactos extends CrudCatalogo {
    public ControladorContactos(RepositorioCatalogos repositorio) { super(repositorio, "contacto"); }
}

