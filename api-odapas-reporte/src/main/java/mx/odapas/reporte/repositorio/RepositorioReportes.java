package mx.odapas.reporte.repositorio;

import mx.odapas.reporte.modelo.Reporte;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RepositorioReportes {
    private final JdbcTemplate jdbc;
    private static final String CONSULTA = """
            SELECT folio, tipo, descripcion, colonia, calle, zona, latitud, longitud,
                   estado, prioridad, fecha_creacion
            FROM public.v_reportes
            """;
    private static final RowMapper<Reporte> MAPEO = (resultado, fila) -> new Reporte(
            resultado.getString("folio"), resultado.getString("tipo"), resultado.getString("descripcion"),
            resultado.getString("colonia"), resultado.getString("calle"), resultado.getString("zona"),
            coordenada(resultado.getBigDecimal("latitud")), coordenada(resultado.getBigDecimal("longitud")),
            resultado.getString("estado"), resultado.getString("prioridad"),
            resultado.getObject("fecha_creacion", OffsetDateTime.class)
                    .atZoneSameInstant(ZoneId.of("America/Mexico_City"))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    public RepositorioReportes(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Reporte> listar(String tipo, String zona) {
        String sql = CONSULTA + " WHERE 1=1";
        List<Object> parametros = new ArrayList<>();
        if (tipo != null && !tipo.isBlank()) {
            sql += " AND LOWER(tipo)=LOWER(?)";
            parametros.add(tipo);
        }
        if (zona != null && !zona.isBlank()) {
            sql += " AND LOWER(zona)=LOWER(?)";
            parametros.add(zona);
        }
        return jdbc.query(sql + " ORDER BY fecha_creacion, folio", MAPEO, parametros.toArray());
    }

    public Optional<Reporte> obtener(String folio) {
        return jdbc.query(CONSULTA + " WHERE LOWER(folio)=LOWER(?)", MAPEO, folio).stream().findFirst();
    }

    public List<Reporte> listarPropios(String tipo,String zona,int usuario) {
        String sql=CONSULTA+" WHERE id IN (SELECT id FROM public.reporte WHERE ciudadano_id=?)";
        List<Object> parametros=new ArrayList<>();parametros.add(usuario);
        if(tipo!=null && !tipo.isBlank()){sql+=" AND LOWER(tipo)=LOWER(?)";parametros.add(tipo);}
        if(zona!=null && !zona.isBlank()){sql+=" AND LOWER(zona)=LOWER(?)";parametros.add(zona);}
        return jdbc.query(sql+" ORDER BY fecha_creacion, folio",MAPEO,parametros.toArray());
    }
    public Optional<Reporte> obtenerPropio(String folio,int usuario) {
        return jdbc.query(CONSULTA+" WHERE LOWER(folio)=LOWER(?) AND id IN (SELECT id FROM public.reporte WHERE ciudadano_id=?)",
          MAPEO,folio,usuario).stream().findFirst();
    }

    private static Double coordenada(BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }
}
