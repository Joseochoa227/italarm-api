package co.italarm.api.garantias.infraestructura;

import co.italarm.api.shared.dominio.EstadoGarantia;
import co.italarm.api.shared.dominio.Garantia;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consulta de garantías sobre la vista {@code garantia} (V10): la mano de obra de cada instalación
 * y cada equipo con serial vendido o instalado, de documentos no anulados (RF-123, RF-124). Es un
 * modelo de lectura que cruza ventas, instalaciones e inventario.
 */
@Repository
public class ConsultaGarantiasSql {

  private static final RowMapper<FilaGarantia> FILA =
      (rs, n) ->
          new FilaGarantia(
              rs.getString("tipo"),
              rs.getString("clase"),
              rs.getLong("documento_id"),
              rs.getLong("documento_numero"),
              rs.getObject("fecha", LocalDate.class),
              rs.getLong("cliente_id"),
              rs.getString("cliente_nombre"),
              rs.getObject("serial_id", Long.class),
              rs.getString("serial_numero"),
              rs.getObject("producto_id", Long.class),
              rs.getString("producto_nombre"),
              rs.getObject("vencimiento", LocalDate.class));

  private final NamedParameterJdbcTemplate jdbc;

  public ConsultaGarantiasSql(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Una garantía de la vista. {@code serialId} es vacío en la mano de obra. */
  public record FilaGarantia(
      String tipo,
      String clase,
      long documentoId,
      long documentoNumero,
      LocalDate fecha,
      long clienteId,
      String clienteNombre,
      Long serialId,
      String serialNumero,
      Long productoId,
      String productoNombre,
      LocalDate vencimiento) {}

  /** Filtros: cada uno es opcional. {@code tipo} es INSTALACION o VENTA. */
  public record Filtro(
      EstadoGarantia estado, Long clienteId, String tipo, String serial, LocalDate hoy) {}

  /** Garantías del filtro, de la que vence primero a la última. */
  public Page<FilaGarantia> buscar(Filtro filtro, Pageable pagina) {
    MapSqlParameterSource parametros = new MapSqlParameterSource();
    String donde = condiciones(filtro, parametros);
    Long total =
        jdbc.queryForObject("select count(*) from garantia" + donde, parametros, Long.class);
    parametros.addValue("limite", pagina.getPageSize());
    parametros.addValue("desplazamiento", pagina.getOffset());
    List<FilaGarantia> filas =
        jdbc.query(
            "select * from garantia"
                + donde
                + " order by vencimiento, documento_id, serial_id nulls first"
                + " limit :limite offset :desplazamiento",
            parametros,
            FILA);
    return new PageImpl<>(filas, pagina, total == null ? 0 : total);
  }

  /** Garantía de mano de obra de una instalación no anulada. */
  public Optional<FilaGarantia> deInstalacion(Long instalacionId) {
    return jdbc
        .query(
            "select * from garantia where tipo = 'INSTALACION' and clase = 'MANO_OBRA'"
                + " and documento_id = :id",
            new MapSqlParameterSource("id", instalacionId),
            FILA)
        .stream()
        .findFirst();
  }

  /** Garantía de un serial vendido o instalado en un documento no anulado. */
  public Optional<FilaGarantia> deSerial(Long serialId) {
    return jdbc
        .query(
            "select * from garantia where serial_id = :id",
            new MapSqlParameterSource("id", serialId),
            FILA)
        .stream()
        .findFirst();
  }

  private static String condiciones(Filtro filtro, MapSqlParameterSource parametros) {
    List<String> condiciones = new ArrayList<>();
    if (filtro.estado() != null) {
      parametros.addValue("hoy", filtro.hoy());
      parametros.addValue("porVencer", filtro.hoy().plusDays(Garantia.DIAS_POR_VENCER));
      condiciones.add(
          switch (filtro.estado()) {
            case VENCIDA -> "vencimiento < :hoy";
            case POR_VENCER -> "vencimiento between :hoy and :porVencer";
            case VIGENTE -> "vencimiento > :porVencer";
          });
    }
    if (filtro.clienteId() != null) {
      parametros.addValue("cliente", filtro.clienteId());
      condiciones.add("cliente_id = :cliente");
    }
    if (filtro.tipo() != null) {
      parametros.addValue("tipo", filtro.tipo());
      condiciones.add("tipo = :tipo");
    }
    if (filtro.serial() != null && !filtro.serial().isBlank()) {
      String escapado =
          filtro
              .serial()
              .trim()
              .toUpperCase(Locale.ROOT)
              .replace("\\", "\\\\")
              .replace("%", "\\%")
              .replace("_", "\\_");
      parametros.addValue("serial", "%" + escapado + "%");
      condiciones.add("serial_numero like :serial escape '\\'");
    }
    return condiciones.isEmpty() ? "" : " where " + String.join(" and ", condiciones);
  }
}
