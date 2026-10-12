package co.italarm.api.reportes.aplicacion;

import static co.italarm.api.documentos.aplicacion.LibroExcel.dinero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fecha;
import static co.italarm.api.documentos.aplicacion.LibroExcel.numero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.texto;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import co.italarm.api.reportes.dominio.Periodo;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Reportes en Excel (RF-144, P-62): lo mismo que muestra cada reporte, una hoja por bloque. */
@Service
public class ExportacionReportes {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final List<String> COLUMNAS_MONTOS = List.of("USD", "COP", "VES");

  private final ServicioReportes reportes;
  private final ExportadorExcel exportador;

  public ExportacionReportes(ServicioReportes reportes, ExportadorExcel exportador) {
    this.reportes = reportes;
    this.exportador = exportador;
  }

  public ArchivoGenerado periodo(Periodo periodo) {
    ReportePeriodoVista r = reportes.reporte(periodo);
    List<String> filtros = List.of(descripcion(periodo));
    IndicadoresVista i = r.indicadores();
    List<LibroExcel.Hoja> hojas = new ArrayList<>();
    hojas.add(
        new LibroExcel.Hoja(
            "Indicadores",
            "Indicadores del período",
            List.of(
                descripcion(periodo),
                "Valores en USD con las tasas de cada documento; COP y VES con las tasas de hoy."),
            concatenar(List.of("Indicador", "Cantidad"), COLUMNAS_MONTOS, List.of("Porcentaje")),
            List.of(
                indicador("Ventas de material", i.ventas().cantidad(), i.ventas().total(), null),
                indicador(
                    "Instalaciones", i.instalaciones().cantidad(), i.instalaciones().total(), null),
                indicador("Utilidad", null, i.utilidad().total(), i.utilidad().porcentaje()),
                indicador(
                    "Inventario valorizado (hoy)",
                    i.inventario().productos(),
                    i.inventario().valor(),
                    null))));
    hojas.add(
        new LibroExcel.Hoja(
            "Ventas e instalaciones",
            "Ventas e instalaciones por " + ("MES".equals(r.agrupacion()) ? "mes" : "semana"),
            filtros,
            List.of("Período", "Ventas", "Total ventas", "Instalaciones", "Total instalaciones"),
            r.serie().stream()
                .map(
                    b ->
                        fila(
                            texto(b.etiqueta()),
                            numero(BigDecimal.valueOf(b.ventas())),
                            dinero(b.totalVentas()),
                            numero(BigDecimal.valueOf(b.instalaciones())),
                            dinero(b.totalInstalaciones())))
                .toList()));
    hojas.add(hojaTop("Más vendidos", "Productos más vendidos", filtros, r.masVendidos()));
    hojas.add(
        hojaTop(
            "Más instalados", "Productos más usados en instalaciones", filtros, r.masInstalados()));
    hojas.add(
        new LibroExcel.Hoja(
            "Compras",
            "Compras por proveedor",
            filtros,
            List.of("Proveedor", "Compras", "Total"),
            r.comprasPorProveedor().stream()
                .map(
                    c ->
                        fila(
                            texto(c.proveedor()),
                            numero(BigDecimal.valueOf(c.compras())),
                            dinero(c.total())))
                .toList()));
    hojas.add(hojaStockBajo(r.stockBajo()));
    hojas.add(
        new LibroExcel.Hoja(
            "Cotizaciones",
            "Cotizaciones del período",
            List.of(
                descripcion(periodo),
                "Aprobadas: "
                    + (r.cotizaciones().porcentajeAprobadas() == null
                        ? "sin respuestas"
                        : r.cotizaciones().porcentajeAprobadas().toPlainString() + " %")),
            List.of("Estado", "Cantidad"),
            r.cotizaciones().porEstado().stream()
                .map(e -> fila(texto(e.estado()), numero(BigDecimal.valueOf(e.cantidad()))))
                .toList()));
    return exportador.exportar(new LibroExcel("reporte", hojas));
  }

  public ArchivoGenerado utilidad(Periodo periodo, String tipo, boolean porUtilidad) {
    ReporteUtilidadVista r =
        reportes.utilidad(
            periodo, tipo, porUtilidad, PageRequest.of(0, ExportadorExcel.LIMITE_FILAS + 1));
    ExportadorExcel.exigirLimite(r.documentos().contenido().size());
    List<String> filtros = new ArrayList<>(List.of(descripcion(periodo)));
    if (tipo != null) {
      filtros.add("Tipo: " + ("VENTA".equals(tipo) ? "ventas" : "instalaciones"));
    }
    List<List<LibroExcel.Celda>> filas = new ArrayList<>();
    for (UtilidadDocumentoVista d : r.documentos().contenido()) {
      filas.add(
          fila(
              texto("VENTA".equals(d.tipo()) ? "Venta" : "Instalación"),
              texto(d.consecutivo()),
              fecha(d.fecha()),
              texto(d.cliente()),
              dinero(d.total()),
              dinero(d.costo()),
              dinero(d.utilidad()),
              numero(d.porcentajeUtilidad()),
              dinero(d.totalUsd()),
              dinero(d.costoUsd()),
              dinero(d.utilidadUsd())));
    }
    filas.add(
        fila(
            texto("Total"),
            null,
            null,
            null,
            null,
            null,
            null,
            numero(r.porcentajeUtilidad()),
            dinero(r.totalUsd()),
            dinero(r.costoUsd()),
            dinero(r.utilidadUsd())));
    return exportador.exportar(
        new LibroExcel(
            "utilidad",
            List.of(
                new LibroExcel.Hoja(
                    "Utilidad",
                    "Utilidad por venta e instalación",
                    filtros,
                    List.of(
                        "Tipo",
                        "Consecutivo",
                        "Fecha",
                        "Cliente",
                        "Cobrado",
                        "Costo",
                        "Utilidad",
                        "Margen %",
                        "Cobrado USD",
                        "Costo USD",
                        "Utilidad USD"),
                    filas))));
  }

  public ArchivoGenerado seriales(String estado, Long productoId) {
    ReporteSerialesVista r =
        reportes.seriales(estado, productoId, PageRequest.of(0, ExportadorExcel.LIMITE_FILAS + 1));
    ExportadorExcel.exigirLimite(r.seriales().contenido().size());
    List<String> filtros = new ArrayList<>();
    filtros.add(
        "Por estado: "
            + String.join(
                ", ", r.porEstado().stream().map(e -> e.estado() + " " + e.cantidad()).toList()));
    if (estado != null) {
      filtros.add("Estado: " + estado);
    }
    return exportador.exportar(
        new LibroExcel(
            "seriales",
            List.of(
                new LibroExcel.Hoja(
                    "Seriales",
                    "Seriales",
                    filtros,
                    List.of(
                        "Serial",
                        "Estado",
                        "Código",
                        "Producto",
                        "Entrada",
                        "Documento de entrada",
                        "Documento de salida",
                        "Garantía hasta"),
                    r.seriales().contenido().stream()
                        .map(
                            s ->
                                fila(
                                    texto(s.numero()),
                                    texto(s.estado()),
                                    texto(s.codigoProducto()),
                                    texto(s.producto()),
                                    fecha(s.fechaEntrada()),
                                    texto(s.entrada()),
                                    texto(s.salida()),
                                    fecha(s.vencimientoGarantia())))
                        .toList()))));
  }

  private static LibroExcel.Hoja hojaTop(
      String nombre,
      String titulo,
      List<String> filtros,
      List<ReportePeriodoVista.ProductoTop> top) {
    return new LibroExcel.Hoja(
        nombre,
        titulo,
        filtros,
        List.of("Código", "Producto", "Cantidad", "Unidad", "Valor"),
        top.stream()
            .map(
                p ->
                    fila(
                        texto(p.codigo()),
                        texto(p.nombre()),
                        numero(p.cantidad()),
                        texto(p.unidad()),
                        dinero(p.valor())))
            .toList());
  }

  private static LibroExcel.Hoja hojaStockBajo(List<StockBajoVista> productos) {
    return new LibroExcel.Hoja(
        "Stock bajo",
        "Productos por debajo del stock mínimo (hoy)",
        List.of(),
        List.of("Código", "Producto", "Stock", "Mínimo", "Unidad"),
        productos.stream()
            .map(
                p ->
                    fila(
                        texto(p.codigo()),
                        texto(p.nombre()),
                        numero(p.stock()),
                        numero(p.minimo()),
                        texto(p.unidad())))
            .toList());
  }

  private static List<LibroExcel.Celda> indicador(
      String nombre, Long cantidad, MontoEnMonedas monto, BigDecimal porcentaje) {
    return fila(
        texto(nombre),
        cantidad == null ? null : numero(BigDecimal.valueOf(cantidad)),
        celda(monto.usd()),
        celda(monto.cop()),
        celda(monto.ves()),
        numero(porcentaje));
  }

  private static LibroExcel.Celda celda(Dinero dinero) {
    return dinero(dinero);
  }

  static String descripcion(Periodo periodo) {
    return "Período: " + FECHA.format(periodo.desde()) + " al " + FECHA.format(periodo.hasta());
  }

  /** Fila con celdas que pueden ser vacías. */
  static List<LibroExcel.Celda> fila(LibroExcel.Celda... celdas) {
    return Arrays.asList(celdas);
  }

  @SafeVarargs
  private static List<String> concatenar(List<String>... listas) {
    List<String> todas = new ArrayList<>();
    for (List<String> lista : listas) {
      todas.addAll(lista);
    }
    return todas;
  }
}
