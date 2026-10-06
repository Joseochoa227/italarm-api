package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.comercial.aplicacion.VistaPreviaMaterial;
import co.italarm.api.cotizaciones.dominio.ContenidoVersion;
import co.italarm.api.cotizaciones.dominio.Cotizacion;
import co.italarm.api.cotizaciones.dominio.EstadoCotizacion;
import co.italarm.api.cotizaciones.dominio.LineaCotizacion;
import co.italarm.api.cotizaciones.dominio.MotivoRechazo;
import co.italarm.api.cotizaciones.dominio.ReglasCotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.cotizaciones.dominio.VersionCotizacion;
import co.italarm.api.cotizaciones.infraestructura.CotizacionRepositorio;
import co.italarm.api.cotizaciones.infraestructura.EspecificacionesCotizaciones;
import co.italarm.api.cotizaciones.infraestructura.VersionCotizacionRepositorio;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.EnlaceCreado;
import co.italarm.api.documentos.aplicacion.ServicioEnlacesComprobante;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.FormatoDinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.PrecioSugerido;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.TasaNoDisponibleException;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDescuento;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.terceros.aplicacion.ClienteDocumento;
import co.italarm.api.terceros.aplicacion.ConsultaClientes;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cotizaciones (sección 3.11): elaboración, seguimiento, PDF y datos para convertirlas. */
@Service
public class ServicioCotizaciones {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String OPERACION_DUPLICAR = "COTIZACION_DUPLICAR";

  private final CotizacionRepositorio cotizaciones;
  private final VersionCotizacionRepositorio versiones;
  private final RegistroCotizaciones registro;
  private final ConsultaProductos productos;
  private final ConsultaClientes clientes;
  private final VistaPreviaMaterial vistaPreviaMaterial;
  private final ServicioTasas tasas;
  private final ServicioIdempotencia idempotencia;
  private final ConsultaUsuarios usuarios;
  private final ComprobantesCotizacion comprobantes;
  private final ServicioEnlacesComprobante enlaces;
  private final FechaNegocio fechas;

  public ServicioCotizaciones(
      CotizacionRepositorio cotizaciones,
      VersionCotizacionRepositorio versiones,
      RegistroCotizaciones registro,
      ConsultaProductos productos,
      ConsultaClientes clientes,
      VistaPreviaMaterial vistaPreviaMaterial,
      ServicioTasas tasas,
      ServicioIdempotencia idempotencia,
      ConsultaUsuarios usuarios,
      ComprobantesCotizacion comprobantes,
      ServicioEnlacesComprobante enlaces,
      FechaNegocio fechas) {
    this.cotizaciones = cotizaciones;
    this.versiones = versiones;
    this.registro = registro;
    this.productos = productos;
    this.clientes = clientes;
    this.vistaPreviaMaterial = vistaPreviaMaterial;
    this.tasas = tasas;
    this.idempotencia = idempotencia;
    this.usuarios = usuarios;
    this.comprobantes = comprobantes;
    this.enlaces = enlaces;
    this.fechas = fechas;
  }

  /**
   * Precios según el tipo de cliente, costos con las tasas de hoy y de la última compra, cobro con
   * utilidad estimada y vencimiento, sin guardar nada (RF-81, RF-84, CP-09).
   */
  @Transactional(readOnly = true)
  public VistaPreviaCotizacionVista vistaPrevia(DatosCotizacion datos) {
    RegistroCotizaciones.Preparacion preparacion = registro.preparar(datos);
    Moneda moneda = datos.moneda();
    VistaPreviaMaterial.Resultado material =
        vistaPreviaMaterial.construir(preparacion.lineas(), moneda, preparacion.conversion());
    return new VistaPreviaCotizacionVista(
        datos.tipo().name(),
        preparacion.fecha(),
        preparacion.validezDias(),
        ReglasCotizacion.vencimiento(preparacion.fecha(), preparacion.validezDias()),
        PreparacionMaterial.clienteVista(preparacion.cliente()),
        moneda,
        PreparacionMaterial.tasasVista(preparacion.tasas()),
        PreparacionMaterial.avisos(preparacion.fecha(), preparacion.tasas()),
        material.lineas(),
        PreparacionMaterial.resumenVista(preparacion.resumen(), moneda, preparacion.conversion()));
  }

  /**
   * Registra la cotización en Borrador una sola vez por {@code Idempotency-Key} (RT-07) y devuelve
   * su id. La transacción la abre {@link RegistroCotizaciones}.
   */
  public Long registrar(DatosCotizacion datos, ClaveIdempotencia clave) {
    return idempotencia.ejecutar(clave, () -> registro.registrar(datos, clave));
  }

  /**
   * Crea una cotización nueva en Borrador con los mismos productos, cantidades, precios cotizados,
   * mano de obra y descripción, con fecha y tasas de hoy (RF-87, P-52).
   */
  public Long duplicar(Long id, Long usuarioId, String claveIdempotencia) {
    DatosCotizacion datos = datosDe(id);
    ClaveIdempotencia clave =
        claveIdempotencia == null
            ? null
            : new ClaveIdempotencia(usuarioId, OPERACION_DUPLICAR + "_" + id, claveIdempotencia);
    return idempotencia.ejecutar(clave, () -> registro.registrar(datos, clave));
  }

  @Transactional(readOnly = true)
  public DatosCotizacion datosDe(Long id) {
    Cotizacion cotizacion = buscarConLineas(id);
    return new DatosCotizacion(
        cotizacion.getTipo(),
        cotizacion.getClienteId(),
        cotizacion.getValidezDias(),
        cotizacion.getMoneda(),
        cotizacion.getLineas().stream()
            .map(
                l ->
                    new LineaMaterial(
                        l.getProductoId(), l.getCantidad(), List.of(), l.getPrecioUnitario()))
            .toList(),
        cotizacion.getResumen().manoDeObra(),
        cotizacion.getDescripcion(),
        cotizacion.getDescuentoTipo(),
        cotizacion.getDescuentoValor(),
        cotizacion.getObservaciones(),
        cotizacion.getMonedasComprobante());
  }

  /** Listado (RF-89, RF-90), más recientes primero. Sin fechas, todas. */
  @Transactional(readOnly = true)
  public Pagina<CotizacionResumenVista> listar(
      EstadoCotizacion estado,
      Long clienteId,
      TipoCotizacion tipo,
      LocalDate desde,
      LocalDate hasta,
      Boolean porVencer,
      Pageable pagina) {
    LocalDate hoy = fechas.hoy();
    Page<Cotizacion> encontradas =
        cotizaciones.findAll(
            EspecificacionesCotizaciones.filtrar(
                estado, clienteId, tipo, desde, hasta, Boolean.TRUE.equals(porVencer), hoy),
            pagina);
    Map<Long, String> nombres =
        nombres(
            encontradas.getContent().stream()
                .map(Cotizacion::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    return Pagina.de(
        encontradas,
        c ->
            new CotizacionResumenVista(
                c.getId(),
                c.consecutivo(),
                c.getTipo().name(),
                c.getFecha(),
                c.getClienteId(),
                c.getCliente().nombre(),
                c.getMoneda(),
                new Dinero(c.getResumen().total(), c.getMoneda()),
                usd(c.getResumen().totalUsd()),
                c.getEstado().name(),
                c.getVence(),
                diasParaVencer(c, hoy),
                ReglasCotizacion.porVencer(c.getEstado(), c.getVence(), hoy),
                c.getDocumentoNumero(),
                nombres.get(c.getCreatedBy()),
                c.getCreatedAt()));
  }

  @Transactional(readOnly = true)
  public CotizacionVista detalle(Long id) {
    Cotizacion cotizacion = buscarConLineas(id);
    LocalDate hoy = fechas.hoy();
    List<VersionCotizacion> anteriores = versiones.findByCotizacionIdOrderByNumeroVersionDesc(id);
    Map<Long, String> nombres =
        nombres(
            Stream.concat(
                    Stream.of(cotizacion.getCreatedBy()),
                    anteriores.stream().map(VersionCotizacion::getCreatedBy))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    Moneda moneda = cotizacion.getMoneda();
    Cotizacion.TasasCotizacion tasasCotizacion = cotizacion.getTasas();
    Tasas conversion = new Tasas(tasasCotizacion.trm(), tasasCotizacion.tasaVes());
    return new CotizacionVista(
        cotizacion.getId(),
        cotizacion.consecutivo(),
        cotizacion.getNumeroVersion(),
        cotizacion.getTipo().name(),
        cotizacion.getFecha(),
        cotizacion.getValidezDias(),
        cotizacion.getVence(),
        diasParaVencer(cotizacion, hoy),
        ReglasCotizacion.porVencer(cotizacion.getEstado(), cotizacion.getVence(), hoy),
        PreparacionMaterial.clienteVista(cotizacion.getCliente()),
        moneda,
        new TasasDocumentoVista(
            tasasCotizacion.trm(),
            tasasCotizacion.fechaTrm(),
            tasasCotizacion.tasaVes(),
            tasasCotizacion.fechaTasaVes()),
        cotizacion.getDescripcion(),
        cotizacion.getLineas().stream()
            .map(
                l ->
                    new CotizacionVista.Linea(
                        l.getProductoId(),
                        l.getCodigo(),
                        l.getDescripcion(),
                        l.getUnidad(),
                        PreparacionMaterial.cantidadVista(l.getCantidad()),
                        new Dinero(l.getPrecioUnitario(), moneda),
                        new Dinero(l.getPrecioSugerido(), moneda),
                        new Dinero(l.getSubtotal(), moneda),
                        usd(l.getCostoUnitarioUsd())))
            .toList(),
        cotizacion.getDescuentoTipo().name(),
        cotizacion.getDescuentoValor(),
        PreparacionMaterial.resumenVista(cotizacion.getResumen(), moneda, conversion),
        new Dinero(cotizacion.getResumen().total(), moneda),
        new Dinero(cotizacion.getResumen().utilidad(), moneda),
        cotizacion.getResumen().porcentajeUtilidad(),
        cotizacion.getObservaciones(),
        cotizacion.getMonedasComprobante(),
        cotizacion.getEstado().name(),
        cotizacion.getEnviadaEn(),
        cotizacion.getAprobadaEn(),
        cotizacion.getEstado() == EstadoCotizacion.RECHAZADA
            ? new CotizacionVista.Rechazo(
                cotizacion.getMotivoRechazo() == null ? null : cotizacion.getMotivoRechazo().name(),
                cotizacion.getDetalleRechazo(),
                cotizacion.getRechazadaEn())
            : null,
        cotizacion.getVencidaEl(),
        cotizacion.getDocumentoId() == null
            ? null
            : new CotizacionVista.DocumentoGenerado(
                cotizacion.getTipo().name(),
                cotizacion.getDocumentoId(),
                cotizacion.getDocumentoNumero(),
                cotizacion.getConvertidaEn()),
        anteriores.stream().map(v -> versionAnterior(v, nombres)).toList(),
        nombres.get(cotizacion.getCreatedBy()),
        cotizacion.getCreatedAt(),
        cotizacion.getVersion());
  }

  /** Edita la cotización (RF-88, P-46). */
  public CotizacionVista editar(Long id, DatosCotizacion datos, long version) {
    registro.editar(id, datos, version);
    return detalle(id);
  }

  /** Marca la cotización como enviada (P-50). */
  @Transactional
  public CotizacionVista enviar(Long id) {
    bloquear(id).enviar(fechas.ahora());
    cotizaciones.flush();
    return detalle(id);
  }

  /** Cliente aprobó (RF-93, P-48). */
  @Transactional
  public CotizacionVista aprobar(Long id) {
    bloquear(id).aprobar(fechas.ahora());
    cotizaciones.flush();
    return detalle(id);
  }

  /** El cliente no aceptó, con motivo y detalle opcionales (P-51). */
  @Transactional
  public CotizacionVista rechazar(Long id, MotivoRechazo motivo, String detalle) {
    bloquear(id).rechazar(motivo, detalle, fechas.ahora());
    cotizaciones.flush();
    return detalle(id);
  }

  /**
   * Vence las cotizaciones en Borrador o En evaluación cuyo vencimiento ya pasó (RN-13, P-47).
   *
   * @return cuántas quedaron vencidas
   */
  @Transactional
  public int vencer() {
    LocalDate hoy = fechas.hoy();
    int vencidas = 0;
    for (Cotizacion cotizacion :
        cotizaciones.findByEstadoInAndVenceBefore(
            List.of(EstadoCotizacion.BORRADOR, EstadoCotizacion.EN_EVALUACION), hoy)) {
      if (cotizacion.vencerSiCorresponde(hoy)) {
        vencidas++;
      }
    }
    cotizaciones.flush();
    return vencidas;
  }

  /**
   * Formulario de venta o de instalación precargado con la cotización aprobada (RF-94), con los
   * avisos de precio, costo y stock cambiados desde que se cotizó (RF-96, CP-22, CP-23).
   */
  @Transactional(readOnly = true)
  public ConversionCotizacionVista conversion(Long id) {
    Cotizacion cotizacion = buscarConLineas(id);
    cotizacion.validarConversion(cotizacion.getTipo(), cotizacion.getClienteId());
    Moneda moneda = cotizacion.getMoneda();
    ClienteDocumento cliente = clientes.porId(cotizacion.getClienteId()).orElse(null);
    boolean precioInstalador =
        cliente == null
            ? "INSTALADOR".equals(cotizacion.getCliente().tipo())
            : cliente.precioInstalador();
    Map<Long, ProductoValorizado> valorizados =
        productos.valorizadosPorId(
            cotizacion.getLineas().stream().map(LineaCotizacion::getProductoId).toList());
    Tasas hoy = PreparacionMaterial.conversion(tasas.tasasPara(fechas.hoy()));

    List<ConversionCotizacionVista.Aviso> avisos = new ArrayList<>();
    List<ConversionCotizacionVista.Linea> lineas = new ArrayList<>();
    boolean puedeGuardar = true;
    boolean sinTasas = false;
    for (LineaCotizacion linea : cotizacion.getLineas()) {
      ProductoValorizado producto = valorizados.get(linea.getProductoId());
      lineas.add(
          new ConversionCotizacionVista.Linea(
              producto.id(),
              producto.codigo(),
              producto.nombre(),
              producto.abreviatura(),
              producto.controlaSerial(),
              PreparacionMaterial.cantidadVista(linea.getCantidad()),
              linea.getPrecioUnitario(),
              producto.stock()));
      if (!producto.activo()) {
        avisos.add(
            new ConversionCotizacionVista.Aviso(
                "INACTIVO", producto.id(), producto.nombre() + " está inactivo."));
        puedeGuardar = false;
      }
      if (linea.getCantidad().compareTo(producto.stock()) > 0) {
        avisos.add(
            new ConversionCotizacionVista.Aviso(
                "STOCK",
                producto.id(),
                producto.nombre()
                    + ": stock insuficiente · quedan "
                    + producto.stock().toPlainString()
                    + " "
                    + producto.abreviatura()));
        puedeGuardar = false;
      }
      try {
        BigDecimal sugeridoHoy =
            PrecioSugerido.en(
                precioInstalador ? producto.precioInstalador() : producto.precioClienteFinal(),
                moneda,
                hoy);
        if (sugeridoHoy.compareTo(linea.getPrecioSugerido()) != 0) {
          avisos.add(
              new ConversionCotizacionVista.Aviso(
                  "PRECIO",
                  producto.id(),
                  producto.nombre()
                      + ": el precio sugerido cambió de "
                      + dinero(linea.getPrecioSugerido(), moneda)
                      + " a "
                      + dinero(sugeridoHoy, moneda)
                      + ". Se mantiene el precio cotizado de "
                      + dinero(linea.getPrecioUnitario(), moneda)
                      + "."));
        }
      } catch (TasaNoDisponibleException e) {
        sinTasas = true;
      }
      BigDecimal costoHoy =
          producto.costoActualUsd() == null ? BigDecimal.ZERO : producto.costoActualUsd();
      if (costoHoy.compareTo(linea.getCostoUnitarioUsd()) != 0) {
        avisos.add(
            new ConversionCotizacionVista.Aviso(
                "COSTO",
                producto.id(),
                producto.nombre()
                    + ": el costo cambió de "
                    + dinero(linea.getCostoUnitarioUsd(), Moneda.USD)
                    + " a "
                    + dinero(costoHoy, Moneda.USD)
                    + "."));
      }
    }
    if (sinTasas) {
      avisos.add(
          new ConversionCotizacionVista.Aviso(
              "TASA",
              null,
              "Faltan las tasas de hoy: no se pudo comparar el precio sugerido de hoy con el"
                  + " cotizado."));
    }
    BigDecimal manoDeObra = cotizacion.getResumen().manoDeObra();
    boolean conDescuento = cotizacion.getDescuentoValor().signum() > 0;
    return new ConversionCotizacionVista(
        cotizacion.getId(),
        cotizacion.consecutivo(),
        cotizacion.getTipo().name(),
        cotizacion.getClienteId(),
        cotizacion.getCliente().nombre(),
        cliente == null ? cotizacion.getCliente().direccion() : cliente.direccion(),
        moneda,
        lineas,
        cotizacion.getTipo() == TipoCotizacion.INSTALACION ? manoDeObra : null,
        cotizacion.getDescripcion(),
        conDescuento ? cotizacion.getDescuentoTipo() : null,
        conDescuento ? cotizacion.getDescuentoValor() : null,
        cotizacion.getObservaciones(),
        cotizacion.getMonedasComprobante(),
        avisos,
        puedeGuardar);
  }

  /** PDF de la cotización para descargar con sesión (RF-133). No cambia el estado (P-50). */
  public ArchivoGenerado comprobante(Long id) {
    return comprobantes.pdf(id);
  }

  /**
   * Enlace público del PDF con el mensaje y el enlace de WhatsApp (RF-134). Si la cotización estaba
   * en Borrador, queda En evaluación (P-50).
   */
  @Transactional
  public EnlaceCotizacionVista crearEnlace(Long id, Long usuarioId) {
    Cotizacion cotizacion = bloquear(id);
    if (cotizacion.getEstado() == EstadoCotizacion.BORRADOR) {
      cotizacion.enviar(fechas.ahora());
    }
    EnlaceCreado enlace = enlaces.crear(TipoDocumento.COTIZACION, cotizacion.getId(), usuarioId);
    String mensaje =
        "Hola "
            + cotizacion.getCliente().nombre()
            + ", te compartimos la cotización "
            + cotizacion.consecutivo()
            + " de ITALARM, válida hasta el "
            + FECHA.format(cotizacion.getVence())
            + ": "
            + enlace.url();
    cotizaciones.flush();
    return new EnlaceCotizacionVista(
        enlace.url(),
        enlace.venceEn(),
        mensaje,
        whatsapp(cotizacion.getCliente().telefono(), mensaje),
        cotizacion.getEstado().name());
  }

  /** Mensaje de seguimiento por WhatsApp (RF-92, P-53). */
  @Transactional(readOnly = true)
  public SeguimientoCotizacionVista seguimiento(Long id) {
    Cotizacion cotizacion = buscar(id);
    String mensaje =
        "Hola "
            + cotizacion.getCliente().nombre()
            + ", te escribimos de ITALARM para saber si pudiste revisar la cotización "
            + cotizacion.consecutivo()
            + " por "
            + dinero(cotizacion.getResumen().total(), cotizacion.getMoneda())
            + ", válida hasta el "
            + FECHA.format(cotizacion.getVence())
            + ". Quedamos atentos.";
    return new SeguimientoCotizacionVista(
        mensaje, whatsapp(cotizacion.getCliente().telefono(), mensaje));
  }

  private static CotizacionVista.VersionAnterior versionAnterior(
      VersionCotizacion version, Map<Long, String> nombres) {
    ContenidoVersion contenido = version.getContenido();
    Moneda moneda = contenido.moneda();
    return new CotizacionVista.VersionAnterior(
        version.getNumeroVersion(),
        contenido.fecha(),
        contenido.vence(),
        contenido.descripcion(),
        new Dinero(contenido.manoDeObra(), moneda),
        new Dinero(contenido.descuento(), moneda),
        new Dinero(contenido.total(), moneda),
        contenido.lineas().stream()
            .map(
                l ->
                    new CotizacionVista.LineaAnterior(
                        l.productoId(),
                        l.codigo(),
                        l.descripcion(),
                        l.unidad(),
                        PreparacionMaterial.cantidadVista(l.cantidad()),
                        new Dinero(l.precioUnitario(), moneda),
                        new Dinero(l.subtotal(), moneda)))
            .toList(),
        nombres.get(version.getCreatedBy()),
        version.getCreatedAt());
  }

  /** RF-90: solo mientras la cotización puede vencer. */
  private static Long diasParaVencer(Cotizacion cotizacion, LocalDate hoy) {
    EstadoCotizacion estado = cotizacion.getEstado();
    return estado == EstadoCotizacion.BORRADOR || estado == EstadoCotizacion.EN_EVALUACION
        ? ReglasCotizacion.diasParaVencer(cotizacion.getVence(), hoy)
        : null;
  }

  private static String whatsapp(String telefono, String mensaje) {
    String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
    return digitos.isEmpty()
        ? null
        : "https://wa.me/"
            + digitos
            + "?text="
            + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
  }

  private Cotizacion bloquear(Long id) {
    return cotizaciones
        .bloquear(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
  }

  private Cotizacion buscar(Long id) {
    return cotizaciones
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
  }

  private Cotizacion buscarConLineas(Long id) {
    return cotizaciones
        .findConLineasById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
  }

  private Map<Long, String> nombres(Collection<Long> ids) {
    return ids.isEmpty() ? Map.of() : usuarios.nombres(ids);
  }

  private static String dinero(BigDecimal monto, Moneda moneda) {
    return FormatoDinero.formatear(new Dinero(monto, moneda));
  }

  private static Dinero usd(BigDecimal monto) {
    return monto == null ? null : new Dinero(monto, Moneda.USD);
  }

  /** Descuento escrito, para el PDF. */
  static String etiquetaDescuento(TipoDescuento tipo, BigDecimal valor) {
    return tipo == TipoDescuento.PORCENTAJE
        ? "Descuento ("
            + PreparacionMaterial.cantidadVista(valor).toPlainString().replace('.', ',')
            + " %)"
        : "Descuento";
  }
}
