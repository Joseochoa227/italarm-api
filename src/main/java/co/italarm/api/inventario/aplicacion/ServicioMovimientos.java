package co.italarm.api.inventario.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.DatosProductoInventario;
import co.italarm.api.inventario.dominio.AnulabilidadCompra;
import co.italarm.api.inventario.dominio.CompraNoAnulableException;
import co.italarm.api.inventario.dominio.HistorialCosto;
import co.italarm.api.inventario.dominio.MotorCosto;
import co.italarm.api.inventario.dominio.MovimientoInventario;
import co.italarm.api.inventario.dominio.MovimientoSerial;
import co.italarm.api.inventario.dominio.ProductoInactivoException;
import co.italarm.api.inventario.dominio.ProductoInventario;
import co.italarm.api.inventario.dominio.ProductoNoExisteException;
import co.italarm.api.inventario.dominio.ReglaCosto;
import co.italarm.api.inventario.dominio.ResultadoCosto;
import co.italarm.api.inventario.dominio.Serial;
import co.italarm.api.inventario.dominio.SerialDuplicadoException;
import co.italarm.api.inventario.dominio.SerialesNoCoincidenException;
import co.italarm.api.inventario.dominio.TipoMovimiento;
import co.italarm.api.inventario.dominio.TipoMovimientoSerial;
import co.italarm.api.inventario.infraestructura.HistorialCostoRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoInventarioRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoSerialRepositorio;
import co.italarm.api.inventario.infraestructura.ProductoInventarioRepositorio;
import co.italarm.api.inventario.infraestructura.SerialRepositorio;
import co.italarm.api.shared.dominio.CantidadInvalidaException;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.ReglaCantidad;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimientos de inventario que hacen otros módulos (compras ahora; ventas e instalaciones
 * después). Bloquea los productos y seriales afectados en orden de id (BP-08), aplica la regla de
 * costo y escribe el kárdex, el historial de costo y el de seriales.
 */
@Service
public class ServicioMovimientos {

  private final ProductoInventarioRepositorio productos;
  private final MovimientoInventarioRepositorio kardex;
  private final HistorialCostoRepositorio historial;
  private final SerialRepositorio seriales;
  private final MovimientoSerialRepositorio movimientosSerial;
  private final ConsultaProductos consultaProductos;
  private final Clock reloj;

  public ServicioMovimientos(
      ProductoInventarioRepositorio productos,
      MovimientoInventarioRepositorio kardex,
      HistorialCostoRepositorio historial,
      SerialRepositorio seriales,
      MovimientoSerialRepositorio movimientosSerial,
      ConsultaProductos consultaProductos,
      Clock reloj) {
    this.productos = productos;
    this.kardex = kardex;
    this.historial = historial;
    this.seriales = seriales;
    this.movimientosSerial = movimientosSerial;
    this.consultaProductos = consultaProductos;
    this.reloj = reloj;
  }

  /** Cómo cambiaría el costo de un producto con una compra, sin guardar nada (RF-41). */
  @Transactional(readOnly = true)
  public CambioCosto vistaPreviaCosto(
      Long productoId, BigDecimal cantidad, BigDecimal costoUnitarioUsd) {
    ProductoInventario producto =
        productos.findById(productoId).orElseThrow(() -> new ProductoNoExisteException(productoId));
    ResultadoCosto resultado =
        MotorCosto.aplicarCompra(
            producto.getStock(), producto.getCostoActualUsd(), cantidad, costoUnitarioUsd);
    return cambio(productoId, producto.getStock(), resultado);
  }

  /**
   * Suma al inventario lo que entra con una compra (RF-45, RF-66): stock, costo, kárdex, historial
   * de costo y seriales. Corre dentro de la transacción de la compra.
   *
   * @param detalleSeriales texto para el historial de los seriales (proveedor y factura)
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public List<CambioCosto> registrarEntradaCompra(
      DocumentoRef compra,
      LocalDate fecha,
      List<LineaEntradaCompra> lineas,
      String detalleSeriales,
      Long usuarioId) {
    Map<Long, DatosProductoInventario> datos =
        consultaProductos.porId(lineas.stream().map(LineaEntradaCompra::productoId).toList());
    Map<Long, ProductoInventario> bloqueados = bloquear(datos.keySet());
    Instant ahora = reloj.instant();
    List<CambioCosto> cambios = new ArrayList<>();
    for (LineaEntradaCompra linea : lineas) {
      DatosProductoInventario producto = exigirProducto(datos, linea.productoId());
      if (!producto.activo()) {
        throw new ProductoInactivoException(
            producto.nombre() + " está inactivo y no se puede comprar.");
      }
      exigirCantidad(linea.cantidad(), producto);
      List<String> numeros = validarSerialesNuevos(producto, linea.cantidad(), linea.seriales());

      ProductoInventario inventario = bloqueados.get(linea.productoId());
      BigDecimal stockAntes = inventario.getStock();
      ResultadoCosto resultado =
          MotorCosto.aplicarCompra(
              stockAntes,
              inventario.getCostoActualUsd(),
              linea.cantidad(),
              linea.costoUnitarioUsd());
      inventario.entrar(linea.cantidad());
      inventario.cambiarCosto(resultado.costoNuevo());

      kardex.save(
          MovimientoInventario.entrada(
              linea.productoId(),
              TipoMovimiento.COMPRA,
              compra,
              fecha,
              linea.cantidad(),
              inventario.getStock(),
              resultado.costoNuevo(),
              null,
              usuarioId,
              ahora));
      historial.save(
          HistorialCosto.deCompra(
              linea.productoId(),
              compra,
              fecha,
              linea.monedaFactura(),
              linea.tasaFactura(),
              linea.costoUnitarioFactura(),
              linea.costoUnitarioUsd(),
              resultado,
              usuarioId,
              ahora));
      registrarSeriales(
          linea.productoId(), numeros, compra, fecha, detalleSeriales, usuarioId, ahora);
      cambios.add(cambio(linea.productoId(), stockAntes, resultado));
    }
    return cambios;
  }

  /**
   * Motivo por el que no se puede anular la compra (P-23), o vacío si se puede: para cada producto,
   * la compra debe ser su último movimiento y sus seriales deben seguir en bodega.
   */
  @Transactional(readOnly = true)
  public Optional<String> motivoNoAnulable(DocumentoRef compra, List<Long> productoIds) {
    Map<Long, DatosProductoInventario> datos = consultaProductos.porId(productoIds);
    List<AnulabilidadCompra.Producto> estado = new ArrayList<>();
    for (Long productoId : productoIds.stream().sorted().toList()) {
      boolean ultimo =
          kardex
              .findFirstByProductoIdOrderByIdDesc(productoId)
              .filter(m -> m.getTipo() == TipoMovimiento.COMPRA && compra.equals(m.getDocumento()))
              .isPresent();
      boolean enBodega =
          seriales
              .findByProductoIdAndDocumentoEntradaTipoAndDocumentoEntradaId(
                  productoId, compra.tipo(), compra.id())
              .stream()
              .allMatch(Serial::estaDisponible);
      String nombre =
          datos.containsKey(productoId) ? datos.get(productoId).nombre() : "El producto";
      estado.add(new AnulabilidadCompra.Producto(nombre, ultimo, enBodega));
    }
    return AnulabilidadCompra.motivoNoAnulable(estado);
  }

  /**
   * Revierte lo que entró con una compra (RF-71): descuenta el stock, devuelve el costo al anterior
   * según el historial y deja sus seriales anulados. Corre dentro de la transacción de la
   * anulación.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void anularEntradaCompra(
      DocumentoRef compra, LocalDate fecha, List<LineaAnulacion> lineas, Long usuarioId) {
    Map<Long, DatosProductoInventario> datos =
        consultaProductos.porId(lineas.stream().map(LineaAnulacion::productoId).toList());
    Map<Long, ProductoInventario> bloqueados = bloquear(datos.keySet());
    Optional<String> motivo =
        motivoNoAnulable(compra, lineas.stream().map(LineaAnulacion::productoId).toList());
    if (motivo.isPresent()) {
      throw new CompraNoAnulableException(motivo.get());
    }
    Instant ahora = reloj.instant();
    for (LineaAnulacion linea : lineas) {
      DatosProductoInventario producto = exigirProducto(datos, linea.productoId());
      ProductoInventario inventario = bloqueados.get(linea.productoId());
      BigDecimal costoActual = inventario.getCostoActualUsd();
      BigDecimal costoAnterior =
          historial
              .findFirstByProductoIdAndDocumentoTipoAndDocumentoIdOrderByIdAsc(
                  linea.productoId(), compra.tipo(), compra.id())
              .map(HistorialCosto::getCostoAnterior)
              .orElse(costoActual);
      inventario.salir(linea.cantidad(), producto.abreviatura());
      inventario.cambiarCosto(costoAnterior);
      kardex.save(
          MovimientoInventario.salida(
              linea.productoId(),
              TipoMovimiento.ANULACION_COMPRA,
              compra,
              fecha,
              linea.cantidad(),
              inventario.getStock(),
              inventario.getCostoActualUsd(),
              null,
              usuarioId,
              ahora));
      historial.save(
          HistorialCosto.sinFactura(
              linea.productoId(),
              compra,
              fecha,
              costoActual,
              inventario.getCostoActualUsd(),
              ReglaCosto.ANULACION,
              usuarioId,
              ahora));
      for (Serial serial :
          seriales.bloquearDeEntrada(linea.productoId(), compra.tipo(), compra.id())) {
        serial.anular();
        movimientosSerial.save(
            MovimientoSerial.de(
                serial.getId(),
                TipoMovimientoSerial.ANULACION,
                compra,
                fecha,
                "Compra anulada",
                usuarioId,
                ahora));
      }
    }
  }

  /** Seriales que entraron con un documento, por producto. */
  @Transactional(readOnly = true)
  public Map<Long, List<String>> serialesDeEntrada(DocumentoRef documento) {
    return seriales
        .findByDocumentoEntradaTipoAndDocumentoEntradaIdOrderById(documento.tipo(), documento.id())
        .stream()
        .collect(
            Collectors.groupingBy(
                Serial::getProductoId, Collectors.mapping(Serial::getNumero, Collectors.toList())));
  }

  /** Bloquea los productos en orden de id y los devuelve por id. */
  Map<Long, ProductoInventario> bloquear(java.util.Collection<Long> ids) {
    return productos.bloquear(ids).stream()
        .collect(Collectors.toMap(ProductoInventario::getId, Function.identity()));
  }

  static DatosProductoInventario exigirProducto(
      Map<Long, DatosProductoInventario> datos, Long productoId) {
    DatosProductoInventario producto = datos.get(productoId);
    if (producto == null) {
      throw new ProductoNoExisteException(productoId);
    }
    return producto;
  }

  static void exigirCantidad(BigDecimal cantidad, DatosProductoInventario producto) {
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
  List<String> validarSerialesNuevos(
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

  void registrarSeriales(
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

  private static CambioCosto cambio(Long productoId, BigDecimal stock, ResultadoCosto resultado) {
    return new CambioCosto(
        productoId,
        stock,
        resultado.costoAnterior(),
        resultado.costoNuevo(),
        resultado.regla().name());
  }
}
