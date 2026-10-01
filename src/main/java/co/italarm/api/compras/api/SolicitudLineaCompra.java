package co.italarm.api.compras.api;

import co.italarm.api.compras.aplicacion.DatosCompra;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Producto de una compra.
 *
 * @param costoUnitario en la moneda de la factura, mayor que 0 (P-21)
 * @param seriales uno por unidad si el producto controla serial (RF-20)
 */
public record SolicitudLineaCompra(
    @NotNull(message = "Elige el producto.") Long productoId,
    @NotNull(message = "Ingresa la cantidad.")
        @Positive(message = "La cantidad debe ser mayor que 0.")
        @Digits(integer = 11, fraction = 3, message = "La cantidad no es válida.")
        BigDecimal cantidad,
    @NotNull(message = "Ingresa el costo unitario.")
        @Positive(message = "El costo unitario debe ser mayor que 0.")
        @Digits(integer = 15, fraction = 4, message = "El costo unitario no es válido.")
        BigDecimal costoUnitario,
    @Size(max = 1000, message = "Una línea admite máximo 1000 seriales.")
        List<
                @NotBlank(message = "Hay un serial vacío.")
                @Size(max = 80, message = "Un serial admite máximo 80 caracteres.") String>
            seriales) {

  DatosCompra.Linea aDatos() {
    return new DatosCompra.Linea(productoId, cantidad, costoUnitario, seriales);
  }
}
