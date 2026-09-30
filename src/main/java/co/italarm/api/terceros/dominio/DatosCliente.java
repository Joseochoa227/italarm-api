package co.italarm.api.terceros.dominio;

/** Datos editables de un cliente (sección 3.10). */
public record DatosCliente(
    TipoCliente tipo,
    String nombre,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String telefono,
    String correo,
    String direccion,
    String ciudad) {}
