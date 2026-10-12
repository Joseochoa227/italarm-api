package co.italarm.api.reportes.aplicacion;

import co.italarm.api.reportes.dominio.Periodo;
import co.italarm.api.reportes.dominio.ReglasReportes;
import co.italarm.api.reportes.dominio.TipoPeriodo;
import co.italarm.api.reportes.infraestructura.ConsultasReportesSql;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reportes por período (sección 3.16): indicadores, gráfico, rankings, compras y cotizaciones. */
@Service
public class ServicioReportes {

  private static final int TOP = 10;
  private static final List<String> ESTADOS_COTIZACION =
      List.of("BORRADOR", "EN_EVALUACION", "APROBADA", "CONVERTIDA", "RECHAZADA", "VENCIDA");
  private static final List<String> ESTADOS_SERIAL =
      List.of("EN_BODEGA", "VENDIDO", "INSTALADO", "DADO_DE_BAJA", "ANULADO");

  private final ConsultasReportesSql consultas;
  private final ServicioTasas tasas;
  private final FechaNegocio fechas;

  public ServicioReportes(
      ConsultasReportesSql consultas, ServicioTasas tasas, FechaNegocio fechas) {
    this.consultas = consultas;
    this.tasas = tasas;
    this.fechas = fechas;
  }

  /**
   * El período pedido (RF-135): {@code tipo} alrededor de {@code fecha} (hoy si falta), o el rango
   * libre {@code desde}–{@code hasta}.
   */
  public Periodo periodo(TipoPeriodo tipo, LocalDate fecha, LocalDate desde, LocalDate hasta) {
    if (tipo == TipoPeriodo.RANGO || (tipo == null && (desde != null || hasta != null))) {
      return Periodo.rango(desde, hasta);
    }
    return Periodo.de(tipo == null ? TipoPeriodo.MES : tipo, fecha == null ? fechas.hoy() : fecha);
  }

  @Transactional(readOnly = true)
  public ReportePeriodoVista reporte(Periodo periodo) {
    Tasas hoy = tasasDeHoy();
    List<ConsultasReportesSql.PorDia> dias = consultas.porDia(periodo.desde(), periodo.hasta());
    List<ReportePeriodoVista.Barra> serie =
        periodo.tramos().stream()
            .map(
                t -> {
                  Predicate<ConsultasReportesSql.PorDia> enTramo =
                      d -> !d.fecha().isBefore(t.desde()) && !d.fecha().isAfter(t.hasta());
                  return new ReportePeriodoVista.Barra(
                      t.etiqueta(),
                      t.desde(),
                      t.hasta(),
                      cantidad(dias, enTramo, "VENTA"),
                      usd(suma(dias, enTramo, "VENTA")),
                      cantidad(dias, enTramo, "INSTALACION"),
                      usd(suma(dias, enTramo, "INSTALACION")));
                })
            .toList();
    List<ConsultasReportesSql.CompraProveedor> compras =
        consultas.comprasPorProveedor(periodo.desde(), periodo.hasta());
    Map<String, Long> porEstado = consultas.cotizacionesPorEstado(periodo.desde(), periodo.hasta());
    long aprobadas =
        porEstado.getOrDefault("APROBADA", 0L) + porEstado.getOrDefault("CONVERTIDA", 0L);
    Periodo anterior = periodo.anterior();
    Periodo siguiente = periodo.siguiente();
    return new ReportePeriodoVista(
        periodo.tipo().name(),
        periodo.desde(),
        periodo.hasta(),
        anterior == null
            ? null
            : new ReportePeriodoVista.PeriodoVista(anterior.desde(), anterior.hasta()),
        siguiente == null
            ? null
            : new ReportePeriodoVista.PeriodoVista(siguiente.desde(), siguiente.hasta()),
        indicadores(periodo.desde(), periodo.hasta(), hoy),
        periodo.agrupacion().name(),
        serie,
        top("VENTA", periodo),
        top("INSTALACION", periodo),
        compras.stream()
            .map(
                c ->
                    new ReportePeriodoVista.CompraProveedor(
                        c.proveedorId(), c.proveedor(), c.compras(), usd(c.totalUsd())))
            .toList(),
        usd(
            compras.stream()
                .map(ConsultasReportesSql.CompraProveedor::totalUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add)),
        stockBajo(null),
        new ReportePeriodoVista.Cotizaciones(
            ESTADOS_COTIZACION.stream()
                .map(e -> new ReportePeriodoVista.PorEstado(e, porEstado.getOrDefault(e, 0L)))
                .toList(),
            porEstado.values().stream().mapToLong(Long::longValue).sum(),
            ReglasReportes.porcentajeAprobadas(
                aprobadas, porEstado.getOrDefault("RECHAZADA", 0L))));
  }

  /** Indicadores de un rango (RF-08, RF-136), con el inventario de hoy (P-58). */
  @Transactional(readOnly = true)
  public IndicadoresVista indicadores(LocalDate desde, LocalDate hasta, Tasas hoy) {
    Map<String, ConsultasReportesSql.Totales> totales = consultas.totales(desde, hasta);
    ConsultasReportesSql.Totales ventas = totales.get("VENTA");
    ConsultasReportesSql.Totales instalaciones = totales.get("INSTALACION");
    BigDecimal cobrado = ventas.totalUsd().add(instalaciones.totalUsd());
    BigDecimal utilidad = ventas.utilidadUsd().add(instalaciones.utilidadUsd());
    ConsultasReportesSql.Inventario inventario = consultas.inventario();
    return new IndicadoresVista(
        documentos(ventas, hoy),
        documentos(instalaciones, hoy),
        new IndicadoresVista.Utilidad(
            hoy.equivalentes(usd(utilidad)), ReglasReportes.porcentaje(utilidad, cobrado)),
        new IndicadoresVista.InventarioValorizado(
            inventario.productos(), hoy.equivalentes(usd(inventario.valorUsd())), fechas.hoy()));
  }

  /** Productos bajo el mínimo; sin límite, todos (RF-10, RF-140). */
  @Transactional(readOnly = true)
  public List<StockBajoVista> stockBajo(Integer limite) {
    return consultas.stockBajo(limite).stream()
        .map(
            p ->
                new StockBajoVista(
                    p.id(),
                    p.codigo(),
                    p.nombre(),
                    p.unidad(),
                    sinCeros(p.stock()),
                    sinCeros(p.minimo()),
                    ReglasReportes.nivelStock(p.stock(), p.minimo())))
        .toList();
  }

  @Transactional(readOnly = true)
  public long contarStockBajo() {
    return consultas.contarStockBajo();
  }

  /** Utilidad por documento (RF-143, P-63). {@code tipo}: VENTA, INSTALACION o vacío. */
  @Transactional(readOnly = true)
  public ReporteUtilidadVista utilidad(
      Periodo periodo, String tipo, boolean porUtilidad, Pageable pagina) {
    Page<ConsultasReportesSql.Utilidad> documentos =
        consultas.utilidad(periodo.desde(), periodo.hasta(), tipo, porUtilidad, pagina);
    Map<String, ConsultasReportesSql.Totales> totales =
        consultas.totales(periodo.desde(), periodo.hasta());
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal costo = BigDecimal.ZERO;
    BigDecimal utilidad = BigDecimal.ZERO;
    for (Map.Entry<String, ConsultasReportesSql.Totales> e : totales.entrySet()) {
      if (tipo == null || tipo.equals(e.getKey())) {
        total = total.add(e.getValue().totalUsd());
        costo = costo.add(e.getValue().costoUsd());
        utilidad = utilidad.add(e.getValue().utilidadUsd());
      }
    }
    return new ReporteUtilidadVista(
        periodo.desde(),
        periodo.hasta(),
        Pagina.de(documentos, ServicioReportes::utilidadVista),
        usd(total),
        usd(costo),
        usd(utilidad),
        ReglasReportes.porcentaje(utilidad, total));
  }

  /** Seriales por estado y listado filtrable (RF-141). */
  @Transactional(readOnly = true)
  public ReporteSerialesVista seriales(String estado, Long productoId, Pageable pagina) {
    Map<String, Long> porEstado = consultas.serialesPorEstado(productoId);
    return new ReporteSerialesVista(
        ESTADOS_SERIAL.stream()
            .map(e -> new ReporteSerialesVista.PorEstado(e, porEstado.getOrDefault(e, 0L)))
            .toList(),
        Pagina.de(
            consultas.seriales(estado, productoId, pagina),
            s ->
                new ReporteSerialesVista.Serial(
                    s.id(),
                    s.numero(),
                    s.estado(),
                    s.productoId(),
                    s.productoCodigo(),
                    s.producto(),
                    s.fechaEntrada(),
                    s.entrada(),
                    s.salidaTipo(),
                    s.salidaId(),
                    s.salida(),
                    s.vencimientoGarantia())));
  }

  /** Tasas de hoy (o las últimas) para los equivalentes en COP y VES (P-57). */
  public Tasas tasasDeHoy() {
    TasasAplicables aplicables = tasas.tasasPara(fechas.hoy());
    return new Tasas(aplicables.trm(), aplicables.tasaVes());
  }

  private List<ReportePeriodoVista.ProductoTop> top(String tipo, Periodo periodo) {
    return consultas.masMovidos(tipo, periodo.desde(), periodo.hasta(), TOP).stream()
        .map(
            p ->
                new ReportePeriodoVista.ProductoTop(
                    p.productoId(),
                    p.codigo(),
                    p.nombre(),
                    sinCeros(p.cantidad()),
                    p.unidad(),
                    usd(p.valorUsd())))
        .toList();
  }

  private static IndicadoresVista.Documentos documentos(
      ConsultasReportesSql.Totales totales, Tasas hoy) {
    return new IndicadoresVista.Documentos(
        totales.cantidad(),
        hoy.equivalentes(usd(totales.totalUsd())),
        totales.porMoneda().stream()
            .map(m -> new Dinero(m.total(), Moneda.valueOf(m.moneda())))
            .toList());
  }

  private static UtilidadDocumentoVista utilidadVista(ConsultasReportesSql.Utilidad u) {
    Moneda moneda = Moneda.valueOf(u.moneda());
    return new UtilidadDocumentoVista(
        u.tipo(),
        u.id(),
        TipoDocumento.valueOf(u.tipo()).consecutivo(u.numero()),
        u.fecha(),
        u.clienteId(),
        u.cliente(),
        new Dinero(u.total(), moneda),
        new Dinero(u.costo(), moneda),
        new Dinero(u.utilidad(), moneda),
        u.porcentajeUtilidad(),
        usd(u.totalUsd()),
        usd(u.costoUsd()),
        usd(u.utilidadUsd()));
  }

  private static long cantidad(
      List<ConsultasReportesSql.PorDia> dias,
      Predicate<ConsultasReportesSql.PorDia> enTramo,
      String tipo) {
    return dias.stream()
        .filter(enTramo)
        .filter(d -> d.tipo().equals(tipo))
        .mapToLong(ConsultasReportesSql.PorDia::cantidad)
        .sum();
  }

  private static BigDecimal suma(
      List<ConsultasReportesSql.PorDia> dias,
      Predicate<ConsultasReportesSql.PorDia> enTramo,
      String tipo) {
    return dias.stream()
        .filter(enTramo)
        .filter(d -> d.tipo().equals(tipo))
        .map(ConsultasReportesSql.PorDia::totalUsd)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  static Dinero usd(BigDecimal monto) {
    return new Dinero(Redondeo.paraAlmacenar(monto == null ? BigDecimal.ZERO : monto), Moneda.USD);
  }

  private static BigDecimal sinCeros(BigDecimal valor) {
    if (valor == null) {
      return null;
    }
    BigDecimal limpio = valor.stripTrailingZeros();
    return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
  }
}
