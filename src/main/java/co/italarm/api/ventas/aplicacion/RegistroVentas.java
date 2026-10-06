package co.italarm.api.ventas.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.comercial.aplicacion.MaterialPreparado;
import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import co.italarm.api.inventario.aplicacion.ExistenciaProducto;
import co.italarm.api.inventario.aplicacion.LineaSalida;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Garantia;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.shared.infraestructura.GeneradorConsecutivos;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ClienteDocumento;
import co.italarm.api.terceros.aplicacion.ConsultaClientes;
import co.italarm.api.ventas.dominio.ClienteNoExisteException;
import co.italarm.api.ventas.dominio.LineaVenta;
import co.italarm.api.ventas.dominio.Venta;
import co.italarm.api.ventas.dominio.VentaInvalidaException;
import co.italarm.api.ventas.infraestructura.VentaRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de una venta en una sola transacción (RF-102): bloquea los productos, toma su costo
 * vigente (RF-68), guarda la venta con sus tasas (RN-04) y descuenta el inventario con los seriales
 * y su garantía (RF-103).
 */
@Service
public class RegistroVentas {

  private final VentaRepositorio ventas;
  private final ServicioMovimientos movimientos;
  private final ConsultaProductos productos;
  private final ConsultaClientes clientes;
  private final ServicioTasas tasas;
  private final ServicioConfiguracion configuracion;
  private final ServicioIdempotencia idempotencia;
  private final GeneradorConsecutivos consecutivos;
  private final FechaNegocio fechas;

  public RegistroVentas(
      VentaRepositorio ventas,
      ServicioMovimientos movimientos,
      ConsultaProductos productos,
      ConsultaClientes clientes,
      ServicioTasas tasas,
      ServicioConfiguracion configuracion,
      ServicioIdempotencia idempotencia,
      GeneradorConsecutivos consecutivos,
      FechaNegocio fechas) {
    this.ventas = ventas;
    this.movimientos = movimientos;
    this.productos = productos;
    this.clientes = clientes;
    this.tasas = tasas;
    this.configuracion = configuracion;
    this.idempotencia = idempotencia;
    this.consecutivos = consecutivos;
    this.fechas = fechas;
  }

  /** Registra la venta y devuelve su id. */
  @Transactional
  public Long registrar(DatosVenta datos, Long usuarioId, ClaveIdempotencia clave) {
    validarLineas(datos.lineas());
    ClienteDocumento cliente =
        clientes.porId(datos.clienteId()).orElseThrow(ClienteNoExisteException::new);
    Descuento descuento = Descuento.de(datos.descuentoTipo(), datos.descuentoValor());
    LocalDate hoy = fechas.hoy();
    TasasAplicables aplicables = tasas.tasasPara(hoy);
    Tasas conversion = PreparacionMaterial.conversion(aplicables);
    conversion.aUsd(BigDecimal.ONE, datos.moneda());
    idempotencia.reservar(clave);

    List<Long> ids = datos.lineas().stream().map(LineaMaterial::productoId).toList();
    Map<Long, ExistenciaProducto> existencias = movimientos.bloquearParaSalida(ids);
    Map<Long, ProductoValorizado> valorizados = productos.valorizadosPorId(ids);
    Map<Long, BigDecimal> costos = new HashMap<>();
    existencias.forEach((id, existencia) -> costos.put(id, existencia.costoActualUsd()));
    List<MaterialPreparado> lineas =
        PreparacionMaterial.lineas(
            datos.lineas(),
            valorizados,
            costos,
            cliente.precioInstalador(),
            datos.moneda(),
            conversion);
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            lineas.stream().map(MaterialPreparado::calculo).toList(),
            descuento,
            datos.moneda(),
            conversion);

    Venta venta =
        ventas.save(
            Venta.registrar(
                consecutivos.siguiente(TipoDocumento.VENTA),
                hoy,
                PreparacionMaterial.copia(cliente),
                datos.moneda(),
                new Venta.TasasVenta(
                    aplicables.trm(),
                    aplicables.fechaTrm(),
                    aplicables.tasaVes(),
                    aplicables.fechaTasaVes()),
                descuento,
                resumen,
                datos.observaciones(),
                datos.monedasComprobante(),
                lineas.stream()
                    .map(
                        l ->
                            new LineaVenta(
                                l.producto().id(),
                                l.producto().codigo(),
                                l.producto().nombre(),
                                l.producto().abreviatura(),
                                l.cantidad(),
                                l.precioUnitario(),
                                l.precioSugerido(),
                                l.costoUnitarioUsd()))
                    .toList()));
    DocumentoRef documento =
        new DocumentoRef(TipoDocumento.VENTA, venta.getId(), venta.consecutivo());
    movimientos.registrarSalida(
        documento,
        hoy,
        lineas.stream()
            .map(l -> new LineaSalida(l.producto().id(), l.cantidad(), l.seriales()))
            .toList(),
        Garantia.vencimiento(hoy, configuracion.garantiaEquiposMeses()),
        "Cliente: " + cliente.nombre() + " · " + venta.consecutivo(),
        usuarioId);
    ventas.flush();
    idempotencia.asociar(clave, venta.getId());
    return venta.getId();
  }

  /** Al menos un producto y una línea por producto (como en las compras, P-20). */
  static void validarLineas(List<LineaMaterial> lineas) {
    if (lineas == null || lineas.isEmpty()) {
      throw new VentaInvalidaException(
          VentaInvalidaException.SIN_LINEAS, "La venta debe tener al menos un producto.");
    }
    if (PreparacionMaterial.hayRepetidos(lineas)) {
      throw new VentaInvalidaException(
          VentaInvalidaException.PRODUCTO_REPETIDO,
          "Un producto aparece dos veces en la venta. Súmalo en una sola línea.");
    }
  }
}
