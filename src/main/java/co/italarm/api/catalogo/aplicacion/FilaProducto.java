package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Fila de la hoja Productos de la carga inicial (RF-149). La categoría va por nombre y la unidad
 * por abreviatura.
 *
 * @param fila número de fila en Excel
 */
public record FilaProducto(
    int fila,
    @NotBlank(message = "Falta el código.")
        @Size(max = 30, message = "El código admite máximo 30 caracteres.")
        @Pattern(
            regexp = "\\s*[A-Za-z0-9][A-Za-z0-9._/-]*\\s*",
            message = "El código solo admite letras, números, punto, guion, guion bajo y barra.")
        String codigo,
    @NotBlank(message = "Falta el nombre.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        String nombre,
    @Size(max = 80, message = "La marca admite máximo 80 caracteres.") String marca,
    @Size(max = 80, message = "El modelo admite máximo 80 caracteres.") String modelo,
    @NotBlank(message = "Falta la categoría.") String categoria,
    @NotBlank(message = "Falta la unidad de medida.") String unidad,
    @NotNull(message = "Indica si controla serial (Sí o No).") Boolean controlaSerial,
    @NotNull(message = "Falta el precio instalador.")
        @PositiveOrZero(message = "El precio instalador no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El precio admite máximo 4 decimales.")
        BigDecimal precioInstalador,
    @NotNull(message = "Falta el precio cliente final.")
        @PositiveOrZero(message = "El precio cliente final no puede ser negativo.")
        @Digits(integer = 15, fraction = 4, message = "El precio admite máximo 4 decimales.")
        BigDecimal precioClienteFinal,
    Moneda monedaPrecio,
    @PositiveOrZero(message = "El stock mínimo no puede ser negativo.")
        @Digits(integer = 11, fraction = 3, message = "El stock mínimo no es válido.")
        BigDecimal stockMinimo,
    @Size(max = 2000, message = "La descripción admite máximo 2000 caracteres.")
        String descripcion) {}
