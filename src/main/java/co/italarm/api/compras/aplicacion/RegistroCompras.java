package co.italarm.api.compras.aplicacion;

import co.italarm.api.compras.dominio.Compra;
import co.italarm.api.compras.dominio.LineaCompra;
import co.italarm.api.compras.dominio.ProveedorNoExisteException;
import co.italarm.api.compras.dominio.ReglasCompra;
import co.italarm.api.compras.infraestructura.CompraRepositorio;
import co.italarm.api.inventario.aplicacion.CambioCosto;
import co.italarm.api.inventario.aplicacion.LineaEntradaCompra;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.shared.infraestructura.GeneradorConsecutivos;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ConsultaProveedores;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de una compra en una sola transacción (RF-45): guarda la compra con sus tasas y suma al
 * inventario (stock, costo, kárdex, historial de costo y seriales).
 */
@Service
public class RegistroCompras {

  private final CompraRepositorio compras;
  private final ServicioMovimientos movimientos;
  private final ServicioTasas tasas;
  private final ConsultaProveedores proveedores;
  private final ServicioIdempotencia idempotencia;
  private final GeneradorConsecutivos consecutivos;
  private final FechaNegocio fechas;

  public RegistroCompras(
      CompraRepositorio compras,
      ServicioMovimientos movimientos,
      ServicioTasas tasas,
      ConsultaProveedores proveedores,
      ServicioIdempotencia idempotencia,
      GeneradorConsecutivos consecutivos,
      FechaNegocio fechas) {
    this.compras = compras;
    this.movimientos = movimientos;
    this.tasas = tasas;
    this.proveedores = proveedores;
    this.idempotencia = idempotencia;
    this.consecutivos = consecutivos;
    this.fechas = fechas;
  }

  /** Registra la compra y devuelve su id. */
  @Transactional
  public Long registrar(DatosCompra datos, Long usuarioId, ClaveIdempotencia clave) {
    LocalDate fecha = fechaDe(datos.fecha(), fechas);
    ReglasCompra.validar(fecha, fechas.hoy(), lineasAValidar(datos.lineas()));
    String proveedor = proveedores.nombres(List.of(datos.proveedorId())).get(datos.proveedorId());
    if (proveedor == null) {
      throw new ProveedorNoExisteException();
    }
    idempotencia.reservar(clave);

    TasasAplicables aplicables = tasas.tasasPara(fecha);
    Tasas conversion = new Tasas(aplicables.trm(), aplicables.tasaVes());
    BigDecimal tasaFactura = tasaDeLaFactura(datos.moneda(), aplicables);
    List<LineaCompra> lineas = new ArrayList<>();
    for (DatosCompra.Linea linea : datos.lineas()) {
      lineas.add(
          new LineaCompra(
              linea.productoId(),
              linea.cantidad(),
              linea.costoUnitario(),
              conversion.aUsd(linea.costoUnitario(), datos.moneda())));
    }
    Compra compra =
        compras.save(
            Compra.registrar(
                consecutivos.siguiente(TipoDocumento.COMPRA),
                fecha,
                datos.proveedorId(),
                datos.numeroFactura(),
                datos.moneda(),
                aplicables.trm(),
                aplicables.fechaTrm(),
                aplicables.tasaVes(),
                aplicables.fechaTasaVes(),
                lineas));
    DocumentoRef documento =
        new DocumentoRef(TipoDocumento.COMPRA, compra.getId(), compra.consecutivo());

    Map<Long, DatosCompra.Linea> porProducto =
        datos.lineas().stream()
            .collect(Collectors.toMap(DatosCompra.Linea::productoId, Function.identity()));
    List<LineaEntradaCompra> entradas =
        compra.getLineas().stream()
            .map(
                linea ->
                    new LineaEntradaCompra(
                        linea.getProductoId(),
                        linea.getCantidad(),
                        linea.getCostoUnitarioUsd(),
                        porProducto.get(linea.getProductoId()).seriales(),
                        datos.moneda(),
                        tasaFactura,
                        linea.getCostoUnitario()))
            .toList();
    Map<Long, CambioCosto> cambios =
        movimientos
            .registrarEntradaCompra(
                documento,
                fecha,
                entradas,
                "Proveedor: " + proveedor + " · Factura " + compra.getNumeroFactura(),
                usuarioId)
            .stream()
            .collect(Collectors.toMap(CambioCosto::productoId, Function.identity()));
    for (LineaCompra linea : compra.getLineas()) {
      CambioCosto cambio = cambios.get(linea.getProductoId());
      linea.registrarCambioCosto(cambio.costoAnterior(), cambio.costoNuevo(), cambio.regla());
    }
    compras.flush();
    idempotencia.asociar(clave, compra.getId());
    return compra.getId();
  }

  static LocalDate fechaDe(LocalDate fecha, FechaNegocio fechas) {
    return fecha == null ? fechas.hoy() : fecha;
  }

  static List<ReglasCompra.Linea> lineasAValidar(List<DatosCompra.Linea> lineas) {
    return lineas == null
        ? List.of()
        : lineas.stream()
            .map(l -> new ReglasCompra.Linea(l.productoId(), l.cantidad(), l.costoUnitario()))
            .toList();
  }

  /** Tasa con que se convirtió la factura; null si es en USD. */
  static BigDecimal tasaDeLaFactura(Moneda moneda, TasasAplicables aplicables) {
    return switch (moneda) {
      case USD -> null;
      case COP -> aplicables.trm();
      case VES -> aplicables.tasaVes();
    };
  }
}
