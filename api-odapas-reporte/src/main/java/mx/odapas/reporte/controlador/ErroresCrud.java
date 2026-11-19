package mx.odapas.reporte.controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Map;

/** Respuestas de error uniformes {"error": "..."} sin exponer detalles internos. */
@RestControllerAdvice
public class ErroresCrud {
    private static final Logger LOG = LoggerFactory.getLogger(ErroresCrud.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> solicitud(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error",
                error.getReason() == null ? "Solicitud inválida" : error.getReason()));
    }

    /** Mensaje de una regla de negocio escrita en los triggers del proyecto (RAISE EXCEPTION). */
    private static String mensajeRegla(SQLException sql) {
        String m = sql.getMessage();
        if (m == null) return null;
        int i = m.indexOf("ERROR: ");
        String linea = (i >= 0 ? m.substring(i + 7) : m).split("\n")[0].trim();
        return linea.isEmpty() || linea.length() > 200 || linea.contains("violates") || linea.contains("viola") ? null : linea;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException error) {
        Throwable causa = error.getMostSpecificCause();
        String estado = causa instanceof SQLException sql ? sql.getSQLState() : "";
        if ("23505".equals(estado))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ya existe un registro con esos datos únicos."));
        if ("23503".equals(estado))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El registro relacionado no existe o este registro está siendo utilizado. Revisa las relaciones antes de eliminar."));
        if ("23P01".equals(estado))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ya existe una asignación vigente que se traslapa en el tiempo."));
        if ("23514".equals(estado) && causa instanceof SQLException sql && mensajeRegla(sql) != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", mensajeRegla(sql)));
        return ResponseEntity.badRequest().body(Map.of("error", "Los datos no cumplen las restricciones de la base de datos."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MultipartException.class})
    public ResponseEntity<Map<String, String>> cuerpo(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("error", "La solicitud no tiene el formato esperado."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tamano(MaxUploadSizeExceededException error) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "Cada fotografía debe pesar máximo 5 MB."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> metodo(HttpRequestMethodNotSupportedException error) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("error", "Método no permitido en esta ruta."));
    }

    /** Reglas diferidas que PostgreSQL comprueba al confirmar la transacción. */
    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ResponseEntity<Map<String, String>> confirmacion(org.springframework.transaction.TransactionSystemException error) {
        Throwable c = error;
        while (c != null && !(c instanceof SQLException)) c = c.getCause();
        if (c instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("23"))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                    mensajeRegla(sql) == null ? "La operación no cumple las reglas de la base de datos." : mensajeRegla(sql)));
        LOG.error("Error al confirmar transacción", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No fue posible completar la operación."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> datos(DataAccessException error) {
        LOG.error("Error de base de datos", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No fue posible completar la operación en la base de datos."));
    }
}
