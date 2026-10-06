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
import co.italarm.api.inventario.dominio.TipoMovimiento;
import co.italarm.api.inventario.dominio.TipoMovimientoSerial;
import co.italarm.api.inventario.infraestructura.HistorialCostoRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoInventarioRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoSerialRepositorio;
import co.italarm.api.inventario.infraestructura.ProductoInventarioRepositorio;
import co.italarm.api.inventario.infraestructura.SerialRepositorio;
import co.italarm.api.shared.dominio.DocumentoRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final OperacionesInventario operaciones;
  private final Clock reloj;

  public ServicioMovimientos(
      ProductoInventarioRepositorio productos,
      MovimientoInventarioRepositorio kardex,
      HistorialCostoRepositorio historial,
      SerialRepositorio seriales,
      MovimientoSerialRepositorio movimientosSerial,
      ConsultaProductos consultaProductos,
      OperacionesInventario operaciones,
      Clock reloj) {
    this.productos = productos;
    this.kardex = kardex;
    this.historial = historial;
    this.seriales = seriales;
    this.movimientosSerial = movimientosSerial;
    this.consultaProductos = consultaProductos;
    this.operaciones = operaciones;
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
    Map<Long, ProductoInventario> bloqueados = operaciones.bloquear(datos.keySet());
    Instant ahora = reloj.instant();
    List<CambioCosto> cambios = new ArrayList<>();
    for (LineaEntradaCompra linea : lineas) {
      DatosProductoInventario producto =
          OperacionesInventario.exigirProducto(datos, linea.productoId());
      if (!producto.activo()) {
        throw new ProductoInactivoException(
            producto.nombre() + " está inactivo y no se puede comprar.");
      }
      OperacionesInventario.exigirCantidad(linea.cantidad(), producto);
      List<String> numeros =
          operaciones.validarSerialesNuevos(producto, linea.cantidad(), linea.seriales());

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
      operaciones.registrarSeriales(
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
    Map<Long, ProductoInventario> bloqueados = operaciones.bloquear(datos.keySet());
    Optional<String> motivo =
        motivoNoAnulable(compra, lineas.stream().map(LineaAnulacion::productoId).toList());
    if (motivo.isPresent()) {
      throw new CompraNoAnulableException(motivo.get());
    }
    Instant ahora = reloj.instant();
    for (LineaAnulacion linea : lineas) {
      DatosProductoInventario producto =
          OperacionesInventario.exigirProducto(datos, linea.productoId());
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

  /**
   * Bloquea los productos de una salida en orden de id (BP-08) y devuelve su stock y costo vigente
   * (RF-68). Los bloqueos duran hasta el final de la transacción, así que el costo no cambia antes
   * de registrar la salida.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Map<Long, ExistenciaProducto> bloquearParaSalida(Collection<Long> productoIds) {
    return operaciones.bloquear(productoIds).values().stream()
        .collect(
            Collectors.toMap(
                ProductoInventario::getId,
                p -> new ExistenciaProducto(p.getId(), p.getStock(), p.getCostoActualUsd())));
  }

  /**
   * Descuenta lo que sale con una venta o una instalación (RF-102, RF-109): stock, kárdex con el
   * costo vigente y seriales vendidos o instalados con su garantía (RF-22, RF-23, RF-114). Corre
   * dentro de la transacción del documento.
   *
   * @param documento la venta o la instalación
   * @param vencimientoGarantia fin de la garantía de los equipos con serial
   * @param detalleSeriales texto para el historial de los seriales (cliente y documento)
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void registrarSalida(
      DocumentoRef documento,
      LocalDate fecha,
      List<LineaSalida> lineas,
      LocalDate vencimientoGarantia,
      String detalleSeriales,
      Long usuarioId) {
    Map<Long, DatosProductoInventario> datos =
        consultaProductos.porId(lineas.stream().map(LineaSalida::productoId).toList());
    Map<Long, ProductoInventario> bloqueados = operaciones.bloquear(datos.keySet());
    Salida salida = Salida.de(documento);
    Instant ahora = reloj.instant();
    for (LineaSalida linea : lineas) {
      DatosProductoInventario producto =
          OperacionesInventario.exigirProducto(datos, linea.productoId());
      if (!producto.activo()) {
        throw new ProductoInactivoException(
            producto.nombre() + " está inactivo y no se puede usar.");
      }
      OperacionesInventario.exigirCantidad(linea.cantidad(), producto);
      ProductoInventario inventario = bloqueados.get(linea.productoId());
      inventario.salir(linea.cantidad(), producto.abreviatura());
      List<Serial> salen =
          operaciones.serialesQueSalen(producto, linea.cantidad(), linea.seriales());
      kardex.save(
          MovimientoInventario.salida(
              linea.productoId(),
              salida.movimiento(),
              documento,
              fecha,
              linea.cantidad(),
              inventario.getStock(),
              inventario.getCostoActualUsd(),
              null,
              usuarioId,
              ahora));
      for (Serial serial : salen) {
        if (salida == Salida.VENTA) {
          serial.vender(documento, vencimientoGarantia);
        } else {
          serial.instalar(documento, vencimientoGarantia);
        }
        movimientosSerial.save(
            MovimientoSerial.de(
                serial.getId(),
                salida.movimientoSerial(),
                documento,
                fecha,
                detalleSeriales,
                usuarioId,
                ahora));
      }
    }
  }

  /**
   * Devuelve a bodega lo que salió con una venta o instalación anulada (RF-72): entra al costo
   * vigente sin cambiarlo (como un ajuste de entrada, RN-06) y los seriales vuelven a bodega sin
   * garantía.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void anularSalida(
      DocumentoRef documento, LocalDate fecha, List<LineaAnulacion> lineas, Long usuarioId) {
    Salida salida = Salida.de(documento);
    Map<Long, ProductoInventario> bloqueados =
        operaciones.bloquear(lineas.stream().map(LineaAnulacion::productoId).toList());
    Instant ahora = reloj.instant();
    for (LineaAnulacion linea : lineas) {
      ProductoInventario inventario = bloqueados.get(linea.productoId());
      inventario.entrar(linea.cantidad());
      kardex.save(
          MovimientoInventario.entrada(
              linea.productoId(),
              salida.anulacion(),
              documento,
              fecha,
              linea.cantidad(),
              inventario.getStock(),
              inventario.getCostoActualUsd(),
              null,
              usuarioId,
              ahora));
      for (Serial serial :
          seriales.bloquearDeSalida(linea.productoId(), documento.tipo(), documento.id())) {
        serial.devolver(documento);
        movimientosSerial.save(
            MovimientoSerial.de(
                serial.getId(),
                salida.anulacionSerial(),
                documento,
                fecha,
                salida.detalleAnulacion(),
                usuarioId,
                ahora));
      }
    }
  }

  /** Seriales que salieron con un documento, por producto, con su garantía. */
  @Transactional(readOnly = true)
  public Map<Long, List<SerialSalida>> serialesDeSalida(DocumentoRef documento) {
    return seriales
        .findByDocumentoSalidaTipoAndDocumentoSalidaIdOrderById(documento.tipo(), documento.id())
        .stream()
        .collect(
            Collectors.groupingBy(
                Serial::getProductoId,
                Collectors.mapping(
                    s -> new SerialSalida(s.getId(), s.getNumero(), s.getVencimientoGarantia()),
                    Collectors.toList())));
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

  private static CambioCosto cambio(Long productoId, BigDecimal stock, ResultadoCosto resultado) {
    return new CambioCosto(
        productoId,
        stock,
        resultado.costoAnterior(),
        resultado.costoNuevo(),
        resultado.regla().name());
  }

  /** Tipos de salida que descuentan inventario con seriales y garantía. */
  private enum Salida {
    VENTA(
        TipoMovimiento.VENTA,
        TipoMovimiento.ANULACION_VENTA,
        TipoMovimientoSerial.VENTA,
        TipoMovimientoSerial.ANULACION_VENTA,
        "Venta anulada"),
    INSTALACION(
        TipoMovimiento.INSTALACION,
        TipoMovimiento.ANULACION_INSTALACION,
        TipoMovimientoSerial.INSTALACION,
        TipoMovimientoSerial.ANULACION_INSTALACION,
        "Instalación anulada");

    private final TipoMovimiento movimiento;
    private final TipoMovimiento anulacion;
    private final TipoMovimientoSerial movimientoSerial;
    private final TipoMovimientoSerial anulacionSerial;
    private final String detalleAnulacion;

    Salida(
        TipoMovimiento movimiento,
        TipoMovimiento anulacion,
        TipoMovimientoSerial movimientoSerial,
        TipoMovimientoSerial anulacionSerial,
        String detalleAnulacion) {
      this.movimiento = movimiento;
      this.anulacion = anulacion;
      this.movimientoSerial = movimientoSerial;
      this.anulacionSerial = anulacionSerial;
      this.detalleAnulacion = detalleAnulacion;
    }

    static Salida de(DocumentoRef documento) {
      return switch (documento.tipo()) {
        case VENTA -> VENTA;
        case INSTALACION -> INSTALACION;
        default -> throw new IllegalArgumentException("No es una salida: " + documento.tipo());
      };
    }

    TipoMovimiento movimiento() {
      return movimiento;
    }

    TipoMovimiento anulacion() {
      return anulacion;
    }

    TipoMovimientoSerial movimientoSerial() {
      return movimientoSerial;
    }

    TipoMovimientoSerial anulacionSerial() {
      return anulacionSerial;
    }

    String detalleAnulacion() {
      return detalleAnulacion;
    }
  }
}
