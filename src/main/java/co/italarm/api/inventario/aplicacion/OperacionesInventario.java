package co.italarm.api.inventario.aplicacion;

import co.italarm.api.catalogo.aplicacion.DatosProductoInventario;
import co.italarm.api.inventario.dominio.MovimientoSerial;
import co.italarm.api.inventario.dominio.ProductoInventario;
import co.italarm.api.inventario.dominio.Serial;
import co.italarm.api.inventario.dominio.SerialDuplicadoException;
import co.italarm.api.inventario.dominio.SerialNoDisponibleException;
import co.italarm.api.inventario.dominio.SerialesNoCoincidenException;
import co.italarm.api.inventario.dominio.TipoMovimientoSerial;
import co.italarm.api.inventario.infraestructura.MovimientoSerialRepositorio;
import co.italarm.api.inventario.infraestructura.ProductoInventarioRepositorio;
import co.italarm.api.inventario.infraestructura.SerialRepositorio;
import co.italarm.api.shared.dominio.CantidadInvalidaException;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.ProductoNoExisteException;
import co.italarm.api.shared.dominio.ReglaCantidad;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Pasos comunes de los movimientos de inventario (compras, ajustes, carga inicial). No abre
 * transacciones: siempre corre dentro de la del caso de uso que la llama.
 */
@Component
public class OperacionesInventario {

  private final ProductoInventarioRepositorio productos;
  private final SerialRepositorio seriales;
  private final MovimientoSerialRepositorio movimientosSerial;

  public OperacionesInventario(
      ProductoInventarioRepositorio productos,
      SerialRepositorio seriales,
      MovimientoSerialRepositorio movimientosSerial) {
    this.productos = productos;
    this.seriales = seriales;
    this.movimientosSerial = movimientosSerial;
  }

  /** Bloquea los productos en orden de id y los devuelve por id. */
  public Map<Long, ProductoInventario> bloquear(Collection<Long> ids) {
    return productos.bloquear(ids).stream()
        .collect(Collectors.toMap(ProductoInventario::getId, Function.identity()));
  }

  public static DatosProductoInventario exigirProducto(
      Map<Long, DatosProductoInventario> datos, Long productoId) {
    DatosProductoInventario producto = datos.get(productoId);
    if (producto == null) {
      throw new ProductoNoExisteException(productoId);
    }
    return producto;
  }

  public static void exigirCantidad(BigDecimal cantidad, DatosProductoInventario producto) {
    if (cantidad == null || cantidad.signum() <= 0) {
      throw new CantidadInvalidaException(
          producto.nombre() + ": la cantidad debe ser mayor que 0.");
    }
    ReglaCantidad.validar(
        cantidad, producto.admiteDecimales(), producto.abreviatura(), producto.nombre());
  }

  /**
   * Seriales de unidades que entran: tantos como la cantidad, sin repetir y que no existan ya para
   * el producto (RF-20, P-22). Un producto sin serial no lleva seriales.
   */
  public List<String> validarSerialesNuevos(
      DatosProductoInventario producto, BigDecimal cantidad, List<String> recibidos) {
    if (!producto.controlaSerial()) {
      if (recibidos != null && !recibidos.isEmpty()) {
        throw new SerialesNoCoincidenException(producto.nombre() + " no controla serial.");
      }
      return List.of();
    }
    List<String> numeros = Serial.validarLista(recibidos, cantidad, producto.nombre());
    List<String> existentes = seriales.existentes(producto.id(), numeros);
    if (!existentes.isEmpty()) {
      throw new SerialDuplicadoException(
          producto.nombre() + ": el serial " + existentes.get(0) + " ya está registrado.");
    }
    return numeros;
  }

  public void registrarSeriales(
      Long productoId,
      List<String> numeros,
      DocumentoRef documento,
      LocalDate fecha,
      String detalle,
      Long usuarioId,
      Instant ahora) {
    for (String numero : numeros) {
      Serial serial = seriales.save(Serial.entrar(productoId, numero, documento, fecha));
      movimientosSerial.save(
          MovimientoSerial.de(
              serial.getId(),
              TipoMovimientoSerial.ENTRADA,
              documento,
              fecha,
              detalle,
              usuarioId,
              ahora));
    }
  }

  /**
   * Seriales que salen de bodega con un documento (ajuste, venta o instalación): tantos como la
   * cantidad, sin repetir y registrados para el producto. Los bloquea en orden de id (BP-08). Un
   * producto sin serial no lleva seriales.
   */
  public List<Serial> serialesQueSalen(
      DatosProductoInventario producto, BigDecimal cantidad, List<String> recibidos) {
    if (!producto.controlaSerial()) {
      if (recibidos != null && !recibidos.isEmpty()) {
        throw new SerialesNoCoincidenException(producto.nombre() + " no controla serial.");
      }
      return List.of();
    }
    List<String> numeros = Serial.validarLista(recibidos, cantidad, producto.nombre());
    List<Serial> encontrados = seriales.bloquear(producto.id(), numeros);
    Set<String> existentes =
        encontrados.stream().map(Serial::getNumero).collect(Collectors.toSet());
    for (String numero : numeros) {
      if (!existentes.contains(numero)) {
        throw new SerialNoDisponibleException(
            producto.nombre() + ": el serial " + numero + " no está registrado.");
      }
    }
    return encontrados;
  }

  /** Da de baja los seriales que salen con un ajuste (RF-59); deben estar en bodega. */
  public void darDeBajaSeriales(
      DatosProductoInventario producto,
      BigDecimal cantidad,
      List<String> recibidos,
      DocumentoRef documento,
      LocalDate fecha,
      String detalle,
      Long usuarioId,
      Instant ahora) {
    for (Serial serial : serialesQueSalen(producto, cantidad, recibidos)) {
      serial.darDeBaja(documento);
      movimientosSerial.save(
          MovimientoSerial.de(
              serial.getId(),
              TipoMovimientoSerial.BAJA,
              documento,
              fecha,
              detalle,
              usuarioId,
              ahora));
    }
  }
}
