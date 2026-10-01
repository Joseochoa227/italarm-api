package co.italarm.api.cargainicial.aplicacion;

import co.italarm.api.cargainicial.dominio.CargaInicialConErroresException;
import co.italarm.api.catalogo.aplicacion.CargaProductos;
import co.italarm.api.inventario.aplicacion.CargaInventario;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.terceros.aplicacion.CargaTerceros;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda la carga inicial en una sola transacción (RF-151): productos, clientes, proveedores y el
 * documento II-00N. Vuelve a validar todo; con un solo error no guarda nada (RF-150).
 */
@Service
public class RegistroCargaInicial {

  private final ValidacionCargaInicial validacion;
  private final CargaProductos productos;
  private final CargaInventario inventario;
  private final CargaTerceros terceros;
  private final ServicioIdempotencia idempotencia;

  public RegistroCargaInicial(
      ValidacionCargaInicial validacion,
      CargaProductos productos,
      CargaInventario inventario,
      CargaTerceros terceros,
      ServicioIdempotencia idempotencia) {
    this.validacion = validacion;
    this.productos = productos;
    this.inventario = inventario;
    this.terceros = terceros;
    this.idempotencia = idempotencia;
  }

  /** Devuelve el id del documento de inventario inicial. */
  @Transactional
  public Long registrar(
      ArchivoCarga archivo, String nombreArchivo, Long usuarioId, ClaveIdempotencia clave) {
    ResultadoCargaVista resultado = validacion.validar(archivo);
    if (!resultado.valido()) {
      throw new CargaInicialConErroresException(resultado.errores());
    }
    idempotencia.reservar(clave);
    Map<String, Long> ids = inventario.idsExistentes(archivo.inventario());
    ids.putAll(productos.crear(archivo.productos()));
    terceros.crear(archivo.clientes(), archivo.proveedores());
    Long documento =
        inventario.registrar(
            archivo.inventario(),
            ids,
            nombreArchivo,
            archivo.productos().size(),
            archivo.clientes().size(),
            archivo.proveedores().size(),
            usuarioId);
    idempotencia.asociar(clave, documento);
    return documento;
  }
}
