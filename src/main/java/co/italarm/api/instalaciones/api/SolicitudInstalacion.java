package co.italarm.api.instalaciones.api;

import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.instalaciones.aplicacion.DatosInstalacion;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Instalación a registrar o a previsualizar (RF-107 a RF-119).
 *
 * @param fecha puede ser anterior a hoy, nunca futura; vacía es hoy (P-38)
 * @param direccion vacía = la del cliente
 * @param tecnicos ids de usuarios activos (`GET /usuarios/tecnicos`), al menos uno (P-37)
 * @param manoDeObra valor de la mano de obra en la moneda del cobro (RF-118)
 * @param garantiaManoObraMeses 1 a 3; vacío = el de Configuración (P-39)
 * @param condicionesGarantia vacías = las de Configuración (RF-115)
 */
public record SolicitudInstalacion(
    @NotNull(message = "Elige el cliente.") Long clienteId,
    LocalDate fecha,
    @Size(max = 200, message = "La dirección admite máximo 200 caracteres.") String direccion,
    @NotBlank(message = "Describe el trabajo realizado.")
        @Size(max = 2000, message = "La descripción admite máximo 2000 caracteres.")
        String descripcion,
    @NotEmpty(message = "Elige al menos un técnico.")
        @Size(max = 10, message = "Máximo 10 técnicos.")
        List<@NotNull(message = "Hay un técnico vacío.") Long> tecnicos,
    @NotNull(message = "Elige la moneda del cobro.") Moneda moneda,
    @Size(max = 200, message = "Una instalación admite máximo 200 productos.")
        List<@Valid @NotNull(message = "Hay una línea vacía.") SolicitudLineaMaterial> lineas,
    @PositiveOrZero(message = "La mano de obra no puede ser negativa.")
        @Digits(integer = 15, fraction = 4, message = "La mano de obra admite máximo 4 decimales.")
        BigDecimal manoDeObra,
    TipoDescuento descuentoTipo,
    @PositiveOrZero(message = "El descuento no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El descuento admite máximo 4 decimales.")
        BigDecimal descuentoValor,
    @Min(value = 1, message = "La garantía de mano de obra es de 1 a 3 meses.")
        @Max(value = 3, message = "La garantía de mano de obra es de 1 a 3 meses.")
        Integer garantiaManoObraMeses,
    @Size(max = 2000, message = "Las condiciones admiten máximo 2000 caracteres.")
        String condicionesGarantia,
    @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres.")
        String observaciones,
    Set<Moneda> monedasComprobante) {

  DatosInstalacion aDatos() {
    return new DatosInstalacion(
        clienteId,
        fecha,
        direccion,
        descripcion,
        tecnicos,
        moneda,
        lineas == null ? List.of() : lineas.stream().map(SolicitudLineaMaterial::aDatos).toList(),
        manoDeObra,
        descuentoTipo,
        descuentoValor,
        garantiaManoObraMeses,
        condicionesGarantia,
        observaciones,
        monedasComprobante);
  }

  /** Material usado (RF-108). */
  public record SolicitudLineaMaterial(
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
}
