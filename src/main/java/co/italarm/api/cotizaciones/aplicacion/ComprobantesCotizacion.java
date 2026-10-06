package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.configuracion.aplicacion.EmpresaDocumentos;
import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import co.italarm.api.cotizaciones.dominio.Cotizacion;
import co.italarm.api.cotizaciones.dominio.EstadoCotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.cotizaciones.infraestructura.CotizacionRepositorio;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.DocumentoPdf;
import co.italarm.api.documentos.aplicacion.FuenteComprobantes;
import co.italarm.api.documentos.aplicacion.GeneradorPdf;
import co.italarm.api.shared.dominio.CopiaCliente;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.FormatoDinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cotización en PDF (RF-126 a RF-131): fecha y "Válida hasta", datos del cliente, ítems con la mano
 * de obra como una línea más, descuento, totales en la moneda de la cotización y en las monedas
 * adicionales elegidas, condiciones y pie de la configuración.
 */
@Service
public class ComprobantesCotizacion implements FuenteComprobantes {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final CotizacionRepositorio cotizaciones;
  private final ServicioConfiguracion configuracion;
  private final GeneradorPdf generador;

  public ComprobantesCotizacion(
      CotizacionRepositorio cotizaciones,
      ServicioConfiguracion configuracion,
      GeneradorPdf generador) {
    this.cotizaciones = cotizaciones;
    this.configuracion = configuracion;
    this.generador = generador;
  }

  @Override
  public TipoDocumento tipo() {
    return TipoDocumento.COTIZACION;
  }

  @Override
  @Transactional(readOnly = true)
  public ArchivoGenerado pdf(Long cotizacionId) {
    Cotizacion cotizacion =
        cotizaciones
            .findConLineasById(cotizacionId)
            .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
    EmpresaDocumentos empresa = configuracion.empresaParaDocumentos();
    Moneda moneda = cotizacion.getMoneda();
    ResumenDocumento resumen = cotizacion.getResumen();
    CopiaCliente cliente = cotizacion.getCliente();

    List<DocumentoPdf.Item> items = new ArrayList<>();
    cotizacion
        .getLineas()
        .forEach(
            l ->
                items.add(
                    new DocumentoPdf.Item(
                        l.getDescripcion(),
                        cantidad(l.getCantidad()) + " " + l.getUnidad(),
                        dinero(l.getPrecioUnitario(), moneda),
                        dinero(l.getSubtotal(), moneda),
                        List.of())));
    if (resumen.manoDeObra().signum() > 0) {
      items.add(
          new DocumentoPdf.Item(
              "Mano de obra · instalación y configuración",
              "1",
              dinero(resumen.manoDeObra(), moneda),
              dinero(resumen.manoDeObra(), moneda),
              List.of()));
    }

    List<DocumentoPdf.Total> totales = new ArrayList<>();
    totales.add(new DocumentoPdf.Total("Subtotal", dinero(resumen.subtotal(), moneda)));
    if (resumen.descuento().signum() > 0) {
      totales.add(
          new DocumentoPdf.Total(
              ServicioCotizaciones.etiquetaDescuento(
                  cotizacion.getDescuentoTipo(), cotizacion.getDescuentoValor()),
              "-" + dinero(resumen.descuento(), moneda)));
    }
    totales.add(new DocumentoPdf.Total("Total de contado", dinero(resumen.total(), moneda)));

    List<String> pie = new ArrayList<>();
    pie.add(
        "Cotización válida por "
            + cotizacion.getValidezDias()
            + " días, hasta el "
            + FECHA.format(cotizacion.getVence())
            + ".");
    String garantia =
        cotizacion.getTipo() == TipoCotizacion.INSTALACION
            ? "Garantía de "
                + empresa.garantiaEquiposMeses()
                + " meses en los equipos con serial y de "
                + empresa.garantiaManoObraMeses()
                + " meses en la mano de obra."
            : "Garantía de " + empresa.garantiaEquiposMeses() + " meses en los equipos con serial.";
    pie.add(
        empresa.condicionesGarantia() == null
            ? garantia
            : garantia + " " + empresa.condicionesGarantia());
    if (empresa.piePdf() != null) {
      pie.add(empresa.piePdf());
    }

    StringBuilder observaciones = new StringBuilder();
    if (cotizacion.getDescripcion() != null) {
      observaciones.append("Trabajo a realizar: ").append(cotizacion.getDescripcion());
    }
    if (cotizacion.getObservaciones() != null) {
      if (!observaciones.isEmpty()) {
        observaciones.append("\n");
      }
      observaciones.append(cotizacion.getObservaciones());
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
            cotizacion.getTipo() == TipoCotizacion.INSTALACION
                ? "Cotización de instalación"
                : "Cotización",
            cotizacion.consecutivo(),
            cotizacion.getFecha(),
            cotizacion.getVence(),
            new DocumentoPdf.Cliente(
                cliente.nombre(), cliente.documento(), cliente.direccion(), cliente.telefono()),
            items,
            totales,
            equivalentes(cotizacion),
            observaciones.isEmpty() ? null : observaciones.toString(),
            pie,
            marca(cotizacion.getEstado()));
    return new ArchivoGenerado(
        cotizacion.consecutivo().replace(' ', '-') + ".pdf",
        MediaType.APPLICATION_PDF_VALUE,
        generador.generar(documento));
  }

  private static String marca(EstadoCotizacion estado) {
    return switch (estado) {
      case VENCIDA -> "VENCIDA";
      case RECHAZADA -> "RECHAZADA";
      default -> null;
    };
  }

  /** Total en las monedas adicionales con la tasa de la cotización y su fecha (RF-130). */
  private static List<String> equivalentes(Cotizacion cotizacion) {
    Cotizacion.TasasCotizacion tasas = cotizacion.getTasas();
    Tasas conversion = new Tasas(tasas.trm(), tasas.tasaVes());
    Moneda origen = cotizacion.getMoneda();
    BigDecimal total = cotizacion.getResumen().total();
    List<String> lineas = new ArrayList<>();
    for (Moneda destino : cotizacion.getMonedasComprobante()) {
      boolean conCop = origen == Moneda.COP || destino == Moneda.COP;
      boolean conVes = origen == Moneda.VES || destino == Moneda.VES;
      if ((conCop && tasas.trm() == null) || (conVes && tasas.tasaVes() == null)) {
        continue;
      }
      Dinero convertido =
          new Dinero(conversion.desdeUsd(conversion.aUsd(total, origen), destino), destino);
      List<String> referencias = new ArrayList<>();
      if (conCop) {
        referencias.add(
            "TRM "
                + FormatoDinero.formatear(new Dinero(tasas.trm(), Moneda.COP))
                + " del "
                + FECHA.format(tasas.fechaTrm()));
      }
      if (conVes) {
        referencias.add(
            FormatoDinero.formatear(new Dinero(tasas.tasaVes(), Moneda.VES))
                + " por dólar del "
                + FECHA.format(tasas.fechaTasaVes()));
      }
      lineas.add(
          "Total en "
              + destino.name()
              + ": "
              + FormatoDinero.formatear(convertido)
              + " ("
              + String.join("; ", referencias)
              + ")");
    }
    return lineas;
  }

  private static String dinero(BigDecimal monto, Moneda moneda) {
    return FormatoDinero.formatear(new Dinero(monto, moneda));
  }

  private static String cantidad(BigDecimal valor) {
    return PreparacionMaterial.cantidadVista(valor).toPlainString().replace('.', ',');
  }
}
