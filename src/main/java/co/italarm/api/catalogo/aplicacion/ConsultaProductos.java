package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.catalogo.dominio.Producto;
import co.italarm.api.catalogo.infraestructura.ProductoRepositorio;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos de productos para los módulos de inventario y compras. */
@Service
public class ConsultaProductos {

  private final ProductoRepositorio productos;

  public ConsultaProductos(ProductoRepositorio productos) {
    this.productos = productos;
  }

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
