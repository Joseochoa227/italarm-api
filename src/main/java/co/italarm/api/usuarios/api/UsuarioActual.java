package co.italarm.api.usuarios.api;

/** Usuario de la sesión actual. */
public record UsuarioActual(Long id, String nombre, String correo) {}
