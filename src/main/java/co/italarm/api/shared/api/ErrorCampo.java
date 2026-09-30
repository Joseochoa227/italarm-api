package co.italarm.api.shared.api;

/** Error de validación de un campo, dentro de la propiedad {@code errores} del problema. */
public record ErrorCampo(String campo, String mensaje) {}
