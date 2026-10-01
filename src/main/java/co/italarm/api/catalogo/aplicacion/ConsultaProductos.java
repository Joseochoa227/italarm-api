package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.catalogo.dominio.Producto;
import co.italarm.api.catalogo.infraestructura.EspecificacionesProducto;
import co.italarm.api.catalogo.infraestructura.ProductoRepositorio;
import co.italarm.api.catalogo.infraestructura.ValorInventario;
import co.italarm.api.documentos.aplicacion.ServicioArchivos;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos de productos para los módulos de inventario y compras. */
@Service
public class ConsultaProductos {

  private final ProductoRepositorio productos;
  private final ValorInventario valor;
  private final ServicioArchivos archivos;

  public ConsultaProductos(
      ProductoRepositorio productos, ValorInventario valor, ServicioArchivos archivos) {
    this.productos = productos;
    this.valor = valor;
    this.archivos = archivos;
  }

  /**
   * Productos del listado de inventario (RF-49 a RF-51). La búsqueda también trae {@code
   * idsPorSerial}: los productos con un serial que coincide.
   */
  @Transactional(readOnly = true)
  public Page<ProductoValorizado> valorizados(
      Long categoriaId,
      Boolean activo,
      String buscar,
      Collection<Long> idsPorSerial,
      Pageable pagina) {
    return productos
        .findAll(
            EspecificacionesProducto.filtrar(categoriaId, activo, buscar, idsPorSerial), pagina)
        .map(this::valorizado);
  }

  /** Cantidad de productos y valor en USD de todo el filtro, no solo de la página. */
  @Transactional(readOnly = true)
  public TotalInventario total(
      Long categoriaId, Boolean activo, String buscar, Collection<Long> idsPorSerial) {
    ValorInventario.Total total =
        valor.calcular(EspecificacionesProducto.filtrar(categoriaId, activo, buscar, idsPorSerial));
    return new TotalInventario(total.productos(), total.valorUsd());
  }

  @Transactional(readOnly = true)
  public Optional<ProductoValorizado> valorizado(Long id) {
    return productos.findConRelacionesById(id).map(this::valorizado);
  }

  /** Total del listado de inventario. */
  public record TotalInventario(long productos, BigDecimal valorUsd) {}

  @Transactional(readOnly = true)
  public Map<Long, DatosProductoInventario> porId(Collection<Long> ids) {
    return productos.findByIdIn(ids).stream()
        .map(ConsultaProductos::datos)
        .collect(Collectors.toMap(DatosProductoInventario::id, Function.identity()));
  }

  /** Por código (en mayúsculas, como se guarda). */
  @Transactional(readOnly = true)
  public Map<String, DatosProductoInventario> porCodigo(Collection<String> codigos) {
    return productos
        .findByCodigoIn(codigos.stream().map(Producto::normalizarCodigo).toList())
        .stream()
        .map(ConsultaProductos::datos)
        .collect(Collectors.toMap(DatosProductoInventario::codigo, Function.identity()));
  }

  private ProductoValorizado valorizado(Producto p) {
    return new ProductoValorizado(
        p.getId(),
        p.getCodigo(),
        p.getNombre(),
        p.getMarca(),
        p.getModelo(),
        p.getCategoria().getId(),
        p.getCategoria().getNombre(),
        p.getUnidadMedida().getAbreviatura(),
        p.isControlaSerial(),
        Vistas.cantidad(p.getStock()),
        Vistas.cantidad(p.getStockMinimo()),
        p.estaBajoMinimo(),
        p.getCostoActualUsd(),
        p.precioInstalador(),
        p.precioClienteFinal(),
        archivos.urlDe(p.getFotoClave()),
        p.isActivo());
  }

  private static DatosProductoInventario datos(Producto p) {
    return new DatosProductoInventario(
        p.getId(),
        p.getCodigo(),
        p.getNombre(),
        p.isActivo(),
        p.isControlaSerial(),
        p.getUnidadMedida().isAdmiteDecimales(),
        p.getUnidadMedida().getAbreviatura(),
        p.getCategoria().getId(),
        p.getCategoria().getNombre(),
        p.getMarca());
  }
}
