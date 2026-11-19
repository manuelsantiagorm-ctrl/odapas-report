package mx.odapas.reporte.controlador;
import mx.odapas.reporte.privacidad.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
@RestController
@RequestMapping("/api/cuenta")
public class ControladorCuenta {
 private final ServicioCuenta servicio;
 public ControladorCuenta(ServicioCuenta servicio){this.servicio=servicio;}
 private int id(HttpServletRequest r){return ((ServicioCuenta.Identidad)r.getAttribute("identidad")).id();}
 @GetMapping public Map<String,Object> perfil(HttpServletRequest r){return servicio.perfil(id(r));}
 @PutMapping public Map<String,Object> actualizar(HttpServletRequest r,@RequestBody Map<String,Object>d){return servicio.actualizar(id(r),d);}
 @PutMapping("/password") public Map<String,String> password(HttpServletRequest r,@RequestBody Map<String,Object>d){
  servicio.password(id(r),d);return Map.of("mensaje","Contraseña actualizada. Vuelve a iniciar sesión.");
 }
 @GetMapping("/exportar") public ResponseEntity<Map<String,Object>> exportar(HttpServletRequest r){
  return ResponseEntity.ok().header("Content-Disposition","attachment; filename=mis-datos-odapas.json").body(servicio.exportar(id(r)));
 }
 @GetMapping("/arco") public List<Map<String,Object>> solicitudes(HttpServletRequest r){return servicio.solicitudes(id(r));}
 @PostMapping("/arco") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> solicitud(HttpServletRequest r,@RequestBody Map<String,Object>d){return servicio.solicitud(id(r),d);}
 @PutMapping("/preferencias") public Map<String,String> preferencias(HttpServletRequest r,@RequestBody Map<String,Object>d){
  servicio.preferencias(id(r),d);return Map.of("mensaje","Preferencias actualizadas");
 }
 @PostMapping("/baja") public Map<String,Object> baja(HttpServletRequest r,@RequestBody Map<String,Object>d){return servicio.baja(id(r),d);}
 @PostMapping("/aceptaciones") public Map<String,String> aceptar(HttpServletRequest r,@RequestBody Map<String,Object>d){
  servicio.aceptar(id(r),d);return Map.of("mensaje","Versiones aceptadas");
 }
 @GetMapping("/aceptaciones") public Map<String,Object> aceptaciones(HttpServletRequest r){return servicio.estadoLegal(id(r));}
}

