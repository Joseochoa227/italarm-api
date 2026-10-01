package co.italarm.api.cargainicial.aplicacion;

import co.italarm.api.catalogo.aplicacion.CargaProductos;
import co.italarm.api.catalogo.aplicacion.FilaProducto;
import co.italarm.api.inventario.aplicacion.CargaInventario;
import co.italarm.api.inventario.aplicacion.FilaInventario;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.ErrorCarga;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.terceros.aplicacion.CargaTerceros;
import co.italarm.api.terceros.aplicacion.FilaCliente;
import co.italarm.api.terceros.aplicacion.FilaProveedor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revisa todo el archivo sin guardar nada (RF-150): cada hoja con las reglas de su módulo. Las
 * filas con errores de formato no se revisan más, para no repetir el mismo error.
 */
@Service
public class ValidacionCargaInicial {

  private static final List<String> ORDEN_HOJAS =
      List.of(
          CargaProductos.HOJA,
          CargaInventario.HOJA,
          CargaTerceros.HOJA_CLIENTES,
          CargaTerceros.HOJA_PROVEEDORES);

  private final CargaProductos productos;
  private final CargaInventario inventario;
  private final CargaTerceros terceros;

  public ValidacionCargaInicial(
      CargaProductos productos, CargaInventario inventario, CargaTerceros terceros) {
    this.productos = productos;
    this.inventario = inventario;
    this.terceros = terceros;
  }

  @Transactional(readOnly = true)
  public ResultadoCargaVista validar(ArchivoCarga archivo) {
    Set<String> conFormatoInvalido = new HashSet<>();
    archivo.errores().forEach(e -> conFormatoInvalido.add(e.hoja() + "#" + e.fila()));
    List<ErrorCarga> errores = new ArrayList<>(archivo.errores());

    List<FilaProducto> filasProductos =
        sinFormatoInvalido(
            archivo.productos(), CargaProductos.HOJA, FilaProducto::fila, conFormatoInvalido);
    CargaProductos.Validacion validacion = productos.validar(filasProductos);
    errores.addAll(validacion.errores());
    Set<String> productosConErrores = new HashSet<>(validacion.codigosConErrores());
    archivo.productos().stream()
        .filter(f -> !filasProductos.contains(f) && f.codigo() != null && !f.codigo().isBlank())
        .forEach(f -> productosConErrores.add(f.codigo().trim().toUpperCase(Locale.ROOT)));

    errores.addAll(
        inventario.validar(
            sinFormatoInvalido(
                archivo.inventario(),
                CargaInventario.HOJA,
                FilaInventario::fila,
                conFormatoInvalido),
            validacion.nuevos(),
            productosConErrores));
    errores.addAll(
        terceros.validarClientes(
            sinFormatoInvalido(
                archivo.clientes(),
                CargaTerceros.HOJA_CLIENTES,
                FilaCliente::fila,
                conFormatoInvalido)));
    errores.addAll(
        terceros.validarProveedores(
            sinFormatoInvalido(
                archivo.proveedores(),
                CargaTerceros.HOJA_PROVEEDORES,
                FilaProveedor::fila,
                conFormatoInvalido)));

    errores.sort(
        Comparator.comparingInt((ErrorCarga e) -> orden(e.hoja()))
            .thenComparingInt(ErrorCarga::fila));
    return new ResultadoCargaVista(errores.isEmpty(), errores, resumen(archivo));
  }

  private static ResultadoCargaVista.Resumen resumen(ArchivoCarga archivo) {
    BigDecimal valor =
        archivo.inventario().stream()
            .filter(f -> f.cantidad() != null && f.costoUnitarioUsd() != null)
            .map(f -> f.cantidad().multiply(f.costoUnitarioUsd()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ResultadoCargaVista.Resumen(
        archivo.productos().size(),
        archivo.clientes().size(),
        archivo.proveedores().size(),
        archivo.inventario().size(),
        new Dinero(Redondeo.paraAlmacenar(valor), Moneda.USD));
  }

  private static <T> List<T> sinFormatoInvalido(
      List<T> filas, String hoja, ToIntFunction<T> fila, Set<String> conFormatoInvalido) {
    return filas.stream()
        .filter(f -> !conFormatoInvalido.contains(hoja + "#" + fila.applyAsInt(f)))
        .toList();
  }

  private static int orden(String hoja) {
    int posicion = ORDEN_HOJAS.indexOf(hoja);
    return posicion < 0 ? ORDEN_HOJAS.size() : posicion;
  }
}
