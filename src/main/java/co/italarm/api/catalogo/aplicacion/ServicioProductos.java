package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.catalogo.dominio.Categoria;
import co.italarm.api.catalogo.dominio.CategoriaNoExisteException;
import co.italarm.api.catalogo.dominio.DatosProducto;
import co.italarm.api.catalogo.dominio.Producto;
import co.italarm.api.catalogo.dominio.ProductoCodigoDuplicadoException;
import co.italarm.api.catalogo.dominio.ProductoConMovimientosException;
import co.italarm.api.catalogo.dominio.UnidadMedida;
import co.italarm.api.catalogo.dominio.UnidadNoExisteException;
import co.italarm.api.catalogo.infraestructura.CategoriaRepositorio;
import co.italarm.api.catalogo.infraestructura.EspecificacionesProducto;
import co.italarm.api.catalogo.infraestructura.ProductoRepositorio;
import co.italarm.api.catalogo.infraestructura.UnidadMedidaRepositorio;
import co.italarm.api.documentos.aplicacion.ServicioArchivos;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Productos del catálogo (sección 3.3). */
@Service
public class ServicioProductos {

  private final ProductoRepositorio productos;
  private final CategoriaRepositorio categorias;
  private final UnidadMedidaRepositorio unidades;
  private final MovimientosProducto movimientos;
  private final ServicioArchivos archivos;

  public ServicioProductos(
      ProductoRepositorio productos,
      CategoriaRepositorio categorias,
      UnidadMedidaRepositorio unidades,
      MovimientosProducto movimientos,
      ServicioArchivos archivos) {
    this.productos = productos;
    this.categorias = categorias;
    this.unidades = unidades;
    this.movimientos = movimientos;
    this.archivos = archivos;
  }

  @Transactional(readOnly = true)
  public Page<ProductoVista> buscar(
      Long categoriaId, Boolean activo, String buscar, Pageable pagina) {
    return productos
        .findAll(EspecificacionesProducto.filtrar(categoriaId, activo, buscar), pagina)
        .map(this::vista);
  }

  @Transactional(readOnly = true)
  public ProductoVista detalle(Long id) {
    return vista(buscarProducto(id));
  }

  @Transactional
  public ProductoVista crear(DatosProducto datos, Long categoriaId, Long unidadId) {
    if (productos.existsByCodigoIgnoreCase(Producto.normalizarCodigo(datos.codigo()))) {
      throw codigoDuplicado(datos.codigo());
    }
    Producto producto = Producto.crear(datos, categoria(categoriaId), unidad(unidadId));
    return vista(productos.saveAndFlush(producto));
  }

  @Transactional
  public ProductoVista actualizar(
      Long id, DatosProducto datos, Long categoriaId, Long unidadId, long version) {
    Producto producto = buscarProducto(id);
    producto.verificarVersion(version);
    if (productos.existsByCodigoIgnoreCaseAndIdNot(Producto.normalizarCodigo(datos.codigo()), id)) {
      throw codigoDuplicado(datos.codigo());
    }
    producto.actualizar(
        datos, categoria(categoriaId), unidad(unidadId), movimientos.tieneMovimientos(id));
    productos.flush();
    return vista(producto);
  }

  @Transactional
  public ProductoVista desactivar(Long id) {
    Producto producto = buscarProducto(id);
    producto.desactivar();
    productos.flush();
    return vista(producto);
  }

  @Transactional
  public ProductoVista activar(Long id) {
    Producto producto = buscarProducto(id);
    producto.activar();
    productos.flush();
    return vista(producto);
  }

  /** Elimina un producto sin movimientos (RF-14). */
  @Transactional
  public void eliminar(Long id) {
    Producto producto = buscarProducto(id);
    if (movimientos.tieneMovimientos(id)) {
      throw new ProductoConMovimientosException(
          "El producto tiene movimientos y no se puede eliminar; puedes desactivarlo.");
    }
    productos.delete(producto);
    archivos.eliminarAlConfirmar(producto.getFotoClave());
  }

  @Transactional
  public ProductoVista cambiarFoto(Long id, byte[] contenido) {
    Producto producto = buscarProducto(id);
    String clave = archivos.guardarImagen("productos/" + id, "foto", contenido);
    archivos.eliminarAlConfirmar(producto.cambiarFoto(clave));
    productos.flush();
    return vista(producto);
  }

  @Transactional
  public ProductoVista quitarFoto(Long id) {
    Producto producto = buscarProducto(id);
    archivos.eliminarAlConfirmar(producto.quitarFoto());
    productos.flush();
    return vista(producto);
  }

  private Producto buscarProducto(Long id) {
    return productos
        .findConRelacionesById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe."));
  }

  private Categoria categoria(Long id) {
    return categorias
        .findById(id)
        .orElseThrow(() -> new CategoriaNoExisteException("La categoría indicada no existe."));
  }

  private UnidadMedida unidad(Long id) {
    return unidades
        .findById(id)
        .orElseThrow(() -> new UnidadNoExisteException("La unidad de medida indicada no existe."));
  }

  private static ProductoCodigoDuplicadoException codigoDuplicado(String codigo) {
    return new ProductoCodigoDuplicadoException(
        "Ya existe un producto con el código " + Producto.normalizarCodigo(codigo) + ".");
  }

  private ProductoVista vista(Producto producto) {
    return new ProductoVista(
        producto.getId(),
        producto.getCodigo(),
        producto.getNombre(),
        producto.getMarca(),
        producto.getModelo(),
        new ProductoVista.Referencia(
            producto.getCategoria().getId(), producto.getCategoria().getNombre()),
        UnidadMedidaVista.de(producto.getUnidadMedida()),
        producto.isControlaSerial(),
        producto.precioInstalador(),
        producto.precioClienteFinal(),
        Vistas.cantidad(producto.getStock()),
        Vistas.cantidad(producto.getStockMinimo()),
        producto.estaBajoMinimo(),
        producto.getCostoActualUsd() == null
            ? null
            : new Dinero(producto.getCostoActualUsd(), Moneda.USD),
        producto.getDescripcion(),
        archivos.urlDe(producto.getFotoClave()),
        producto.isActivo(),
        producto.getVersion());
  }
}
