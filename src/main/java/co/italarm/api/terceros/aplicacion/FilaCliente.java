package co.italarm.api.terceros.aplicacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Fila de la hoja Clientes de la carga inicial (sección 3.10).
 *
 * @param tipo "Instalador" o "Cliente final"
 * @param tipoDocumento "CC" o "NIT"
 */
public record FilaCliente(
    int fila,
    @NotBlank(message = "Falta el tipo de cliente (Instalador o Cliente final).") String tipo,
    @NotBlank(message = "Falta el nombre o razón social.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        String nombre,
    String tipoDocumento,
    @Size(max = 30, message = "El documento admite máximo 30 caracteres.")
        @Pattern(
            regexp = "[0-9.\\s-]*",
            message = "El documento solo admite números, puntos y guion.")
        String numeroDocumento,
    @NotBlank(message = "Falta el teléfono o WhatsApp.")
        @Size(max = 25, message = "El teléfono es demasiado largo.")
        String telefono,
    @Email(message = "El correo no es válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String correo,
    @Size(max = 200, message = "La dirección admite máximo 200 caracteres.") String direccion,
    @Size(max = 80, message = "La ciudad admite máximo 80 caracteres.") String ciudad) {}
