package mx.odapas.reporte.controlador;
import mx.odapas.reporte.privacidad.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
@RestController
@RequestMapping("/api/admin/arco")
public class ControladorPrivacidadAdmin {
 private final ServicioCuenta servicio;
 public ControladorPrivacidadAdmin(ServicioCuenta servicio){this.servicio=servicio;}
 @GetMapping public List<Map<String,Object>> solicitudes(){return servicio.pendientes();}
 @PutMapping("/{id}") public Map<String,Object> resolver(@PathVariable int id,HttpServletRequest r,@RequestBody Map<String,Object>d){
  return servicio.resolver(((ServicioCuenta.Identidad)r.getAttribute("identidad")).id(),id,d);
 }
}

