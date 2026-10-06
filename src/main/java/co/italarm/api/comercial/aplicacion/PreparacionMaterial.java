package co.italarm.api.comercial.aplicacion;

import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.CantidadInvalidaException;
import co.italarm.api.shared.dominio.CopiaCliente;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.PrecioSugerido;
import co.italarm.api.shared.dominio.ProductoNoExisteException;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ClienteDocumento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pasos comunes de los documentos que venden material (ventas, instalaciones y, desde la Fase 5,
 * cotizaciones): precio sugerido, cantidades, tasas, avisos y vistas.
 */
public final class PreparacionMaterial {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private PreparacionMaterial() {}

  /** Si algún producto aparece en dos líneas (cada documento lanza su propio error). */
  public static boolean hayRepetidos(List<LineaMaterial> lineas) {
    Set<Long> vistos = new HashSet<>();
    return lineas.stream().anyMatch(linea -> !vistos.add(linea.productoId()));
  }

  /**
   * Arma cada línea: la cantidad (o la de seriales, RF-21), el precio sugerido según el tipo de
   * cliente (RN-01, P-28) y el precio escrito a mano, si lo hay (RF-78).
   *
   * @param costos costo vigente en USD por producto (RF-68)
   */
  public static List<MaterialPreparado> lineas(
      List<LineaMaterial> datos,
      Map<Long, ProductoValorizado> productos,
      Map<Long, BigDecimal> costos,
      boolean precioInstalador,
      Moneda moneda,
      Tasas tasas) {
    List<MaterialPreparado> resultado = new ArrayList<>();
    for (LineaMaterial linea : datos) {
      ProductoValorizado producto = productos.get(linea.productoId());
      if (producto == null) {
        throw new ProductoNoExisteException(linea.productoId());
      }
      BigDecimal cantidad = cantidad(linea, producto);
      BigDecimal sugerido =
          PrecioSugerido.en(
              precioInstalador ? producto.precioInstalador() : producto.precioClienteFinal(),
              moneda,
              tasas);
      BigDecimal precio = linea.precioUnitario() != null ? linea.precioUnitario() : sugerido;
      CalculoDocumento.exigirPrecio(precio);
      resultado.add(
          new MaterialPreparado(
              producto,
              cantidad,
              linea.seriales() == null ? List.of() : linea.seriales(),
              sugerido,
              precio,
              costos.get(producto.id())));
    }
    return resultado;
  }

  private static BigDecimal cantidad(LineaMaterial linea, ProductoValorizado producto) {
    if (linea.cantidad() != null) {
      if (linea.cantidad().signum() <= 0) {
        throw new CantidadInvalidaException(
            producto.nombre() + ": la cantidad debe ser mayor que 0.");
      }
      return linea.cantidad();
    }
    if (producto.controlaSerial() && linea.seriales() != null && !linea.seriales().isEmpty()) {
      return BigDecimal.valueOf(linea.seriales().size());
    }
    throw new CantidadInvalidaException(
        producto.nombre()
            + (producto.controlaSerial()
                ? ": elige los seriales que salen."
                : ": ingresa la cantidad."));
  }

  public static Tasas conversion(TasasAplicables aplicables) {
    return new Tasas(aplicables.trm(), aplicables.tasaVes());
  }

  public static TasasDocumentoVista tasasVista(TasasAplicables aplicables) {
    return new TasasDocumentoVista(
        aplicables.trm(), aplicables.fechaTrm(), aplicables.tasaVes(), aplicables.fechaTasaVes());
  }

  public static CopiaCliente copia(ClienteDocumento cliente) {
    return new CopiaCliente(
        cliente.id(),
        cliente.tipo(),
        cliente.nombre(),
        cliente.documento(),
        cliente.telefono(),
        cliente.direccion(),
        cliente.ciudad());
  }

  public static ClienteDocumentoVista clienteVista(ClienteDocumento cliente) {
    return new ClienteDocumentoVista(
        cliente.id(),
        cliente.tipo(),
        cliente.nombre(),
        cliente.documento(),
        cliente.telefono(),
        cliente.direccion(),
        cliente.ciudad(),
        cliente.precioAplicadoDescripcion());
  }

  public static ClienteDocumentoVista clienteVista(CopiaCliente cliente) {
    return new ClienteDocumentoVista(
        cliente.id(),
        cliente.tipo(),
        cliente.nombre(),
        cliente.documento(),
        cliente.telefono(),
        cliente.direccion(),
        cliente.ciudad(),
        null);
  }

  public static ResumenCobroVista resumenVista(
      ResumenDocumento resumen, Moneda moneda, Tasas conversion) {
    return new ResumenCobroVista(
        conversion.equivalentes(new Dinero(resumen.material(), moneda)),
        conversion.equivalentes(new Dinero(resumen.manoDeObra(), moneda)),
        conversion.equivalentes(new Dinero(resumen.subtotal(), moneda)),
        conversion.equivalentes(new Dinero(resumen.descuento(), moneda)),
        conversion.equivalentes(new Dinero(resumen.total(), moneda)),
        conversion.equivalentes(new Dinero(resumen.costo(), moneda)),
        conversion.equivalentes(new Dinero(resumen.utilidad(), moneda)),
        resumen.porcentajeUtilidad());
  }

  /**
   * Aviso si una tasa falta o no es la de la fecha del documento (RF-33, CP-12).
   *
   * @param fecha fecha del documento
   */
  public static List<String> avisos(LocalDate fecha, TasasAplicables aplicables) {
    List<String> avisos = new ArrayList<>();
    if (aplicables.trm() == null) {
      avisos.add("No hay TRM registrada: los valores en pesos quedan vacíos.");
    } else if (!aplicables.fechaTrm().equals(fecha)) {
      avisos.add(
          "Se usa la TRM del "
              + FORMATO_FECHA.format(aplicables.fechaTrm())
              + ". Registra la de hoy para actualizarla.");
    }
    if (aplicables.tasaVes() == null) {
      avisos.add("No hay tasa del bolívar registrada: los valores en bolívares quedan vacíos.");
    } else if (!aplicables.fechaTasaVes().equals(fecha)) {
      avisos.add(
          "Se usa la tasa del bolívar del "
              + FORMATO_FECHA.format(aplicables.fechaTasaVes())
              + ". Regístrala hoy para actualizarla.");
    }
    return avisos;
  }

  /** "Cámara domo × 2 und, Cable UTP × 30 m y 1 más". */
  public static String resumenTexto(List<String> lineas) {
    int mostrar = Math.min(2, lineas.size());
    String primeros = String.join(", ", lineas.subList(0, mostrar));
    int resto = lineas.size() - mostrar;
    return resto > 0 ? primeros + " y " + resto + " más" : primeros;
  }

  /** "Cámara domo × 2 und". */
  public static String textoLinea(String descripcion, BigDecimal cantidad, String unidad) {
    return descripcion + " × " + cantidadVista(cantidad).toPlainString() + " " + unidad;
  }

  /** Cantidad sin ceros sobrantes: 12.500 → 12.5; 3.000 → 3. */
  public static BigDecimal cantidadVista(BigDecimal valor) {
    if (valor == null) {
      return null;
    }
    BigDecimal sinCeros = valor.stripTrailingZeros();
    return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
  }
}
