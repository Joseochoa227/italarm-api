package co.italarm.api.catalogo.aplicacion;

/** Lo que otros módulos necesitan de un producto para moverlo en el inventario. */
public record DatosProductoInventario(
    Long id,
    String codigo,
    String nombre,
    boolean activo,
    boolean controlaSerial,
    boolean admiteDecimales,
    String abreviatura,
    Long categoriaId,
    String categoria,
    String marca) {}
