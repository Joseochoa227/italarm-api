package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.catalogo.dominio.Categoria;
import co.italarm.api.catalogo.dominio.CategoriaConProductosException;
import co.italarm.api.catalogo.dominio.CategoriaDuplicadaException;
import co.italarm.api.catalogo.infraestructura.CategoriaRepositorio;
import co.italarm.api.catalogo.infraestructura.ProductoRepositorio;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.Textos;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Categorías de productos (RF-15). */
@Service
public class ServicioCategorias {

  private final CategoriaRepositorio categorias;
  private final ProductoRepositorio productos;

  public ServicioCategorias(CategoriaRepositorio categorias, ProductoRepositorio productos) {
    this.categorias = categorias;
    this.productos = productos;
  }

  @Transactional(readOnly = true)
  public List<CategoriaVista> listar() {
    Map<Long, Long> cantidades = new HashMap<>();
    for (Object[] fila : productos.contarPorCategoria()) {
      cantidades.put((Long) fila[0], (Long) fila[1]);
    }
    return categorias.findAllByOrderByNombreAsc().stream()
        .map(c -> vista(c, cantidades.getOrDefault(c.getId(), 0L)))
        .toList();
  }

  @Transactional
  public CategoriaVista crear(String nombre) {
    if (categorias.existsByNombreIgnoreCase(Textos.limpiar(nombre))) {
      throw duplicada();
    }
    return vista(categorias.saveAndFlush(Categoria.crear(nombre)), 0);
  }

  @Transactional
  public CategoriaVista actualizar(Long id, String nombre, long version) {
    Categoria categoria = buscar(id);
    categoria.verificarVersion(version);
    if (categorias.existsByNombreIgnoreCaseAndIdNot(Textos.limpiar(nombre), id)) {
      throw duplicada();
    }
    categoria.renombrar(nombre);
    categorias.flush();
    return vista(categoria, contarProductos(id));
  }

  @Transactional
  public void eliminar(Long id) {
    Categoria categoria = buscar(id);
    if (productos.existsByCategoriaId(id)) {
      throw new CategoriaConProductosException(
          "La categoría \"" + categoria.getNombre() + "\" tiene productos y no se puede eliminar.");
    }
    categorias.delete(categoria);
  }

  private long contarProductos(Long categoriaId) {
    return productos.contarPorCategoria().stream()
        .filter(fila -> categoriaId.equals(fila[0]))
        .map(fila -> (Long) fila[1])
        .findFirst()
        .orElse(0L);
  }

  private Categoria buscar(Long id) {
    return categorias
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe."));
  }

  private static CategoriaDuplicadaException duplicada() {
    return new CategoriaDuplicadaException("Ya existe una categoría con ese nombre.");
  }

  private static CategoriaVista vista(Categoria categoria, long cantidadProductos) {
    return new CategoriaVista(
        categoria.getId(), categoria.getNombre(), cantidadProductos, categoria.getVersion());
  }
}
