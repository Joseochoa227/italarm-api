package co.italarm.api.terceros.dominio;

import co.italarm.api.shared.dominio.Moneda;

/** Datos editables de un proveedor (sección 3.6). */
public record DatosProveedor(
    String nombre,
    String nit,
    String telefono,
    String correo,
    String ciudad,
    Moneda monedaHabitual) {}
