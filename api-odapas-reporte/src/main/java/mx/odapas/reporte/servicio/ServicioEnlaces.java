package mx.odapas.reporte.servicio;

import mx.odapas.reporte.privacidad.Claves;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enlaces de un solo uso: verificar correo, recuperar contraseña y activar trabajador.
 * El token viaja solo en el enlace; en la base se guarda únicamente su huella SHA-256.
 *
 * Este proyecto escolar no tiene servidor de correo. Con odapas.enlaces.mostrar=true
 * (modo demostración) el enlace se devuelve en la respuesta y se escribe en la consola,
 * para poder probar el flujo completo. En producción debe ponerse en false y enviarse por correo.
 */
@Service
public class ServicioEnlaces {
    private static final Logger LOG = LoggerFactory.getLogger(ServicioEnlaces.class);
    public static final Duration VIGENCIA_VERIFICAR = Duration.ofHours(48);
    public static final Duration VIGENCIA_RECUPERAR = Duration.ofMinutes(30);
    public static final Duration VIGENCIA_ACTIVAR = Duration.ofHours(24);

    private final JdbcTemplate jdbc;
    private final boolean mostrar;
    private final String urlFrontend;

    public ServicioEnlaces(JdbcTemplate jdbc,
                           @Value("${odapas.enlaces.mostrar:true}") boolean mostrar,
                           @Value("${odapas.url-frontend:http://localhost:4200}") String urlFrontend) {
        this.jdbc = jdbc;
        this.mostrar = mostrar;
        this.urlFrontend = urlFrontend.replaceAll("/+$", "");
    }

    public boolean modoDemostracion() { return mostrar; }

    /** Invalida los enlaces anteriores del mismo tipo y genera uno nuevo. Devuelve la URL completa. */
    public String emitir(int usuario, String tipo, Integer creadoPor, Duration vigencia) {
        jdbc.update("UPDATE public.token_acceso SET expira_en=GREATEST(creado_en+INTERVAL '1 second',CURRENT_TIMESTAMP) " +
                "WHERE usuario_id=? AND tipo=?::public.tipo_token AND utilizado_en IS NULL AND expira_en>CURRENT_TIMESTAMP",
                usuario, tipo);
        String token = Claves.token();
        jdbc.update("INSERT INTO public.token_acceso(usuario_id,tipo,token_hash,creado_por,expira_en) " +
                "VALUES(?,?::public.tipo_token,?,?,CURRENT_TIMESTAMP + (? * INTERVAL '1 second'))",
                usuario, tipo, Claves.huella(token), creadoPor, vigencia.toSeconds());
        String ruta = switch (tipo) {
            case "verificar_correo" -> "/verificar";
            case "recuperar_password" -> "/restablecer";
            default -> "/activar";
        };
        String url = urlFrontend + ruta + "?token=" + token;
        if (mostrar) LOG.info("[MODO DEMOSTRACIÓN] Enlace {} para usuario {}: {}", tipo, usuario, url);
        return url;
    }

    /** Agrega el enlace a la respuesta solo en modo demostración. */
    public Map<String, Object> respuesta(String mensaje, String url) {
        var r = new LinkedHashMap<String, Object>();
        r.put("mensaje", mensaje);
        if (mostrar && url != null) {
            r.put("enlaceDemo", url);
            r.put("aviso", "Modo demostración: en producción este enlace se enviaría por correo y no se mostraría aquí.");
        }
        return r;
    }

    /** Valida y CONSUME el token (un solo uso). Devuelve el id del usuario. */
    public int consumir(String token, String tipo) {
        if (token == null || token.length() < 20 || token.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enlace inválido");
        var filas = jdbc.queryForList("SELECT id,usuario_id FROM public.token_acceso WHERE token_hash=? AND tipo=?::public.tipo_token " +
                "AND utilizado_en IS NULL AND expira_en>CURRENT_TIMESTAMP FOR UPDATE", Claves.huella(token), tipo);
        if (filas.isEmpty())
            throw new ResponseStatusException(HttpStatus.GONE, "El enlace no es válido, ya se usó o venció. Solicita uno nuevo.");
        jdbc.update("UPDATE public.token_acceso SET utilizado_en=CURRENT_TIMESTAMP WHERE id=?", filas.get(0).get("id"));
        return ((Number) filas.get(0).get("usuario_id")).intValue();
    }
}
