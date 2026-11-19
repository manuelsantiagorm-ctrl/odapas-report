package mx.odapas.reporte.servicio;

import mx.odapas.reporte.privacidad.ServicioCuenta.Identidad;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fotografías de reportes y reparaciones.
 * - Se aceptan solo JPG, PNG y WEBP de hasta 5 MB; el tipo se comprueba con los primeros bytes
 *   del archivo (no se confía en la extensión ni en el Content-Type del cliente).
 * - Se guardan fuera de la base, con nombre aleatorio; la base solo guarda metadatos JSONB.
 * - Para descargarlas se exige sesión y permiso sobre el reporte u orden relacionada.
 */
@Service
public class ServicioArchivos {
    public static final long MAXIMO = 5L * 1024 * 1024;
    private final Path carpeta;
    private final JdbcTemplate jdbc;

    public ServicioArchivos(@Value("${odapas.archivos.carpeta:./archivos}") String carpeta, JdbcTemplate jdbc) throws IOException {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
        Files.createDirectories(this.carpeta);
        this.jdbc = jdbc;
    }

    private static String detectar(byte[] b, int n) {
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P')
            return "image/webp";
        return null;
    }

    /** Valida y guarda la foto. Devuelve los metadatos para la columna foto_json. */
    public Map<String, Object> guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw Datos.invalido("Adjunta una fotografía");
        if (archivo.getSize() > MAXIMO) throw Datos.invalido("Cada fotografía debe pesar máximo 5 MB");
        try (InputStream in = archivo.getInputStream()) {
            byte[] cabecera = new byte[12];
            int leidos = in.readNBytes(cabecera, 0, 12);
            String tipo = detectar(cabecera, leidos);
            if (tipo == null) throw Datos.invalido("Solo se aceptan imágenes JPG, PNG o WEBP");
            String extension = switch (tipo) { case "image/png" -> "png"; case "image/webp" -> "webp"; default -> "jpg"; };
            String nombre = UUID.randomUUID() + "." + extension;
            Path destino = carpeta.resolve(nombre);
            archivo.transferTo(destino);
            String original = archivo.getOriginalFilename() == null ? nombre
                    : archivo.getOriginalFilename().replaceAll("[^A-Za-z0-9._ -]", "_");
            if (original.isBlank()) original = nombre;
            var meta = new LinkedHashMap<String, Object>();
            meta.put("url", "/api/archivos/" + nombre);
            meta.put("nombre", original.length() > 100 ? original.substring(0, 100) : original);
            meta.put("tipo", tipo);
            meta.put("tamano", archivo.getSize());
            return meta;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible guardar la fotografía");
        }
    }

    /** Devuelve el archivo si el usuario tiene permiso sobre la foto. */
    public Path leer(String nombre, Identidad quien) {
        if (nombre == null || !nombre.matches("[0-9a-f-]{36}\\.(jpg|png|webp)"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        String url = "/api/archivos/" + nombre;
        boolean permitido = quien.admin() || Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                  SELECT 1 FROM public.fotografia_reporte f JOIN public.reporte r ON r.id=f.reporte_id
                  WHERE f.foto_json->>'url'=? AND (r.ciudadano_id=? OR EXISTS (
                    SELECT 1 FROM public.orden_trabajo o JOIN public.asignacion_trabajo a ON a.orden_id=o.id
                    WHERE o.reporte_id=r.id AND a.trabajador_id=?))
                  UNION ALL
                  SELECT 1 FROM public.fotografia_reparacion f JOIN public.reparacion rp ON rp.id=f.reparacion_id
                  JOIN public.asignacion_trabajo a ON a.id=rp.asignacion_id JOIN public.orden_trabajo o ON o.id=a.orden_id
                  JOIN public.reporte r ON r.id=o.reporte_id
                  WHERE f.foto_json->>'url'=? AND (r.ciudadano_id=? OR EXISTS (
                    SELECT 1 FROM public.asignacion_trabajo a2 WHERE a2.orden_id=o.id AND a2.trabajador_id=?)))
                """, Boolean.class, url, quien.id(), quien.id(), url, quien.id(), quien.id()));
        Path archivo = carpeta.resolve(nombre).normalize();
        if (!permitido || !archivo.startsWith(carpeta) || !Files.exists(archivo))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        return archivo;
    }
}
