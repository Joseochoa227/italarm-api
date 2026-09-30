package co.italarm.api.shared.seguridad;

/** Usuario dueño del token de la petición en curso. */
public record UsuarioAutenticado(Long usuarioId, Long sesionId, String nombre, String correo) {}
