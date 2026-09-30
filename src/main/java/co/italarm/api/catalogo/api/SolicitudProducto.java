package co.italarm.api.catalogo.api;

import co.italarm.api.catalogo.dominio.DatosProducto;
import co.italarm.api.shared.api.Edicion;
import co.italarm.api.shared.dominio.Moneda;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos de un producto (sección 3.3). Los precios viajan como texto decimal. El stock y el costo no
 * se envían: los calcula el sistema.
 */
public record SolicitudProducto(
    @NotBlank(message = "Ingresa el código.")
        @Size(max = 30, message = "El código admite máximo 30 caracteres.")
        @Pattern(
            regexp = "\\s*[A-Za-z0-9][A-Za-z0-9._/-]*\\s*",
            message = "El código solo admite letras, números, punto, guion, guion bajo y barra.")
        String codigo,
    @NotBlank(message = "Ingresa el nombre.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        String nombre,
    @Size(max = 80, message = "La marca admite máximo 80 caracteres.") String marca,
    @Size(max = 80, message = "El modelo admite máximo 80 caracteres.") String modelo,
    @NotNull(message = "Elige la categoría.") Long categoriaId,
    @NotNull(message = "Elige la unidad de medida.") Long unidadMedidaId,
    @NotNull(message = "Indica si el producto controla serial.") Boolean controlaSerial,
    @NotNull(message = "Ingresa el precio instalador.")
        @PositiveOrZero(message = "El precio no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El precio admite máximo 4 decimales.")
        BigDecimal precioInstalador,
    @NotNull(message = "Ingresa el precio cliente final.")
        @PositiveOrZero(message = "El precio no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El precio admite máximo 4 decimales.")
        BigDecimal precioClienteFinal,
    Moneda monedaPrecio,
    @PositiveOrZero(message = "El stock mínimo no puede ser negativo.")
        @Digits(integer = 11, fraction = 3, message = "El stock mínimo no es válido.")
        BigDecimal stockMinimo,
    @Size(max = 2000, message = "La descripción admite máximo 2000 caracteres.") String descripcion,
    @NotNull(groups = Edicion.class, message = "Falta la versión del registro.") Long version) {

  DatosProducto aDatos() {
    return new DatosProducto(
        codigo,
        nombre,
        marca,
        modelo,
        controlaSerial,
        precioInstalador,
        precioClienteFinal,
        monedaPrecio,
        stockMinimo,
        descripcion);
  }
}
