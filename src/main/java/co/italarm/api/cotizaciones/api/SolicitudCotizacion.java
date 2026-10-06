package co.italarm.api.cotizaciones.api;

import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.cotizaciones.aplicacion.DatosCotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.shared.api.Edicion;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Cotización a registrar, editar o previsualizar (RF-80 a RF-84). La fecha siempre es hoy (P-49).
 * {@code version} solo se envía al editar.
 *
 * @param validezDias 8, 15 o 30; vacío = el de Configuración (RF-83)
 * @param manoDeObra solo en las de instalación, en la moneda de la cotización
 * @param descripcion descripción del trabajo; obligatoria en las de instalación
 * @param descuentoTipo PORCENTAJE o VALOR; vacío = sin descuento
 * @param monedasComprobante otras monedas en que el PDF muestra los totales
 */
public record SolicitudCotizacion(
    @NotNull(message = "Elige el tipo de cotización.") TipoCotizacion tipo,
    @NotNull(message = "Elige el cliente.") Long clienteId,
    Integer validezDias,
    @NotNull(message = "Elige la moneda de la cotización.") Moneda moneda,
    @Size(max = 200, message = "Una cotización admite máximo 200 productos.")
        List<@Valid @NotNull(message = "Hay una línea vacía.") SolicitudLineaCotizacion> lineas,
    @PositiveOrZero(message = "La mano de obra no puede ser negativa.")
        @Digits(integer = 15, fraction = 4, message = "La mano de obra admite máximo 4 decimales.")
        BigDecimal manoDeObra,
    @Size(max = 2000, message = "La descripción admite máximo 2000 caracteres.") String descripcion,
    TipoDescuento descuentoTipo,
    @PositiveOrZero(message = "El descuento no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El descuento admite máximo 4 decimales.")
        BigDecimal descuentoValor,
    @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres.")
        String observaciones,
    Set<Moneda> monedasComprobante,
    @NotNull(groups = Edicion.class, message = "Falta la versión del registro.") Long version) {

  DatosCotizacion aDatos() {
    return new DatosCotizacion(
        tipo,
        clienteId,
        validezDias,
        moneda,
        lineas == null ? List.of() : lineas.stream().map(SolicitudLineaCotizacion::aDatos).toList(),
        manoDeObra,
        descripcion,
        descuentoTipo,
        descuentoValor,
        observaciones,
        monedasComprobante);
  }

  /**
   * Producto cotizado (sin seriales: se eligen al convertir).
   *
   * @param precioUnitario vacío = precio sugerido según el tipo de cliente (RF-84)
   */
  public record SolicitudLineaCotizacion(
      @NotNull(message = "Elige el producto.") Long productoId,
      @NotNull(message = "Ingresa la cantidad.")
          @Positive(message = "La cantidad debe ser mayor que 0.")
          @Digits(integer = 11, fraction = 3, message = "La cantidad no es válida.")
          BigDecimal cantidad,
      @PositiveOrZero(message = "El precio no puede ser negativo.")
          @Digits(integer = 15, fraction = 4, message = "El precio admite máximo 4 decimales.")
          BigDecimal precioUnitario) {

    LineaMaterial aDatos() {
      return new LineaMaterial(productoId, cantidad, List.of(), precioUnitario);
    }
  }
}
