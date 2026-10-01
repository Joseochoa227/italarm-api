package co.italarm.api.inventario.api;

import co.italarm.api.inventario.aplicacion.DatosAjuste;
import co.italarm.api.inventario.dominio.MotivoAjuste;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Ajuste de un producto (RF-58).
 *
 * @param cantidad positiva = entrada; negativa = salida
 * @param costoUnitarioUsd obligatorio solo en una entrada de un producto que nunca tuvo costo
 *     (P-25); en los demás casos se usa el costo vigente
 * @param seriales en una entrada, los nuevos; en una salida, los que se dan de baja
 */
public record SolicitudAjuste(
    @NotNull(message = "Elige el producto.") Long productoId,
    @NotNull(message = "Elige el motivo.") MotivoAjuste motivo,
    @Size(max = 300, message = "La descripción admite máximo 300 caracteres.") String descripcion,
    @NotNull(message = "Ingresa la cantidad.")
        @Digits(integer = 11, fraction = 3, message = "La cantidad no es válida.")
        BigDecimal cantidad,
    @Positive(message = "El costo unitario debe ser mayor que 0.")
        @Digits(integer = 15, fraction = 4, message = "El costo unitario no es válido.")
        BigDecimal costoUnitarioUsd,
    @Size(max = 1000, message = "Un ajuste admite máximo 1000 seriales.")
        List<
                @NotBlank(message = "Hay un serial vacío.")
                @Size(max = 80, message = "Un serial admite máximo 80 caracteres.") String>
            seriales) {

  DatosAjuste aDatos() {
    return new DatosAjuste(productoId, motivo, descripcion, cantidad, costoUnitarioUsd, seriales);
  }
}
