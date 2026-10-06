package co.italarm.api.ventas.api;

import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import co.italarm.api.ventas.aplicacion.DatosVenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Venta a registrar o a previsualizar (RF-97 a RF-100). La fecha siempre es hoy (P-27).
 *
 * @param descuentoTipo PORCENTAJE o VALOR; vacío = sin descuento
 * @param monedasComprobante otras monedas en que el PDF muestra los totales (P-34)
 */
public record SolicitudVenta(
    @NotNull(message = "Elige el cliente.") Long clienteId,
    @NotNull(message = "Elige la moneda de la venta.") Moneda moneda,
    @NotEmpty(message = "Agrega al menos un producto.")
        @Size(max = 200, message = "Una venta admite máximo 200 productos.")
        List<@Valid @NotNull(message = "Hay una línea vacía.") SolicitudLineaVenta> lineas,
    TipoDescuento descuentoTipo,
    @PositiveOrZero(message = "El descuento no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El descuento admite máximo 4 decimales.")
        BigDecimal descuentoValor,
    @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres.")
        String observaciones,
    Set<Moneda> monedasComprobante) {

  DatosVenta aDatos() {
    return new DatosVenta(
        clienteId,
        moneda,
        lineas.stream().map(SolicitudLineaVenta::aDatos).toList(),
        descuentoTipo,
        descuentoValor,
        observaciones,
        monedasComprobante);
  }
}
