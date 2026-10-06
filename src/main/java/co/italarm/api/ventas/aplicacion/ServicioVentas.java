package co.italarm.api.ventas.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.comercial.aplicacion.MaterialPreparado;
import co.italarm.api.comercial.aplicacion.OrigenCotizacion;
import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.comercial.aplicacion.VistaPreviaMaterial;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.EnlaceCreado;
import co.italarm.api.documentos.aplicacion.ServicioEnlacesComprobante;
import co.italarm.api.inventario.aplicacion.LineaAnulacion;
import co.italarm.api.inventario.aplicacion.SerialSalida;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.ClienteNoExisteException;
import co.italarm.api.shared.dominio.CopiaCliente;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ClienteDocumento;
import co.italarm.api.terceros.aplicacion.ConsultaClientes;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import co.italarm.api.ventas.dominio.LineaVenta;
import co.italarm.api.ventas.dominio.Venta;
import co.italarm.api.ventas.infraestructura.EspecificacionesVentas;
import co.italarm.api.ventas.infraestructura.LineaVentaRepositorio;
import co.italarm.api.ventas.infraestructura.TotalesVentas;
import co.italarm.api.ventas.infraestructura.VentaRepositorio;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ventas de material (sección 3.12) y su anulación (RF-72, RF-73). */
@Service
public class ServicioVentas {

  private final VentaRepositorio ventas;
  private final LineaVentaRepositorio lineas;
  private final TotalesVentas totales;
  private final RegistroVentas registro;
  private final ServicioMovimientos movimientos;
  private final ConsultaProductos productos;
  private final ConsultaClientes clientes;
  private final VistaPreviaMaterial vistaPreviaMaterial;
  private final ServicioTasas tasas;
  private final ServicioIdempotencia idempotencia;
  private final ConsultaUsuarios usuarios;
  private final ComprobantesVenta comprobantes;
  private final ServicioEnlacesComprobante enlaces;
  private final OrigenCotizacion origen;
  private final FechaNegocio fechas;

  public ServicioVentas(
      VentaRepositorio ventas,
      LineaVentaRepositorio lineas,
      TotalesVentas totales,
      RegistroVentas registro,
      ServicioMovimientos movimientos,
      ConsultaProductos productos,
      ConsultaClientes clientes,
      VistaPreviaMaterial vistaPreviaMaterial,
      ServicioTasas tasas,
      ServicioIdempotencia idempotencia,
      ConsultaUsuarios usuarios,
      ComprobantesVenta comprobantes,
      ServicioEnlacesComprobante enlaces,
      OrigenCotizacion origen,
      FechaNegocio fechas) {
    this.ventas = ventas;
    this.lineas = lineas;
    this.totales = totales;
    this.registro = registro;
    this.movimientos = movimientos;
    this.productos = productos;
    this.clientes = clientes;
    this.vistaPreviaMaterial = vistaPreviaMaterial;
    this.tasas = tasas;
    this.idempotencia = idempotencia;
    this.usuarios = usuarios;
    this.comprobantes = comprobantes;
    this.enlaces = enlaces;
    this.origen = origen;
    this.fechas = fechas;
  }

  /**
   * Precios, disponibilidad, costos con las tasas de hoy y de la última compra, y resumen con
   * utilidad, sin guardar nada (RF-98 a RF-101, RF-69).
   */
  @Transactional(readOnly = true)
  public VistaPreviaVentaVista vistaPrevia(DatosVenta datos) {
    RegistroVentas.validarLineas(datos.lineas());
    ClienteDocumento cliente =
        clientes.porId(datos.clienteId()).orElseThrow(ClienteNoExisteException::new);
    Descuento descuento = Descuento.de(datos.descuentoTipo(), datos.descuentoValor());
    LocalDate hoy = fechas.hoy();
    TasasAplicables aplicables = tasas.tasasPara(hoy);
    Tasas conversion = PreparacionMaterial.conversion(aplicables);
    Moneda moneda = datos.moneda();
    conversion.aUsd(BigDecimal.ONE, moneda);

    List<Long> ids = datos.lineas().stream().map(LineaMaterial::productoId).toList();
    Map<Long, ProductoValorizado> valorizados = productos.valorizadosPorId(ids);
    Map<Long, BigDecimal> costos = new HashMap<>();
    valorizados.forEach((id, p) -> costos.put(id, p.costoActualUsd()));
    List<MaterialPreparado> preparadas =
        PreparacionMaterial.lineas(
            datos.lineas(), valorizados, costos, cliente.precioInstalador(), moneda, conversion);
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            preparadas.stream().map(MaterialPreparado::calculo).toList(),
            descuento,
            moneda,
            conversion);
    VistaPreviaMaterial.Resultado material =
        vistaPreviaMaterial.construir(preparadas, moneda, conversion);
    return new VistaPreviaVentaVista(
        hoy,
        PreparacionMaterial.clienteVista(cliente),
        moneda,
        PreparacionMaterial.tasasVista(aplicables),
        PreparacionMaterial.avisos(hoy, aplicables),
        material.lineas(),
        PreparacionMaterial.resumenVista(resumen, moneda, conversion),
        material.puedeGuardar());
  }

  /**
   * Registra la venta una sola vez por {@code Idempotency-Key} (RT-07) y devuelve su id. La
   * transacción la abre {@link RegistroVentas}.
   */
  public Long registrar(DatosVenta datos, Long usuarioId, ClaveIdempotencia clave) {
    return idempotencia.ejecutar(clave, () -> registro.registrar(datos, usuarioId, clave));
  }

  /** Listado del período (por defecto, el mes en curso) con totales sin las anuladas (RF-105). */
  @Transactional(readOnly = true)
  public ListadoVentasVista listar(
      Long clienteId,
      Long productoId,
      LocalDate desde,
      LocalDate hasta,
      Boolean incluirAnuladas,
      Pageable pagina) {
    LocalDate hoy = fechas.hoy();
    LocalDate inicio = desde != null ? desde : hoy.withDayOfMonth(1);
    LocalDate fin = hasta != null ? hasta : hoy.with(TemporalAdjusters.lastDayOfMonth());
    Page<Venta> encontradas =
        ventas.findAll(
            EspecificacionesVentas.filtrar(
                clienteId, productoId, inicio, fin, !Boolean.FALSE.equals(incluirAnuladas)),
            pagina);
    Map<Long, List<LineaVenta>> lineasPorVenta = lineasDe(encontradas.getContent());
    Map<Long, String> nombres =
        usuarios.nombres(
            encontradas.getContent().stream()
                .map(Venta::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    Pagina<VentaResumenVista> contenido =
        Pagina.de(
            encontradas,
            v ->
                new VentaResumenVista(
                    v.getId(),
                    v.consecutivo(),
                    v.getFecha(),
                    v.getClienteId(),
                    v.getCliente().nombre(),
                    v.getMoneda(),
                    new Dinero(v.getTotal(), v.getMoneda()),
                    usd(v.getTotalUsd()),
                    new Dinero(v.getUtilidad(), v.getMoneda()),
                    v.getEstado().name(),
                    resumenProductos(lineasPorVenta.getOrDefault(v.getId(), List.of())),
                    nombres.get(v.getCreatedBy()),
                    v.getCreatedAt()));

    List<TotalesVentas.TotalPorMoneda> porMoneda =
        totales.porMoneda(
            EspecificacionesVentas.filtrar(clienteId, productoId, inicio, fin, false));
    return new ListadoVentasVista(
        inicio,
        fin,
        contenido,
        porMoneda.stream()
            .map(
                t ->
                    new ListadoVentasVista.TotalMoneda(
                        new Dinero(t.total(), t.moneda()),
                        new Dinero(t.costo(), t.moneda()),
                        new Dinero(t.utilidad(), t.moneda()),
                        t.ventas()))
            .toList(),
        usd(sumar(porMoneda.stream().map(TotalesVentas.TotalPorMoneda::totalUsd).toList())),
        usd(sumar(porMoneda.stream().map(TotalesVentas.TotalPorMoneda::utilidadUsd).toList())));
  }

  @Transactional(readOnly = true)
  public VentaVista detalle(Long id) {
    Venta venta =
        ventas
            .findConLineasById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe."));
    Map<Long, List<SerialSalida>> seriales = movimientos.serialesDeSalida(documento(venta));
    Map<Long, String> nombres =
        usuarios.nombres(
            Stream.of(venta.getCreatedBy(), venta.getAnuladaPor())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    Moneda moneda = venta.getMoneda();
    Tasas conversion = new Tasas(venta.getTasas().trm(), venta.getTasas().tasaVes());
    ResumenDocumento resumen =
        new ResumenDocumento(
            venta.getSubtotal(),
            BigDecimal.ZERO,
            venta.getSubtotal(),
            venta.getDescuento(),
            venta.getTotal(),
            venta.getCosto(),
            venta.getUtilidad(),
            venta.getPorcentajeUtilidad(),
            venta.getTotalUsd(),
            venta.getCostoUsd(),
            venta.getUtilidadUsd());
    CopiaCliente cliente = venta.getCliente();
    return new VentaVista(
        venta.getId(),
        venta.consecutivo(),
        venta.getFecha(),
        PreparacionMaterial.clienteVista(cliente),
        moneda,
        new TasasDocumentoVista(
            venta.getTasas().trm(),
            venta.getTasas().fechaTrm(),
            venta.getTasas().tasaVes(),
            venta.getTasas().fechaTasaVes()),
        venta.getLineas().stream()
            .map(
                l ->
                    new VentaVista.Linea(
                        l.getProductoId(),
                        l.getCodigo(),
                        l.getDescripcion(),
                        l.getUnidad(),
                        PreparacionMaterial.cantidadVista(l.getCantidad()),
                        new Dinero(l.getPrecioUnitario(), moneda),
                        new Dinero(l.getPrecioSugerido(), moneda),
                        new Dinero(l.getSubtotal(), moneda),
                        usd(l.getCostoUnitarioUsd()),
                        seriales.getOrDefault(l.getProductoId(), List.of()).stream()
                            .map(
                                s ->
                                    new VentaVista.SerialVendido(
                                        s.id(), s.numero(), s.vencimientoGarantia()))
                            .toList()))
            .toList(),
        venta.getDescuentoTipo().name(),
        venta.getDescuentoValor(),
        PreparacionMaterial.resumenVista(resumen, moneda, conversion),
        new Dinero(venta.getTotal(), moneda),
        new Dinero(venta.getUtilidad(), moneda),
        venta.getPorcentajeUtilidad(),
        venta.getObservaciones(),
        venta.getMonedasComprobante(),
        origen.origen(venta.getCotizacionId()).orElse(null),
        venta.getEstado().name(),
        venta.estaAnulada()
            ? new VentaVista.Anulacion(
                venta.getMotivoAnulacion(),
                nombres.get(venta.getAnuladaPor()),
                venta.getAnuladaEn())
            : null,
        nombres.get(venta.getCreatedBy()),
        venta.getCreatedAt(),
        venta.getVersion());
  }

  /** Cambia las observaciones y las monedas adicionales del comprobante (RF-70, P-34). */
  @Transactional
  public VentaVista actualizar(
      Long id, String observaciones, Set<Moneda> monedasComprobante, long version) {
    Venta venta = buscar(id);
    venta.verificarVersion(version);
    venta.cambiarDescripcion(observaciones, monedasComprobante);
    ventas.flush();
    return detalle(id);
  }

  /**
   * Anula la venta (RF-72, RF-73): el material vuelve al costo vigente y los seriales a bodega. No
   * hay plazo para anular (P-31).
   */
  @Transactional
  public VentaVista anular(Long id, String motivo, Long usuarioId) {
    Venta venta =
        ventas
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe."));
    if (!venta.estaAnulada()) {
      movimientos.anularSalida(
          documento(venta),
          fechas.hoy(),
          venta.getLineas().stream()
              .map(l -> new LineaAnulacion(l.getProductoId(), l.getCantidad()))
              .toList(),
          usuarioId);
    }
    if (!venta.estaAnulada() && venta.getCotizacionId() != null) {
      origen.revertir(venta.getCotizacionId(), venta.getId());
    }
    venta.anular(motivo, usuarioId, fechas.ahora());
    ventas.flush();
    return detalle(id);
  }

  /** PDF del comprobante para descargar con sesión (RF-133). */
  public ArchivoGenerado comprobante(Long id) {
    return comprobantes.pdf(id);
  }

  /**
   * Enlace público del comprobante, con el mensaje y el enlace de WhatsApp al número del cliente
   * (RF-134). En el celular, el frontend comparte el PDF descargado.
   */
  @Transactional
  public EnlaceComprobanteVista crearEnlace(Long id, Long usuarioId) {
    Venta venta = buscar(id);
    EnlaceCreado enlace = enlaces.crear(TipoDocumento.VENTA, venta.getId(), usuarioId);
    String mensaje =
        "Hola "
            + venta.getCliente().nombre()
            + ", te compartimos el comprobante de venta "
            + venta.consecutivo()
            + " de ITALARM: "
            + enlace.url();
    String telefono = venta.getCliente().telefono();
    String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
    return new EnlaceComprobanteVista(
        enlace.url(),
        enlace.venceEn(),
        mensaje,
        digitos.isEmpty()
            ? null
            : "https://wa.me/"
                + digitos
                + "?text="
                + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
  }

  private Venta buscar(Long id) {
    return ventas
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe."));
  }

  private Map<Long, List<LineaVenta>> lineasDe(List<Venta> encontradas) {
    List<Long> ids = encontradas.stream().map(Venta::getId).toList();
    return ids.isEmpty()
        ? Map.of()
        : lineas.deVentas(ids).stream().collect(Collectors.groupingBy(LineaVenta::getVentaId));
  }

  static String resumenProductos(List<LineaVenta> lineasVenta) {
    return PreparacionMaterial.resumenTexto(
        lineasVenta.stream()
            .map(
                l ->
                    PreparacionMaterial.textoLinea(
                        l.getDescripcion(), l.getCantidad(), l.getUnidad()))
            .toList());
  }

  static DocumentoRef documento(Venta venta) {
    return new DocumentoRef(TipoDocumento.VENTA, venta.getId(), venta.consecutivo());
  }

  private static BigDecimal sumar(List<BigDecimal> valores) {
    return valores.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static Dinero usd(BigDecimal monto) {
    return monto == null ? null : new Dinero(monto, Moneda.USD);
  }
}
