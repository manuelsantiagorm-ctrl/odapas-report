package mx.odapas.reporte.controlador;

import jakarta.servlet.http.HttpServletRequest;
import mx.odapas.reporte.privacidad.ServicioCuenta;
import mx.odapas.reporte.seguridad.Peticion;
import mx.odapas.reporte.servicio.ServicioAcceso;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Acceso a la aplicación.
 *   POST /api/auth/registro                 Registro público de ciudadanos (genera enlace de verificación)
 *   POST /api/auth/login                    Inicio de sesión (bloqueo tras 5 intentos fallidos)
 *   POST /api/auth/logout                   Cierra la sesión actual            [sesión]
 *   POST /api/auth/verificar                Verifica el correo con el token del enlace
 *   POST /api/auth/reenviar-verificacion    Genera un nuevo enlace de verificación [sesión]
 *   POST /api/auth/recuperar                Solicita enlace para restablecer contraseña
 *   POST /api/auth/restablecer              Define nueva contraseña con el token
 *   POST /api/auth/activar                  Activa la cuenta de un trabajador invitado
 */
@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {
    private final ServicioCuenta cuentas;
    private final ServicioAcceso acceso;

    public ControladorAutenticacion(ServicioCuenta cuentas, ServicioAcceso acceso) {
        this.cuentas = cuentas;
        this.acceso = acceso;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registro(@RequestBody Map<String, Object> d, HttpServletRequest r) {
        return acceso.registrar(d, Peticion.ip(r));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> d, HttpServletRequest r) {
        return acceso.login(d, Peticion.ip(r));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest r) {
        cuentas.logout(Peticion.identidad(r).id(), r.getHeader("Authorization"));
        return Map.of("mensaje", "Sesión cerrada");
    }

    @PostMapping("/verificar")
    public Map<String, Object> verificar(@RequestBody Map<String, Object> d) {
        return acceso.verificarCorreo(d.get("token") instanceof String t ? t : null);
    }

    @PostMapping("/reenviar-verificacion")
    public Map<String, Object> reenviar(HttpServletRequest r) {
        return acceso.reenviarVerificacion(Peticion.identidad(r).id(), Peticion.ip(r));
    }

    @PostMapping("/recuperar")
    public Map<String, Object> recuperar(@RequestBody Map<String, Object> d, HttpServletRequest r) {
        return acceso.recuperar(d, Peticion.ip(r));
    }

    @PostMapping("/restablecer")
    public Map<String, Object> restablecer(@RequestBody Map<String, Object> d) {
        return acceso.restablecer(d);
    }

    @PostMapping("/activar")
    public Map<String, Object> activar(@RequestBody Map<String, Object> d, HttpServletRequest r) {
        return acceso.activar(d, Peticion.ip(r));
    }
}
