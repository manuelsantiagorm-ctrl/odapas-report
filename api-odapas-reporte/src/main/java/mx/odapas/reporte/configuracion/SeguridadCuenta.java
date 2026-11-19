package mx.odapas.reporte.configuracion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.odapas.reporte.privacidad.ServicioCuenta;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autorización en el servidor para CADA petición (no basta con ocultar botones).
 *
 * Públicas: /api/publico/**, /api/zonas (lectura), login, registro, verificar correo,
 *           recuperar/restablecer contraseña y activar cuenta de trabajador.
 * Cualquier sesión: /api/cuenta/**, /api/reportes/**, /api/notificaciones/**, /api/archivos/**.
 * Solo trabajador: /api/trabajador/**.
 * Solo administrador: /api/admin/** y los catálogos CRUD (personas, contactos, municipios,
 *           colonias, tipos-reporte).
 */
@Configuration
public class SeguridadCuenta implements WebMvcConfigurer {
    private final ServicioCuenta servicio;

    public SeguridadCuenta(ServicioCuenta servicio) { this.servicio = servicio; }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                if ("OPTIONS".equals(req.getMethod())) return true;
                var identidad = servicio.autenticar(req.getHeader("Authorization"));
                req.setAttribute("identidad", identidad);
                String ruta = req.getRequestURI();

                if (ruta.startsWith("/api/trabajador")) {
                    if (!identidad.trabajador()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere perfil de trabajador");
                    return true;
                }
                boolean personal = ruta.startsWith("/api/cuenta") || ruta.startsWith("/api/auth/")
                        || ruta.startsWith("/api/reportes") || ruta.startsWith("/api/notificaciones")
                        || ruta.startsWith("/api/archivos");
                if (!personal && !identidad.admin())
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere administrador");

                // El ciudadano debe tener aceptadas las versiones vigentes antes de reportar.
                if (ruta.startsWith("/api/reportes") && !identidad.admin()
                        && !Boolean.TRUE.equals(servicio.estadoLegal(identidad.id()).get("vigente")))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Revisa y acepta los documentos vigentes desde Mi cuenta");
                return true;
            }
        }).addPathPatterns("/api/cuenta/**", "/api/admin/**", "/api/auth/logout", "/api/auth/reenviar-verificacion",
                "/api/personas/**", "/api/contactos/**", "/api/municipios/**", "/api/colonias/**",
                "/api/tipos-reporte/**", "/api/reportes/**", "/api/notificaciones/**", "/api/archivos/**",
                "/api/trabajador/**");
    }
}
