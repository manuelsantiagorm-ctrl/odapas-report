package mx.odapas.reporte.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protección contra fuerza bruta y abuso (en memoria).
 * - Inicio de sesión: 5 fallos por correo o por IP bloquean 15 minutos.
 * - Registro, recuperación y reenvío de enlaces: máximo 10 por hora por IP.
 */
@Component
public class LimitadorIntentos {
    public static final int MAX_FALLOS_LOGIN = 5;
    private static final Duration BLOQUEO_LOGIN = Duration.ofMinutes(15);
    private static final int MAX_SOLICITUDES = 10;
    private static final Duration VENTANA_SOLICITUDES = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> solicitudes = new ConcurrentHashMap<>();

    private static int vigentes(Deque<Instant> cola, Duration ventana) {
        Instant limite = Instant.now().minus(ventana);
        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst().isBefore(limite)) cola.pollFirst();
            return cola.size();
        }
    }

    /** Lanza 429 si la cuenta o la IP están bloqueadas temporalmente. */
    public void comprobarLogin(String correo, String ip) {
        for (String clave : new String[]{"c:" + correo, "i:" + ip}) {
            var cola = fallos.get(clave);
            if (cola != null && vigentes(cola, BLOQUEO_LOGIN) >= MAX_FALLOS_LOGIN * (clave.startsWith("i:") ? 4 : 1))
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Demasiados intentos fallidos. Espera 15 minutos o recupera tu contraseña.");
        }
    }

    public void registrarFallo(String correo, String ip) {
        for (String clave : new String[]{"c:" + correo, "i:" + ip}) {
            var cola = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
            synchronized (cola) { cola.addLast(Instant.now()); }
        }
    }

    public void limpiar(String correo) { fallos.remove("c:" + correo); }

    /** Limita operaciones públicas sensibles (registro, recuperación, reenvíos). */
    public void comprobarSolicitud(String accion, String ip) {
        var cola = solicitudes.computeIfAbsent(accion + ":" + ip, k -> new ArrayDeque<>());
        if (vigentes(cola, VENTANA_SOLICITUDES) >= MAX_SOLICITUDES)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has hecho demasiadas solicitudes. Intenta de nuevo más tarde.");
        synchronized (cola) { cola.addLast(Instant.now()); }
    }
}
