package mx.odapas.reporte.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Encabezados HTTP de seguridad para todas las respuestas de la API. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EncabezadosSeguridad extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadena)
            throws ServletException, IOException {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "no-referrer");
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        res.setHeader("Cross-Origin-Resource-Policy", "same-site");
        if (req.getRequestURI().startsWith("/api/")) {
            res.setHeader("Cache-Control", "no-store");
            res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        }
        if (req.isSecure()) res.setHeader("Strict-Transport-Security", "max-age=31536000");
        cadena.doFilter(req, res);
    }
}
