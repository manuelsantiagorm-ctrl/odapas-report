package mx.odapas.reporte.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import mx.odapas.reporte.privacidad.ServicioCuenta.Identidad;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Utilidades para leer al usuario autenticado y el origen de la petición. */
public final class Peticion {
    private Peticion() {}

    /** Identidad colocada por SeguridadCuenta. Nunca proviene del cuerpo de la petición. */
    public static Identidad identidad(HttpServletRequest r) {
        Object i = r.getAttribute("identidad");
        if (!(i instanceof Identidad identidad))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión");
        return identidad;
    }

    public static Identidad admin(HttpServletRequest r) {
        var i = identidad(r);
        if (!i.admin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere administrador");
        return i;
    }

    public static Identidad trabajador(HttpServletRequest r) {
        var i = identidad(r);
        if (!i.trabajador()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere perfil de trabajador");
        return i;
    }

    /** IP del cliente. Solo se usa para limitar intentos y para la bitácora. */
    public static String ip(HttpServletRequest r) {
        String ip = r.getRemoteAddr();
        return ip == null ? "desconocida" : ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
