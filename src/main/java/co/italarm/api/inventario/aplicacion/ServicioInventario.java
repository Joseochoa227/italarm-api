package co.italarm.api.inventario.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.DatosProductoInventario;
import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.inventario.dominio.EstadoSerial;
import co.italarm.api.inventario.dominio.HistorialCosto;
import co.italarm.api.inventario.dominio.MovimientoInventario;
import co.italarm.api.inventario.dominio.MovimientoSerial;
import co.italarm.api.inventario.dominio.Serial;
import co.italarm.api.inventario.infraestructura.HistorialCostoRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoInventarioRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoSerialRepositorio;
import co.italarm.api.inventario.infraestructura.SerialRepositorio;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas del inventario: listado valorizado, detalle, kárdex, costos y seriales (3.7). */
@Service
public class ServicioInventario {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final ConsultaProductos productos;
  private final MovimientoInventarioRepositorio kardex;
  private final HistorialCostoRepositorio historial;
  private final SerialRepositorio seriales;
  private final MovimientoSerialRepositorio movimientosSerial;
  private final ServicioTasas tasas;
  private final ConsultaUsuarios usuarios;
  private final FechaNegocio fechas;

  public ServicioInventario(
      ConsultaProductos productos,
      MovimientoInventarioRepositorio kardex,
      HistorialCostoRepositorio historial,
      SerialRepositorio seriales,
      MovimientoSerialRepositorio movimientosSerial,
      ServicioTasas tasas,
      ConsultaUsuarios usuarios,
      FechaNegocio fechas) {
    this.productos = productos;
    this.kardex = kardex;
    this.historial = historial;
    this.seriales = seriales;
    this.movimientosSerial = movimientosSerial;
    this.tasas = tasas;
    this.usuarios = usuarios;
    this.fechas = fechas;
  }

  /**
   * Listado valorizado (RF-49 a RF-52). Busca por nombre, código, marca o número de serie, y
   * totaliza todo el filtro, no solo la página.
   */
  @Transactional(readOnly = true)
  public InventarioVista listar(Long categoriaId, Boolean activo, String buscar, Pageable pagina) {
    List<Long> porSerial = productosConSerial(buscar);
    TasasAplicables aplicables = tasas.tasasPara(fechas.hoy());
    Tasas conversion = new Tasas(aplicables.trm(), aplicables.tasaVes());
    Page<ProductoValorizado> encontrados =
        productos.valorizados(categoriaId, activo, buscar, porSerial, pagina);
    ConsultaProductos.TotalInventario total =
        productos.total(categoriaId, activo, buscar, porSerial);
    return new InventarioVista(
        Pagina.de(
            encontrados,
            p ->
                new InventarioVista.Producto(
                    p.id(),
                    p.codigo(),
                    p.nombre(),
                    p.marca(),
                    p.categoria(),
                    p.abreviatura(),
                    p.controlaSerial(),
                    p.stock(),
                    p.stockMinimo(),
                    p.bajoMinimo(),
                    usd(p.costoActualUsd()),
                    conversion.equivalentes(valorEnBodega(p)),
                    p.fotoUrl(),
                    p.activo())),
        total.productos(),
        conversion.equivalentes(new Dinero(Redondeo.paraAlmacenar(total.valorUsd()), Moneda.USD)),
        avisos(aplicables));
  }

  /** Indicadores del producto en las tres monedas y seriales por estado (RF-53 a RF-55). */
  @Transactional(readOnly = true)
  public ProductoInventarioVista detalle(Long productoId) {
    ProductoValorizado p =
        productos
            .valorizado(productoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe."));
    TasasAplicables aplicables = tasas.tasasPara(fechas.hoy());
    Tasas conversion = new Tasas(aplicables.trm(), aplicables.tasaVes());
    Map<EstadoSerial, Long> porEstado = new EnumMap<>(EstadoSerial.class);
    for (Object[] fila : seriales.contarPorEstado(productoId)) {
      porEstado.put((EstadoSerial) fila[0], (Long) fila[1]);
    }
    return new ProductoInventarioVista(
        p.id(),
        p.codigo(),
        p.nombre(),
        p.marca(),
        p.modelo(),
        p.categoria(),
        p.abreviatura(),
        p.controlaSerial(),
        p.stock(),
        p.stockMinimo(),
        p.bajoMinimo(),
        p.costoActualUsd() == null ? null : conversion.equivalentes(usd(p.costoActualUsd())),
        conversion.equivalentes(valorEnBodega(p)),
        conversion.equivalentes(p.precioInstalador()),
        conversion.equivalentes(p.precioClienteFinal()),
        new ProductoInventarioVista.SerialesPorEstado(
            porEstado.getOrDefault(EstadoSerial.EN_BODEGA, 0L),
            porEstado.getOrDefault(EstadoSerial.VENDIDO, 0L),
            porEstado.getOrDefault(EstadoSerial.INSTALADO, 0L),
            porEstado.getOrDefault(EstadoSerial.DADO_DE_BAJA, 0L),
            porEstado.getOrDefault(EstadoSerial.ANULADO, 0L)),
        p.fotoUrl(),
        p.activo(),
        avisos(aplicables));
  }

  /** Kárdex del producto (RF-56). */
  @Transactional(readOnly = true)
  public Page<MovimientoKardexVista> kardex(Long productoId, Pageable pagina) {
    exigirProducto(productoId);
    Page<MovimientoInventario> movimientos = kardex.findByProductoId(productoId, pagina);
    Map<Long, String> nombres =
        nombres(movimientos.getContent().stream().map(MovimientoInventario::getUsuarioId).toList());
    return movimientos.map(
        m ->
            new MovimientoKardexVista(
                m.getId(),
                m.getFecha(),
                m.getTipo().name(),
                m.getTipo().etiqueta(),
                m.getDetalle(),
                m.getDocumento(),
                Cantidades.sinCeros(m.getEntrada()),
                Cantidades.sinCeros(m.getSalida()),
                Cantidades.sinCeros(m.getSaldo()),
                usd(m.getCostoUnitarioUsd()),
                nombres.get(m.getUsuarioId()),
                m.getRegistradoEn()));
  }

  /** Cambios de costo del producto, del más reciente al más antiguo (RF-57). */
  @Transactional(readOnly = true)
  public List<CambioCostoVista> historialCosto(Long productoId) {
    exigirProducto(productoId);
    List<HistorialCosto> cambios = historial.findByProductoIdOrderByIdDesc(productoId);
    Map<Long, String> nombres =
        nombres(cambios.stream().map(HistorialCosto::getUsuarioId).toList());
    return cambios.stream()
        .map(
            h ->
                new CambioCostoVista(
                    h.getId(),
                    h.getFecha(),
                    h.getDocumento(),
                    h.getRegla().name(),
                    usd(h.getCostoAnterior()),
                    usd(h.getCostoNuevo()),
                    h.getCostoFactura() == null
                        ? null
                        : new Dinero(h.getCostoFactura(), h.getMonedaFactura()),
                    h.getTasaFactura(),
                    h.getCostoFacturaUsd() == null
                        ? null
                        : usd(Redondeo.paraAlmacenar(h.getCostoFacturaUsd())),
                    nombres.get(h.getUsuarioId()),
                    h.getRegistradoEn()))
        .toList();
  }

  /** Seriales del producto, todos o de un estado (RF-55). */
  @Transactional(readOnly = true)
  public List<SerialVista> serialesDelProducto(Long productoId, EstadoSerial estado) {
    DatosProductoInventario producto = exigirProducto(productoId);
    List<Serial> encontrados =
        estado == null
            ? seriales.findByProductoIdOrderByNumero(productoId)
            : seriales.findByProductoIdAndEstadoOrderByNumero(productoId, estado);
    return encontrados.stream().map(s -> vista(s, Map.of(productoId, producto))).toList();
  }

  /** Busca un serial desde cualquier pantalla (RF-24): hasta 50 que contengan el texto. */
  @Transactional(readOnly = true)
  public List<SerialVista> buscarSeriales(String numero) {
    if (numero == null || numero.isBlank()) {
      return List.of();
    }
    List<Serial> encontrados =
        seriales.findTop50ByNumeroContainingOrderByNumero(numero.trim().toUpperCase(Locale.ROOT));
    Map<Long, DatosProductoInventario> datos =
        productos.porId(
            encontrados.stream().map(Serial::getProductoId).collect(Collectors.toSet()));
    return encontrados.stream().map(s -> vista(s, datos)).toList();
  }

  /** Historial completo de un serial (RF-24). */
  @Transactional(readOnly = true)
  public HistorialSerialVista historialSerial(Long serialId) {
    Serial serial =
        seriales
            .findById(serialId)
            .orElseThrow(() -> new RecursoNoEncontradoException("El serial no existe."));
    List<MovimientoSerial> movimientos = movimientosSerial.findBySerialIdOrderByIdAsc(serialId);
    Map<Long, String> nombres =
        nombres(movimientos.stream().map(MovimientoSerial::getUsuarioId).toList());
    return new HistorialSerialVista(
        vista(serial, productos.porId(List.of(serial.getProductoId()))),
        movimientos.stream()
            .map(
                m ->
                    new HistorialSerialVista.Movimiento(
                        m.getTipo().name(),
                        m.getFecha(),
                        m.getDocumento(),
                        m.getDetalle(),
                        nombres.get(m.getUsuarioId()),
                        m.getRegistradoEn()))
            .toList());
  }

  private List<Long> productosConSerial(String buscar) {
    if (buscar == null || buscar.isBlank()) {
      return List.of();
    }
    String escapado =
        buscar
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return seriales.productosConSerial("%" + escapado + "%");
  }

  private DatosProductoInventario exigirProducto(Long productoId) {
    DatosProductoInventario producto = productos.porId(List.of(productoId)).get(productoId);
    if (producto == null) {
      throw new RecursoNoEncontradoException("El producto no existe.");
    }
    return producto;
  }

  private Map<Long, String> nombres(Collection<Long> ids) {
    List<Long> conValor = ids.stream().filter(Objects::nonNull).distinct().toList();
    return conValor.isEmpty() ? Map.of() : usuarios.nombres(conValor);
  }

  private static SerialVista vista(Serial serial, Map<Long, DatosProductoInventario> datos) {
    DatosProductoInventario producto = datos.get(serial.getProductoId());
    return new SerialVista(
        serial.getId(),
        serial.getNumero(),
        serial.getEstado().name(),
        new ProductoReferencia(producto.id(), producto.codigo(), producto.nombre()),
        serial.getFechaEntrada(),
        serial.getDocumentoEntrada(),
        serial.getDocumentoSalida(),
        serial.getVencimientoGarantia());
  }

  private static Dinero valorEnBodega(ProductoValorizado p) {
    BigDecimal costo = p.costoActualUsd() == null ? BigDecimal.ZERO : p.costoActualUsd();
    return new Dinero(Redondeo.paraAlmacenar(p.stock().multiply(costo)), Moneda.USD);
  }

  /** Avisos cuando falta una tasa o la vigente no es de hoy (RF-33). */
  private List<String> avisos(TasasAplicables aplicables) {
    LocalDate hoy = fechas.hoy();
    List<String> avisos = new ArrayList<>();
    if (aplicables.trm() == null) {
      avisos.add("No hay TRM registrada: los valores en pesos quedan vacíos.");
    } else if (!aplicables.fechaTrm().equals(hoy)) {
      avisos.add(
          "Los valores en pesos usan la TRM del "
              + FORMATO_FECHA.format(aplicables.fechaTrm())
              + ".");
    }
    if (aplicables.tasaVes() == null) {
      avisos.add("No hay tasa del bolívar registrada: los valores en bolívares quedan vacíos.");
    } else if (!aplicables.fechaTasaVes().equals(hoy)) {
      avisos.add(
          "Los valores en bolívares usan la tasa del "
              + FORMATO_FECHA.format(aplicables.fechaTasaVes())
              + ".");
    }
    return avisos;
  }

  private static Dinero usd(BigDecimal monto) {
    return monto == null ? null : new Dinero(monto, Moneda.USD);
  }
}
