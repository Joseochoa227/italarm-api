package co.italarm.api.terceros.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Fila de la hoja Proveedores de la carga inicial (sección 3.6). */
public record FilaProveedor(
    int fila,
    @NotBlank(message = "Falta el nombre o razón social.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        String nombre,
    @Size(max = 30, message = "El NIT admite máximo 30 caracteres.")
        @Pattern(regexp = "[0-9.\\s-]*", message = "El NIT solo admite números, puntos y guion.")
        String nit,
    @Size(max = 25, message = "El teléfono es demasiado largo.") String telefono,
    @Email(message = "El correo no es válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String correo,
    @Size(max = 80, message = "La ciudad admite máximo 80 caracteres.") String ciudad,
    @NotNull(message = "Falta la moneda habitual (USD, COP o VES).") Moneda monedaHabitual) {}
