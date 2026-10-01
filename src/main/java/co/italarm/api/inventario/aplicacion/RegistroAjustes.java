package co.italarm.api.inventario.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.DatosProductoInventario;
import co.italarm.api.inventario.dominio.Ajuste;
import co.italarm.api.inventario.dominio.CostoRequeridoException;
import co.italarm.api.inventario.dominio.HistorialCosto;
import co.italarm.api.inventario.dominio.MovimientoInventario;
import co.italarm.api.inventario.dominio.ProductoInventario;
import co.italarm.api.inventario.dominio.ReglaCosto;
import co.italarm.api.inventario.dominio.TipoMovimiento;
import co.italarm.api.inventario.infraestructura.AjusteRepositorio;
import co.italarm.api.inventario.infraestructura.HistorialCostoRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoInventarioRepositorio;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.shared.infraestructura.GeneradorConsecutivos;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de un ajuste en una sola transacción (RF-58 a RF-62). La entrada entra al costo vigente
 * sin aplicar la regla de costo (RF-61, RN-06); la salida no deja el stock negativo (RF-62).
 */
@Service
public class RegistroAjustes {

  private final AjusteRepositorio ajustes;
  private final MovimientoInventarioRepositorio kardex;
  private final HistorialCostoRepositorio historial;
  private final OperacionesInventario operaciones;
  private final ConsultaProductos consultaProductos;
  private final ServicioIdempotencia idempotencia;
  private final GeneradorConsecutivos consecutivos;
  private final FechaNegocio fechas;

  public RegistroAjustes(
      AjusteRepositorio ajustes,
      MovimientoInventarioRepositorio kardex,
      HistorialCostoRepositorio historial,
      OperacionesInventario operaciones,
      ConsultaProductos consultaProductos,
      ServicioIdempotencia idempotencia,
      GeneradorConsecutivos consecutivos,
      FechaNegocio fechas) {
    this.ajustes = ajustes;
    this.kardex = kardex;
    this.historial = historial;
    this.operaciones = operaciones;
    this.consultaProductos = consultaProductos;
    this.idempotencia = idempotencia;
    this.consecutivos = consecutivos;
    this.fechas = fechas;
  }

  /** Registra el ajuste y devuelve su id. */
  @Transactional
  public Long registrar(DatosAjuste datos, Long usuarioId, ClaveIdempotencia clave) {
    Map<Long, DatosProductoInventario> encontrados =
        consultaProductos.porId(List.of(datos.productoId()));
    DatosProductoInventario producto =
        OperacionesInventario.exigirProducto(encontrados, datos.productoId());
    Ajuste.validar(datos.motivo(), datos.descripcion(), datos.cantidad());
    BigDecimal cantidad = datos.cantidad().abs();
    OperacionesInventario.exigirCantidad(cantidad, producto);
    idempotencia.reservar(clave);

    ProductoInventario inventario = operaciones.bloquear(List.of(producto.id())).get(producto.id());
    boolean entrada = datos.cantidad().signum() > 0;
    BigDecimal costoAnterior = inventario.getCostoActualUsd();
    BigDecimal costo = costoAnterior;
    List<String> nuevos = List.of();
    if (entrada) {
      if (costo == null) {
        costo = costoInicial(datos.costoUnitarioUsd(), producto);
      }
      nuevos = operaciones.validarSerialesNuevos(producto, cantidad, datos.seriales());
      inventario.entrar(cantidad);
      inventario.cambiarCosto(costo);
    } else {
      inventario.salir(cantidad, producto.abreviatura());
    }

    LocalDate fecha = fechas.hoy();
    Instant ahora = fechas.ahora();
    Ajuste ajuste =
        ajustes.save(
            Ajuste.crear(
                consecutivos.siguiente(TipoDocumento.AJUSTE),
                fecha,
                producto.id(),
                datos.motivo(),
                datos.descripcion(),
                datos.cantidad(),
                costo == null ? BigDecimal.ZERO : Redondeo.paraAlmacenar(costo)));
    DocumentoRef documento = ajuste.documento();
    String detalle = ajuste.detalleMotivo();
    if (entrada) {
      kardex.save(
          MovimientoInventario.entrada(
              producto.id(),
              TipoMovimiento.AJUSTE_ENTRADA,
              documento,
              fecha,
              cantidad,
              inventario.getStock(),
              inventario.getCostoActualUsd(),
              detalle,
              usuarioId,
              ahora));
      if (costoAnterior == null) {
        historial.save(
            HistorialCosto.sinFactura(
                producto.id(),
                documento,
                fecha,
                null,
                inventario.getCostoActualUsd(),
                ReglaCosto.AJUSTE,
                usuarioId,
                ahora));
      }
      operaciones.registrarSeriales(
          producto.id(), nuevos, documento, fecha, "Ajuste: " + detalle, usuarioId, ahora);
    } else {
      kardex.save(
          MovimientoInventario.salida(
              producto.id(),
              TipoMovimiento.AJUSTE_SALIDA,
              documento,
              fecha,
              cantidad,
              inventario.getStock(),
              inventario.getCostoActualUsd(),
              detalle,
              usuarioId,
              ahora));
      operaciones.darDeBajaSeriales(
          producto,
          cantidad,
          datos.seriales(),
          documento,
          fecha,
          "Ajuste: " + detalle,
          usuarioId,
          ahora);
    }
    ajustes.flush();
    idempotencia.asociar(clave, ajuste.getId());
    return ajuste.getId();
  }

  /** P-25: si el producto nunca tuvo costo, la entrada exige el costo unitario en USD. */
  private static BigDecimal costoInicial(BigDecimal costo, DatosProductoInventario producto) {
    if (costo == null || costo.signum() <= 0) {
      throw new CostoRequeridoException(
          producto.nombre()
              + " todavía no tiene costo: ingresa el costo unitario en USD de lo que entra.");
    }
    return costo;
  }
}
