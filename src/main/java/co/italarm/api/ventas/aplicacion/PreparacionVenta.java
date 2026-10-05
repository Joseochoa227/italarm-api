package co.italarm.api.ventas.aplicacion;

import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.shared.dominio.CantidadInvalidaException;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ClienteDocumento;
import co.italarm.api.ventas.dominio.CalculoVenta;
import co.italarm.api.ventas.dominio.DatosClienteVenta;
import co.italarm.api.ventas.dominio.PrecioSugerido;
import co.italarm.api.ventas.dominio.VentaInvalidaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pasos comunes de la vista previa y del registro de una venta. */
final class PreparacionVenta {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private PreparacionVenta() {}

  /** Línea con su producto, cantidad, precios y costo en USD. */
  record Linea(
      ProductoValorizado producto,
      BigDecimal cantidad,
      List<String> seriales,
      BigDecimal precioSugerido,
      BigDecimal precioUnitario,
      BigDecimal costoUnitarioUsd) {

    CalculoVenta.Linea calculo() {
      return new CalculoVenta.Linea(cantidad, precioUnitario, costoUnitarioUsd);
    }
  }

  /** Una línea por producto (como en las compras, P-20). */
  static void exigirSinRepetidos(List<DatosVenta.Linea> lineas) {
    if (lineas == null || lineas.isEmpty()) {
      throw new VentaInvalidaException(
          VentaInvalidaException.SIN_LINEAS, "La venta debe tener al menos un producto.");
    }
    Set<Long> vistos = new HashSet<>();
    for (DatosVenta.Linea linea : lineas) {
      if (!vistos.add(linea.productoId())) {
        throw new VentaInvalidaException(
            VentaInvalidaException.PRODUCTO_REPETIDO,
            "Un producto aparece dos veces en la venta. Súmalo en una sola línea.");
      }
    }
  }

  /**
   * Arma cada línea: la cantidad (o la de seriales, RF-21), el precio sugerido según el tipo de
   * cliente (RN-01, P-28) y el precio escrito a mano, si lo hay (RF-78).
   *
   * @param costos costo vigente en USD por producto (RF-68)
   */
  static List<Linea> lineas(
      List<DatosVenta.Linea> datos,
      Map<Long, ProductoValorizado> productos,
      Map<Long, BigDecimal> costos,
      boolean precioInstalador,
      Moneda moneda,
      Tasas tasas) {
    List<Linea> resultado = new ArrayList<>();
    for (DatosVenta.Linea linea : datos) {
      ProductoValorizado producto = productos.get(linea.productoId());
      if (producto == null) {
        throw new VentaInvalidaException(
            "PRODUCTO_NO_EXISTE", "El producto " + linea.productoId() + " no existe.");
      }
      BigDecimal cantidad = cantidad(linea, producto);
      BigDecimal sugerido =
          PrecioSugerido.en(
              precioInstalador ? producto.precioInstalador() : producto.precioClienteFinal(),
              moneda,
              tasas);
      BigDecimal precio = linea.precioUnitario() != null ? linea.precioUnitario() : sugerido;
      CalculoVenta.exigirPrecio(precio);
      resultado.add(
          new Linea(
              producto,
              cantidad,
              linea.seriales() == null ? List.of() : linea.seriales(),
              sugerido,
              precio,
              costos.get(producto.id())));
    }
    return resultado;
  }

  private static BigDecimal cantidad(DatosVenta.Linea linea, ProductoValorizado producto) {
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

  static Tasas conversion(TasasAplicables aplicables) {
    return new Tasas(aplicables.trm(), aplicables.tasaVes());
  }

  static TasasVentaVista tasasVista(TasasAplicables aplicables) {
    return new TasasVentaVista(
        aplicables.trm(), aplicables.fechaTrm(), aplicables.tasaVes(), aplicables.fechaTasaVes());
  }

  static DatosClienteVenta cliente(ClienteDocumento cliente) {
    return new DatosClienteVenta(
        cliente.id(),
        cliente.tipo(),
        cliente.nombre(),
        cliente.documento(),
        cliente.telefono(),
        cliente.direccion(),
        cliente.ciudad());
  }

  /** Aviso si una tasa falta o no es la de hoy (RF-33, CP-12). */
  static List<String> avisos(LocalDate hoy, TasasAplicables aplicables) {
    List<String> avisos = new ArrayList<>();
    if (aplicables.trm() == null) {
      avisos.add("No hay TRM registrada: los valores en pesos quedan vacíos.");
    } else if (!aplicables.fechaTrm().equals(hoy)) {
      avisos.add(
          "Se usa la TRM del "
              + FORMATO_FECHA.format(aplicables.fechaTrm())
              + ". Registra la de hoy para actualizarla.");
    }
    if (aplicables.tasaVes() == null) {
      avisos.add("No hay tasa del bolívar registrada: los valores en bolívares quedan vacíos.");
    } else if (!aplicables.fechaTasaVes().equals(hoy)) {
      avisos.add(
          "Se usa la tasa del bolívar del "
              + FORMATO_FECHA.format(aplicables.fechaTasaVes())
              + ". Regístrala hoy para actualizarla.");
    }
    return avisos;
  }

  /** "Cámara domo × 2 und, Cable UTP × 30 m y 1 más". */
  static String resumen(List<String> lineas) {
    int mostrar = Math.min(2, lineas.size());
    String primeros = String.join(", ", lineas.subList(0, mostrar));
    int resto = lineas.size() - mostrar;
    return resto > 0 ? primeros + " y " + resto + " más" : primeros;
  }

  /** Cantidad sin ceros sobrantes: 12.500 → 12.5; 3.000 → 3. */
  static BigDecimal cantidadVista(BigDecimal valor) {
    if (valor == null) {
      return null;
    }
    BigDecimal sinCeros = valor.stripTrailingZeros();
    return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
  }
}
