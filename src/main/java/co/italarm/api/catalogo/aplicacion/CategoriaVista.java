package co.italarm.api.catalogo.aplicacion;

/** Categoría con la cantidad de productos que tiene. */
public record CategoriaVista(Long id, String nombre, long cantidadProductos, long version) {}
