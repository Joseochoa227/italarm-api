package co.italarm.api.comercial.aplicacion;

import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.shared.dominio.CalculoDocumento;
import java.math.BigDecimal;
import java.util.List;

/** Línea con su producto, cantidad, precios y costo en USD, lista para calcular y guardar. */
public record MaterialPreparado(
    ProductoValorizado producto,
    BigDecimal cantidad,
    List<String> seriales,
    BigDecimal precioSugerido,
    BigDecimal precioUnitario,
    BigDecimal costoUnitarioUsd) {

  public CalculoDocumento.Linea calculo() {
    return new CalculoDocumento.Linea(cantidad, precioUnitario, costoUnitarioUsd);
  }
}
