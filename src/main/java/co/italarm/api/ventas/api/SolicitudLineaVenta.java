package co.italarm.api.ventas.api;

import co.italarm.api.comercial.aplicacion.LineaMaterial;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Producto de una venta.
 *
 * @param cantidad si el producto controla serial, puede ir vacía: es la cantidad de seriales
 * @param seriales los que salen, si el producto controla serial (RF-21)
 * @param precioUnitario vacío = precio sugerido según el tipo de cliente (RN-01)
 */
public record SolicitudLineaVenta(
    @NotNull(message = "Elige el producto.") Long productoId,
    @Positive(message = "La cantidad debe ser mayor que 0.")
        @Digits(integer = 11, fraction = 3, message = "La cantidad no es válida.")
        BigDecimal cantidad,
    @Size(max = 1000, message = "Una línea admite máximo 1000 seriales.")
        List<
                @NotBlank(message = "Hay un serial vacío.")
                @Size(max = 80, message = "Un serial admite máximo 80 caracteres.") String>
            seriales,
    @PositiveOrZero(message = "El precio no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El precio admite máximo 4 decimales.")
        BigDecimal precioUnitario) {

  LineaMaterial aDatos() {
    return new LineaMaterial(productoId, cantidad, seriales, precioUnitario);
  }
}
