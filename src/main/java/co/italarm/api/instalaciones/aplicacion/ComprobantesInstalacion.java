package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.configuracion.aplicacion.EmpresaDocumentos;
import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.DocumentoPdf;
import co.italarm.api.documentos.aplicacion.FuenteComprobantes;
import co.italarm.api.documentos.aplicacion.GeneradorPdf;
import co.italarm.api.instalaciones.dominio.Instalacion;
import co.italarm.api.instalaciones.infraestructura.InstalacionRepositorio;
import co.italarm.api.inventario.aplicacion.SerialSalida;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.dominio.CopiaCliente;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.FormatoDinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDescuento;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprobante de instalación en PDF (RF-126 a RF-132): trabajo y técnicos, material con seriales,
 * la línea de mano de obra, totales y las garantías con su vencimiento y condiciones.
 */
@Service
public class ComprobantesInstalacion implements FuenteComprobantes {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final InstalacionRepositorio instalaciones;
  private final ServicioMovimientos movimientos;
  private final ServicioConfiguracion configuracion;
  private final ConsultaUsuarios usuarios;
  private final GeneradorPdf generador;

  public ComprobantesInstalacion(
      InstalacionRepositorio instalaciones,
      ServicioMovimientos movimientos,
      ServicioConfiguracion configuracion,
      ConsultaUsuarios usuarios,
      GeneradorPdf generador) {
    this.instalaciones = instalaciones;
    this.movimientos = movimientos;
    this.configuracion = configuracion;
    this.usuarios = usuarios;
    this.generador = generador;
  }

  @Override
  public TipoDocumento tipo() {
    return TipoDocumento.INSTALACION;
  }

  @Override
  @Transactional(readOnly = true)
  public ArchivoGenerado pdf(Long instalacionId) {
    Instalacion instalacion =
        instalaciones
            .findConLineasById(instalacionId)
            .orElseThrow(() -> new RecursoNoEncontradoException("La instalación no existe."));
    EmpresaDocumentos empresa = configuracion.empresaParaDocumentos();
    Map<Long, List<SerialSalida>> seriales =
        movimientos.serialesDeSalida(ServicioInstalaciones.documento(instalacion));
    Moneda moneda = instalacion.getMoneda();
    ResumenDocumento resumen = instalacion.getResumen();
    CopiaCliente cliente = instalacion.getCliente();

    List<DocumentoPdf.Item> items = new ArrayList<>();
    instalacion
        .getLineas()
        .forEach(
            l ->
                items.add(
                    new DocumentoPdf.Item(
                        l.getDescripcion(),
                        cantidad(l.getCantidad()) + " " + l.getUnidad(),
                        dinero(l.getPrecioUnitario(), moneda),
                        dinero(l.getSubtotal(), moneda),
                        seriales.getOrDefault(l.getProductoId(), List.of()).stream()
                            .map(s -> "Serial " + s.numero())
                            .toList())));
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
      String etiqueta =
          instalacion.getDescuentoTipo() == TipoDescuento.PORCENTAJE
              ? "Descuento (" + cantidad(instalacion.getDescuentoValor()) + " %)"
              : "Descuento";
      totales.add(new DocumentoPdf.Total(etiqueta, "-" + dinero(resumen.descuento(), moneda)));
    }
    totales.add(new DocumentoPdf.Total("Total de contado", dinero(resumen.total(), moneda)));

    List<String> pie = new ArrayList<>();
    pie.add(
        "Garantía de mano de obra: "
            + instalacion.getGarantiaManoObraMeses()
            + (instalacion.getGarantiaManoObraMeses() == 1 ? " mes" : " meses")
            + ", hasta el "
            + FECHA.format(instalacion.getVenceManoObra())
            + ".");
    if (!seriales.isEmpty()) {
      pie.add(
          "Garantía de los equipos con serial: hasta el "
              + FECHA.format(instalacion.getVenceEquipos())
              + ".");
    }
    pie.add("Condiciones de la garantía: " + instalacion.getCondicionesGarantia());
    if (empresa.piePdf() != null) {
      pie.add(empresa.piePdf());
    }

    String tecnicos =
        usuarios.nombres(instalacion.getTecnicos()).values().stream()
            .sorted()
            .collect(Collectors.joining(", "));
    StringBuilder observaciones =
        new StringBuilder("Trabajo realizado: ").append(instalacion.getDescripcion());
    observaciones.append("\nTécnicos: ").append(tecnicos);
    if (instalacion.getObservaciones() != null) {
      observaciones.append("\n").append(instalacion.getObservaciones());
    }
    if (instalacion.estaAnulada()) {
      observaciones
          .append("\nInstalación anulada el ")
          .append(FECHA.format(instalacion.getAnuladaEn().atZone(FechaNegocio.ZONA)))
          .append(": ")
          .append(instalacion.getMotivoAnulacion());
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
            "Comprobante de instalación",
            instalacion.consecutivo(),
            instalacion.getFecha(),
            null,
            new DocumentoPdf.Cliente(
                cliente.nombre(),
                cliente.documento(),
                instalacion.getDireccion(),
                cliente.telefono()),
            items,
            totales,
            equivalentes(instalacion),
            observaciones.toString(),
            pie,
            instalacion.estaAnulada() ? "ANULADA" : null);
    return new ArchivoGenerado(
        instalacion.consecutivo() + ".pdf",
        MediaType.APPLICATION_PDF_VALUE,
        generador.generar(documento));
  }

  /** Total en las monedas adicionales con la tasa guardada y su fecha (RF-130). */
  private static List<String> equivalentes(Instalacion instalacion) {
    Instalacion.TasasInstalacion tasas = instalacion.getTasas();
    Tasas conversion = new Tasas(tasas.trm(), tasas.tasaVes());
    Moneda origen = instalacion.getMoneda();
    BigDecimal total = instalacion.getResumen().total();
    List<String> lineas = new ArrayList<>();
    for (Moneda destino : instalacion.getMonedasComprobante()) {
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
