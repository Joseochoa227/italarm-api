package co.italarm.api.compras.api;

import co.italarm.api.compras.aplicacion.DatosCompra;
import co.italarm.api.shared.dominio.Moneda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Datos para la vista previa: fecha, moneda y líneas. Los seriales no se revisan aquí. */
public record SolicitudVistaPreviaCompra(
    LocalDate fecha,
    @NotNull(message = "Elige la moneda de la factura.") Moneda moneda,
    @NotEmpty(message = "Agrega al menos un producto.")
        @Size(max = 200, message = "Una compra admite máximo 200 productos.")
        List<@Valid @NotNull(message = "Hay una línea vacía.") SolicitudLineaCompra> lineas) {

  List<DatosCompra.Linea> aLineas() {
    return lineas.stream().map(SolicitudLineaCompra::aDatos).toList();
  }
}
