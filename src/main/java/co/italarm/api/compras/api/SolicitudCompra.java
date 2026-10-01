package co.italarm.api.compras.api;

import co.italarm.api.compras.aplicacion.DatosCompra;
import co.italarm.api.shared.dominio.Moneda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Compra a registrar (RF-39).
 *
 * @param fecha fecha de la factura: puede ser anterior a hoy, nunca futura; vacía es hoy (P-19)
 */
public record SolicitudCompra(
    @NotNull(message = "Elige el proveedor.") Long proveedorId,
    @NotBlank(message = "Ingresa el número de factura.")
        @Size(max = 50, message = "El número de factura admite máximo 50 caracteres.")
        String numeroFactura,
    LocalDate fecha,
    @NotNull(message = "Elige la moneda de la factura.") Moneda moneda,
    @NotEmpty(message = "Agrega al menos un producto.")
        @Size(max = 200, message = "Una compra admite máximo 200 productos.")
        List<@Valid @NotNull(message = "Hay una línea vacía.") SolicitudLineaCompra> lineas) {

  DatosCompra aDatos() {
    return new DatosCompra(
        proveedorId,
        numeroFactura,
        fecha,
        moneda,
        lineas.stream().map(SolicitudLineaCompra::aDatos).toList());
  }
}
