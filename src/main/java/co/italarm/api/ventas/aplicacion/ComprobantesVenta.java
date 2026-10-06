package co.italarm.api.ventas.aplicacion;

import co.italarm.api.configuracion.aplicacion.EmpresaDocumentos;
import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.DocumentoPdf;
import co.italarm.api.documentos.aplicacion.FuenteComprobantes;
import co.italarm.api.documentos.aplicacion.GeneradorPdf;
import co.italarm.api.inventario.aplicacion.SerialSalida;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.FormatoDinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDescuento;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.ventas.dominio.DatosClienteVenta;
import co.italarm.api.ventas.dominio.Venta;
import co.italarm.api.ventas.infraestructura.VentaRepositorio;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprobante de venta en PDF (RF-126 a RF-131): datos de la empresa, del cliente guardados con la
 * venta (P-35), ítems con seriales y garantía, totales en la moneda de la venta y en las monedas
 * adicionales elegidas (P-34), y el pie de la configuración.
 */
@Service
public class ComprobantesVenta implements FuenteComprobantes {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final VentaRepositorio ventas;
  private final ServicioMovimientos movimientos;
  private final ServicioConfiguracion configuracion;
  private final GeneradorPdf generador;

  public ComprobantesVenta(
      VentaRepositorio ventas,
      ServicioMovimientos movimientos,
      ServicioConfiguracion configuracion,
      GeneradorPdf generador) {
    this.ventas = ventas;
    this.movimientos = movimientos;
    this.configuracion = configuracion;
    this.generador = generador;
  }

  @Override
  public TipoDocumento tipo() {
    return TipoDocumento.VENTA;
  }

  @Override
  @Transactional(readOnly = true)
  public ArchivoGenerado pdf(Long ventaId) {
    Venta venta =
        ventas
            .findConLineasById(ventaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe."));
    EmpresaDocumentos empresa = configuracion.empresaParaDocumentos();
    Map<Long, List<SerialSalida>> seriales =
        movimientos.serialesDeSalida(ServicioVentas.documento(venta));
    Moneda moneda = venta.getMoneda();
    DatosClienteVenta cliente = venta.getCliente();

    List<DocumentoPdf.Item> items =
        venta.getLineas().stream()
            .map(
                l ->
                    new DocumentoPdf.Item(
                        l.getDescripcion(),
                        cantidad(l.getCantidad()) + " " + l.getUnidad(),
                        dinero(l.getPrecioUnitario(), moneda),
                        dinero(l.getSubtotal(), moneda),
                        seriales.getOrDefault(l.getProductoId(), List.of()).stream()
                            .map(
                                s ->
                                    "Serial "
                                        + s.numero()
                                        + (s.vencimientoGarantia() == null
                                            ? ""
                                            : " · garantía hasta "
                                                + FECHA.format(s.vencimientoGarantia())))
                            .toList()))
            .toList();

    List<DocumentoPdf.Total> totales = new ArrayList<>();
    totales.add(new DocumentoPdf.Total("Subtotal", dinero(venta.getSubtotal(), moneda)));
    if (venta.getDescuento().signum() > 0) {
      String etiqueta =
          venta.getDescuentoTipo() == TipoDescuento.PORCENTAJE
              ? "Descuento (" + cantidad(venta.getDescuentoValor()) + " %)"
              : "Descuento";
      totales.add(new DocumentoPdf.Total(etiqueta, "-" + dinero(venta.getDescuento(), moneda)));
    }
    totales.add(new DocumentoPdf.Total("Total de contado", dinero(venta.getTotal(), moneda)));

    List<String> pie = new ArrayList<>();
    if (empresa.condicionesGarantia() != null) {
      pie.add(
          "Garantía de "
              + empresa.garantiaEquiposMeses()
              + " meses en los equipos con serial. "
              + empresa.condicionesGarantia());
    }
    if (empresa.piePdf() != null) {
      pie.add(empresa.piePdf());
    }
    String observaciones = venta.getObservaciones();
    if (venta.estaAnulada()) {
      observaciones =
          "Venta anulada el "
              + FECHA.format(venta.getAnuladaEn().atZone(FechaNegocio.ZONA))
              + ": "
              + venta.getMotivoAnulacion()
              + (observaciones == null ? "" : ". " + observaciones);
    }

    DocumentoPdf documento =
        new DocumentoPdf(
            new DocumentoPdf.Empresa(
                empresa.nombre(),
                empresa.lema(),
                empresa.nit(),
                empresa.ciudad(),
                empresa.telefono(),
                empresa.correo(),
                empresa.logo()),
            "Comprobante de venta",
            venta.consecutivo(),
            venta.getFecha(),
            null,
            new DocumentoPdf.Cliente(
                cliente.nombre(), cliente.documento(), cliente.direccion(), cliente.telefono()),
            items,
            totales,
            equivalentes(venta),
            observaciones,
            pie,
            venta.estaAnulada() ? "ANULADA" : null);
    return new ArchivoGenerado(
        venta.consecutivo() + ".pdf",
        MediaType.APPLICATION_PDF_VALUE,
        generador.generar(documento));
  }

  /** Total en las monedas adicionales, con la tasa guardada en la venta y su fecha (RF-130). */
  private static List<String> equivalentes(Venta venta) {
    Venta.TasasVenta tasas = venta.getTasas();
    Tasas conversion = new Tasas(tasas.trm(), tasas.tasaVes());
    List<String> lineas = new ArrayList<>();
    for (Moneda destino : venta.getMonedasComprobante()) {
      if (!puede(venta.getMoneda(), destino, tasas)) {
        continue;
      }
      Dinero total =
          new Dinero(
              conversion.desdeUsd(conversion.aUsd(venta.getTotal(), venta.getMoneda()), destino),
              destino);
      List<String> referencias = new ArrayList<>();
      if (venta.getMoneda() == Moneda.COP || destino == Moneda.COP) {
        referencias.add(
            "TRM "
                + FormatoDinero.formatear(new Dinero(tasas.trm(), Moneda.COP))
                + " del "
                + FECHA.format(tasas.fechaTrm()));
      }
      if (venta.getMoneda() == Moneda.VES || destino == Moneda.VES) {
        referencias.add(
            FormatoDinero.formatear(new Dinero(tasas.tasaVes(), Moneda.VES))
                + " por dólar del "
                + FECHA.format(tasas.fechaTasaVes()));
      }
      lineas.add(
          "Total en "
              + destino.name()
              + ": "
              + FormatoDinero.formatear(total)
              + " ("
              + String.join("; ", referencias)
              + ")");
    }
    return lineas;
  }

  private static boolean puede(Moneda origen, Moneda destino, Venta.TasasVenta tasas) {
    boolean conCop = origen == Moneda.COP || destino == Moneda.COP;
    boolean conVes = origen == Moneda.VES || destino == Moneda.VES;
    return (!conCop || tasas.trm() != null) && (!conVes || tasas.tasaVes() != null);
  }

  private static String dinero(BigDecimal monto, Moneda moneda) {
    return FormatoDinero.formatear(new Dinero(monto, moneda));
  }

  /** "12,5"; "3". */
  private static String cantidad(BigDecimal valor) {
    return PreparacionVenta.cantidadVista(valor).toPlainString().replace('.', ',');
  }
}
