package co.italarm.api.reportes.infraestructura;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas de lectura de Inicio y reportes (secciones 3.2 y 3.16). Cruzan ventas, instalaciones,
 * compras, inventario y cotizaciones, como la vista de garantías. Las cifras usan los valores en
 * USD guardados en cada documento con sus tasas (RN-04) y nunca cuentan los anulados.
 */
@Repository
public class ConsultasReportesSql {

  /** Ventas o instalaciones activas de un rango. */
  private static final String DOCUMENTOS =
      "select 'VENTA' as tipo, id, numero, fecha, cliente_id, cliente_nombre, moneda, total,"
          + " costo, utilidad, porcentaje_utilidad, total_usd, costo_usd, utilidad_usd"
          + " from venta where estado = 'ACTIVA' and fecha between :desde and :hasta"
          + " union all"
          + " select 'INSTALACION', id, numero, fecha, cliente_id, cliente_nombre, moneda, total,"
          + " costo, utilidad, porcentaje_utilidad, total_usd, costo_usd, utilidad_usd"
          + " from instalacion where estado = 'ACTIVA' and fecha between :desde and :hasta";

  private final NamedParameterJdbcTemplate jdbc;

  public ConsultasReportesSql(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Totales de ventas o instalaciones (RF-08, RF-136). {@code tipo}: VENTA o INSTALACION. */
  public record Totales(
      long cantidad,
      BigDecimal totalUsd,
      BigDecimal costoUsd,
      BigDecimal utilidadUsd,
      List<PorMoneda> porMoneda) {}

  public record PorMoneda(String moneda, long cantidad, BigDecimal total) {}

  public Map<String, Totales> totales(LocalDate desde, LocalDate hasta) {
    MapSqlParameterSource parametros = rango(desde, hasta);
    Map<String, Totales> resultado = new LinkedHashMap<>();
    for (String tipo : List.of("VENTA", "INSTALACION")) {
      parametros.addValue("tipo", tipo);
      List<PorMoneda> porMoneda =
          jdbc.query(
              "select moneda, count(*) as cantidad, sum(total) as total from ("
                  + DOCUMENTOS
                  + ") d where tipo = :tipo group by moneda order by moneda",
              parametros,
              (rs, n) ->
                  new PorMoneda(
                      rs.getString("moneda"), rs.getLong("cantidad"), rs.getBigDecimal("total")));
      resultado.put(
          tipo,
          jdbc.queryForObject(
              "select count(*) as cantidad, coalesce(sum(total_usd), 0) as total_usd,"
                  + " coalesce(sum(costo_usd), 0) as costo_usd,"
                  + " coalesce(sum(utilidad_usd), 0) as utilidad_usd from ("
                  + DOCUMENTOS
                  + ") d where tipo = :tipo",
              parametros,
              (rs, n) ->
                  new Totales(
                      rs.getLong("cantidad"),
                      rs.getBigDecimal("total_usd"),
                      rs.getBigDecimal("costo_usd"),
                      rs.getBigDecimal("utilidad_usd"),
                      porMoneda)));
    }
    return resultado;
  }

  /** Total por día y tipo, para las barras del gráfico (RF-137). */
  public record PorDia(LocalDate fecha, String tipo, long cantidad, BigDecimal totalUsd) {}

  public List<PorDia> porDia(LocalDate desde, LocalDate hasta) {
    return jdbc.query(
        "select fecha, tipo, count(*) as cantidad, sum(total_usd) as total_usd from ("
            + DOCUMENTOS
            + ") d group by fecha, tipo order by fecha",
        rango(desde, hasta),
        (rs, n) ->
            new PorDia(
                rs.getObject("fecha", LocalDate.class),
                rs.getString("tipo"),
                rs.getLong("cantidad"),
                rs.getBigDecimal("total_usd")));
  }

  /** Inventario valorizado de hoy (RF-08, P-58): productos con stock y stock × costo actual. */
  public record Inventario(long productos, BigDecimal valorUsd) {}

  public Inventario inventario() {
    return jdbc.queryForObject(
        "select count(*) as productos,"
            + " coalesce(sum(stock * coalesce(costo_actual_usd, 0)), 0) as valor"
            + " from producto where stock > 0",
        new MapSqlParameterSource(),
        (rs, n) -> new Inventario(rs.getLong("productos"), rs.getBigDecimal("valor")));
  }

  /** Producto activo por debajo de su mínimo (RF-10, RF-140). */
  public record StockBajo(
      Long id, String codigo, String nombre, String unidad, BigDecimal stock, BigDecimal minimo) {}

  private static final String STOCK_BAJO =
      " from producto p join unidad_medida u on u.id = p.unidad_medida_id"
          + " where p.activo and p.stock_minimo is not null and p.stock < p.stock_minimo";

  /** Del más crítico (menor stock ÷ mínimo) al menos crítico. Sin límite, todos. */
  public List<StockBajo> stockBajo(Integer limite) {
    MapSqlParameterSource parametros = new MapSqlParameterSource("limite", limite);
    return jdbc.query(
        "select p.id, p.codigo, p.nombre, u.abreviatura, p.stock, p.stock_minimo"
            + STOCK_BAJO
            + " order by p.stock / p.stock_minimo, p.nombre"
            + (limite == null ? "" : " limit :limite"),
        parametros,
        (rs, n) ->
            new StockBajo(
                rs.getLong("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("abreviatura"),
                rs.getBigDecimal("stock"),
                rs.getBigDecimal("stock_minimo")));
  }

  public long contarStockBajo() {
    return jdbc.queryForObject(
        "select count(*)" + STOCK_BAJO, new MapSqlParameterSource(), Long.class);
  }

  /**
   * Producto más vendido o más usado en instalaciones (RF-138, P-59), por valor en USD (el subtotal
   * de la línea convertido con las tasas del documento).
   */
  public record ProductoTop(
      Long productoId,
      String codigo,
      String nombre,
      String unidad,
      BigDecimal cantidad,
      BigDecimal valorUsd) {}

  /** {@code tipo}: VENTA o INSTALACION. */
  public List<ProductoTop> masMovidos(String tipo, LocalDate desde, LocalDate hasta, int limite) {
    String documento = "VENTA".equals(tipo) ? "venta" : "instalacion";
    MapSqlParameterSource parametros = rango(desde, hasta).addValue("limite", limite);
    return jdbc.query(
        "select l.producto_id, l.codigo, l.descripcion, l.unidad, sum(l.cantidad) as cantidad,"
            + " sum(case d.moneda when 'USD' then l.subtotal"
            + " when 'COP' then l.subtotal / d.trm else l.subtotal / d.tasa_ves end) as valor_usd"
            + " from linea_"
            + documento
            + " l join "
            + documento
            + " d on d.id = l."
            + documento
            + "_id where d.estado = 'ACTIVA' and d.fecha between :desde and :hasta"
            + " group by l.producto_id, l.codigo, l.descripcion, l.unidad"
            + " order by valor_usd desc, cantidad desc limit :limite",
        parametros,
        (rs, n) ->
            new ProductoTop(
                rs.getLong("producto_id"),
                rs.getString("codigo"),
                rs.getString("descripcion"),
                rs.getString("unidad"),
                rs.getBigDecimal("cantidad"),
                rs.getBigDecimal("valor_usd")));
  }

  /** Compras de un proveedor en el período (RF-139). */
  public record CompraProveedor(
      Long proveedorId, String proveedor, long compras, BigDecimal totalUsd) {}

  public List<CompraProveedor> comprasPorProveedor(LocalDate desde, LocalDate hasta) {
    return jdbc.query(
        "select p.id, p.nombre, count(*) as compras, sum(c.total_usd) as total_usd"
            + " from compra c join proveedor p on p.id = c.proveedor_id"
            + " where c.estado = 'ACTIVA' and c.fecha between :desde and :hasta"
            + " group by p.id, p.nombre order by total_usd desc, p.nombre",
        rango(desde, hasta),
        (rs, n) ->
            new CompraProveedor(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getLong("compras"),
                rs.getBigDecimal("total_usd")));
  }

  /** Cotizaciones del período por estado (RF-142). */
  public Map<String, Long> cotizacionesPorEstado(LocalDate desde, LocalDate hasta) {
    Map<String, Long> resultado = new LinkedHashMap<>();
    jdbc.query(
        "select estado, count(*) as cantidad from cotizacion"
            + " where fecha between :desde and :hasta group by estado",
        rango(desde, hasta),
        rs -> {
          resultado.put(rs.getString("estado"), rs.getLong("cantidad"));
        });
    return resultado;
  }

  /**
   * Movimiento reciente para Inicio (RF-13, P-61).
   *
   * @param tipo VENTA, INSTALACION, COMPRA, AJUSTE o INVENTARIO_INICIAL
   * @param detalle cliente, proveedor o producto
   * @param moneda la del documento; USD en ajustes e inventario inicial
   */
  public record Movimiento(
      String tipo,
      Long id,
      long numero,
      String detalle,
      String usuario,
      Instant registradoEn,
      BigDecimal valor,
      String moneda,
      boolean anulado) {}

  public List<Movimiento> ultimosMovimientos(int limite) {
    return jdbc.query(
        "select m.*, u.nombre as usuario from ("
            + "select 'VENTA' as tipo, id, numero, cliente_nombre as detalle, created_by,"
            + " created_at, total as valor, moneda, estado = 'ANULADA' as anulado from venta"
            + " union all select 'INSTALACION', id, numero, cliente_nombre, created_by,"
            + " created_at, total, moneda, estado = 'ANULADA' from instalacion"
            + " union all select 'COMPRA', c.id, c.numero, p.nombre, c.created_by, c.created_at,"
            + " c.total, c.moneda, c.estado = 'ANULADA' from compra c"
            + " join proveedor p on p.id = c.proveedor_id"
            + " union all select 'AJUSTE', a.id, a.numero,"
            + " case a.tipo when 'ENTRADA' then 'Entrada: ' else 'Salida: ' end || p.nombre,"
            + " a.created_by, a.created_at, a.cantidad * a.costo_unitario_usd, 'USD', false"
            + " from ajuste a join producto p on p.id = a.producto_id"
            + " union all select 'INVENTARIO_INICIAL', i.id, i.numero, i.archivo_nombre,"
            + " i.created_by, i.created_at,"
            + " (select coalesce(sum(l.cantidad * l.costo_unitario_usd), 0)"
            + " from linea_inventario_inicial l where l.inventario_inicial_id = i.id), 'USD', false"
            + " from inventario_inicial i"
            + ") m left join usuario u on u.id = m.created_by"
            + " order by m.created_at desc, m.id desc limit :limite",
        new MapSqlParameterSource("limite", limite),
        (rs, n) ->
            new Movimiento(
                rs.getString("tipo"),
                rs.getLong("id"),
                rs.getLong("numero"),
                rs.getString("detalle"),
                rs.getString("usuario"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getBigDecimal("valor"),
                rs.getString("moneda"),
                rs.getBoolean("anulado")));
  }

  /** Utilidad de una venta o instalación (RF-143, P-63). */
  public record Utilidad(
      String tipo,
      Long id,
      long numero,
      LocalDate fecha,
      Long clienteId,
      String cliente,
      String moneda,
      BigDecimal total,
      BigDecimal costo,
      BigDecimal utilidad,
      BigDecimal porcentajeUtilidad,
      BigDecimal totalUsd,
      BigDecimal costoUsd,
      BigDecimal utilidadUsd) {}

  /**
   * @param tipo VENTA o INSTALACION; vacío, ambas
   * @param porUtilidad ordena por utilidad en USD (mayor primero) en lugar de por fecha
   */
  public Page<Utilidad> utilidad(
      LocalDate desde, LocalDate hasta, String tipo, boolean porUtilidad, Pageable pagina) {
    MapSqlParameterSource parametros = rango(desde, hasta).addValue("tipo", tipo);
    String donde = tipo == null ? "" : " where tipo = :tipo";
    Long total =
        jdbc.queryForObject(
            "select count(*) from (" + DOCUMENTOS + ") d" + donde, parametros, Long.class);
    parametros
        .addValue("limite", pagina.getPageSize())
        .addValue("desplazamiento", pagina.getOffset());
    List<Utilidad> filas =
        jdbc.query(
            "select * from ("
                + DOCUMENTOS
                + ") d"
                + donde
                + (porUtilidad
                    ? " order by utilidad_usd desc, fecha desc, id desc"
                    : " order by fecha desc, tipo, numero desc")
                + " limit :limite offset :desplazamiento",
            parametros,
            (rs, n) ->
                new Utilidad(
                    rs.getString("tipo"),
                    rs.getLong("id"),
                    rs.getLong("numero"),
                    rs.getObject("fecha", LocalDate.class),
                    rs.getLong("cliente_id"),
                    rs.getString("cliente_nombre"),
                    rs.getString("moneda"),
                    rs.getBigDecimal("total"),
                    rs.getBigDecimal("costo"),
                    rs.getBigDecimal("utilidad"),
                    rs.getBigDecimal("porcentaje_utilidad"),
                    rs.getBigDecimal("total_usd"),
                    rs.getBigDecimal("costo_usd"),
                    rs.getBigDecimal("utilidad_usd")));
    return new PageImpl<>(filas, pagina, total == null ? 0 : total);
  }

  /** Seriales por estado (RF-141). */
  public Map<String, Long> serialesPorEstado(Long productoId) {
    Map<String, Long> resultado = new LinkedHashMap<>();
    jdbc.query(
        "select estado, count(*) as cantidad from serial"
            + (productoId == null ? "" : " where producto_id = :producto")
            + " group by estado",
        new MapSqlParameterSource("producto", productoId),
        rs -> {
          resultado.put(rs.getString("estado"), rs.getLong("cantidad"));
        });
    return resultado;
  }

  /** Serial con su producto, entrada y salida (RF-141). */
  public record Serial(
      Long id,
      String numero,
      String estado,
      Long productoId,
      String productoCodigo,
      String producto,
      LocalDate fechaEntrada,
      String entrada,
      String salidaTipo,
      Long salidaId,
      String salida,
      LocalDate vencimientoGarantia) {}

  /** Del más reciente al más antiguo. */
  public Page<Serial> seriales(String estado, Long productoId, Pageable pagina) {
    MapSqlParameterSource parametros =
        new MapSqlParameterSource("estado", estado).addValue("producto", productoId);
    String donde =
        " where (cast(:estado as varchar) is null or s.estado = :estado)"
            + " and (cast(:producto as bigint) is null or s.producto_id = :producto)";
    Long total =
        jdbc.queryForObject("select count(*) from serial s" + donde, parametros, Long.class);
    parametros
        .addValue("limite", pagina.getPageSize())
        .addValue("desplazamiento", pagina.getOffset());
    List<Serial> filas =
        jdbc.query(
            "select s.id, s.numero, s.estado, s.producto_id, p.codigo, p.nombre, s.fecha_entrada,"
                + " s.entrada_consecutivo, s.salida_tipo, s.salida_id, s.salida_consecutivo,"
                + " s.vencimiento_garantia from serial s join producto p on p.id = s.producto_id"
                + donde
                + " order by s.fecha_entrada desc, s.id desc limit :limite offset :desplazamiento",
            parametros,
            (rs, n) ->
                new Serial(
                    rs.getLong("id"),
                    rs.getString("numero"),
                    rs.getString("estado"),
                    rs.getLong("producto_id"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getObject("fecha_entrada", LocalDate.class),
                    rs.getString("entrada_consecutivo"),
                    rs.getString("salida_tipo"),
                    rs.getObject("salida_id", Long.class),
                    rs.getString("salida_consecutivo"),
                    rs.getObject("vencimiento_garantia", LocalDate.class)));
    return new PageImpl<>(filas, pagina, total == null ? 0 : total);
  }

  private static MapSqlParameterSource rango(LocalDate desde, LocalDate hasta) {
    return new MapSqlParameterSource("desde", desde).addValue("hasta", hasta);
  }
}
