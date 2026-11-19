package mx.odapas.reporte.servicio;

import mx.odapas.reporte.privacidad.Claves;
import mx.odapas.reporte.privacidad.ServicioCuenta;
import mx.odapas.reporte.seguridad.LimitadorIntentos;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Inicio de sesión protegido, verificación de correo, recuperación de contraseña y activación de personal. */
@Service
public class ServicioAcceso {
    private final JdbcTemplate jdbc;
    private final ServicioCuenta cuentas;
    private final ServicioEnlaces enlaces;
    private final LimitadorIntentos limitador;
    private final Bitacora bitacora;

    public ServicioAcceso(JdbcTemplate jdbc, ServicioCuenta cuentas, ServicioEnlaces enlaces,
                          LimitadorIntentos limitador, Bitacora bitacora) {
        this.jdbc = jdbc;
        this.cuentas = cuentas;
        this.enlaces = enlaces;
        this.limitador = limitador;
        this.bitacora = bitacora;
    }

    private static String correoDe(Map<String, Object> d) {
        return d.get("email") instanceof String s ? s.trim().toLowerCase(Locale.ROOT) : "";
    }

    /** Login con bloqueo temporal tras 5 intentos fallidos. */
    public Map<String, Object> login(Map<String, Object> d, String ip) {
        String correo = correoDe(d);
        limitador.comprobarLogin(correo, ip);
        try {
            var sesion = cuentas.login(d);
            limitador.limpiar(correo);
            return sesion;
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                limitador.registrarFallo(correo, ip);
                bitacora.registrar(null, "login_fallido", "usuario", correo.length() > 80 ? correo.substring(0, 80) : correo, null, ip);
            }
            throw e;
        }
    }

    public Map<String, Object> registrar(Map<String, Object> d, String ip) {
        limitador.comprobarSolicitud("registro", ip);
        return cuentas.registrar(d);
    }

    @Transactional
    public Map<String, Object> verificarCorreo(String token) {
        int usuario = enlaces.consumir(token, "verificar_correo");
        jdbc.update("UPDATE public.contacto c SET correo_verificado_en=CURRENT_TIMESTAMP FROM public.usuario u " +
                "WHERE u.persona_id=c.persona_id AND u.id=? AND c.correo_verificado_en IS NULL", usuario);
        cuentas.evento(usuario, usuario, "correo_verificado");
        return Map.of("mensaje", "Correo verificado. Ya puedes enviar reportes.");
    }

    @Transactional
    public Map<String, Object> reenviarVerificacion(int usuario, String ip) {
        limitador.comprobarSolicitud("verificacion", ip);
        Boolean verificado = jdbc.queryForObject("SELECT c.correo_verificado_en IS NOT NULL FROM public.contacto c " +
                "JOIN public.usuario u ON u.persona_id=c.persona_id WHERE u.id=?", Boolean.class, usuario);
        if (Boolean.TRUE.equals(verificado))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tu correo ya está verificado");
        String url = enlaces.emitir(usuario, "verificar_correo", null, ServicioEnlaces.VIGENCIA_VERIFICAR);
        return enlaces.respuesta("Te enviamos un nuevo enlace de verificación.", url);
    }

    /** Siempre responde lo mismo, exista o no la cuenta, para no revelar qué correos están registrados. */
    @Transactional
    public Map<String, Object> recuperar(Map<String, Object> d, String ip) {
        limitador.comprobarSolicitud("recuperar", ip);
        String correo = ServicioCuenta.correo(d);
        var ids = jdbc.queryForList("SELECT u.id FROM public.usuario u JOIN public.contacto c ON c.persona_id=u.persona_id " +
                "WHERE c.correo=? AND u.estado='activo'", Integer.class, correo);
        String url = ids.isEmpty() ? null : enlaces.emitir(ids.get(0), "recuperar_password", null, ServicioEnlaces.VIGENCIA_RECUPERAR);
        return enlaces.respuesta("Si el correo está registrado, recibirás un enlace válido por 30 minutos.", url);
    }

    @Transactional
    public Map<String, Object> restablecer(Map<String, Object> d) {
        String nueva = ServicioCuenta.clave(d, "password");
        int usuario = enlaces.consumir(d.get("token") instanceof String t ? t : null, "recuperar_password");
        jdbc.update("UPDATE public.usuario SET password_hash=? WHERE id=? AND estado='activo'", Claves.guardar(nueva), usuario);
        jdbc.update("UPDATE public.sesion_usuario SET revocada_en=CURRENT_TIMESTAMP WHERE usuario_id=? AND revocada_en IS NULL", usuario);
        String correo = jdbc.queryForObject("SELECT c.correo FROM public.contacto c JOIN public.usuario u ON u.persona_id=c.persona_id WHERE u.id=?", String.class, usuario);
        limitador.limpiar(correo);
        cuentas.evento(usuario, usuario, "restablecer_password_y_revocacion");
        return Map.of("mensaje", "Contraseña actualizada. Inicia sesión con tu nueva contraseña.");
    }

    /** El trabajador define su contraseña con la invitación que generó el administrador. */
    @Transactional
    public Map<String, Object> activar(Map<String, Object> d, String ip) {
        String nueva = ServicioCuenta.clave(d, "password");
        int usuario = enlaces.consumir(d.get("token") instanceof String t ? t : null, "activar_trabajador");
        int filas = jdbc.update("UPDATE public.usuario SET password_hash=?,estado='activo' WHERE id=? AND estado='pendiente'",
                Claves.guardar(nueva), usuario);
        if (filas == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya fue activada o está suspendida");
        jdbc.update("UPDATE public.contacto c SET correo_verificado_en=COALESCE(correo_verificado_en,CURRENT_TIMESTAMP) " +
                "FROM public.usuario u WHERE u.persona_id=c.persona_id AND u.id=?", usuario);
        cuentas.aceptaciones(usuario, d);
        jdbc.update("INSERT INTO public.preferencia_privacidad(usuario_id,comunicaciones_opcionales) VALUES(?,FALSE) ON CONFLICT DO NOTHING", usuario);
        cuentas.evento(usuario, usuario, "activacion_trabajador");
        bitacora.registrar(usuario, "activacion_trabajador", "usuario", String.valueOf(usuario), null, ip);
        return new LinkedHashMap<>(cuentas.sesion(usuario));
    }
}
