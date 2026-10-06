package co.italarm.api.cotizaciones.dominio;

import co.italarm.api.shared.dominio.CopiaCliente;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Textos;
import co.italarm.api.shared.dominio.TipoDescuento;
import co.italarm.api.shared.dominio.TipoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cotización de venta o de instalación (sección 3.11). Guarda las tasas de su fecha (RN-04), el
 * costo de cada producto al cotizar y la utilidad estimada. No aparta ni descuenta material
 * (RN-11).
 *
 * <p>Estados y transiciones (BP-05):
 *
 * <ul>
 *   <li>Borrador → En evaluación al enviarla.
 *   <li>Borrador o En evaluación → Aprobada (P-48).
 *   <li>Borrador, En evaluación o Aprobada → Rechazada.
 *   <li>Borrador o En evaluación → Vencida desde el día siguiente al vencimiento (RN-13, P-47).
 *   <li>Aprobada → Convertida al guardar la venta o instalación (RF-95); vuelve a Aprobada si se
 *       anula ese documento (RF-74).
 * </ul>
 *
 * Rechazada y Vencida no se reabren: se duplican (P-48).
 */
@Entity
@Table(name = "cotizacion")
public class Cotizacion extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "numero", nullable = false, updatable = false)
  private Long numero;

  @Column(name = "numero_version", nullable = false)
  private int numeroVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 15)
  private TipoCotizacion tipo;

  @Column(name = "fecha", nullable = false)
  private LocalDate fecha;

  @Column(name = "validez_dias", nullable = false)
  private int validezDias;

  @Column(name = "vence", nullable = false)
  private LocalDate vence;

  @Column(name = "cliente_id", nullable = false)
  private Long clienteId;

  @Column(name = "cliente_tipo", nullable = false, length = 15)
  private String clienteTipo;

  @Column(name = "cliente_nombre", nullable = false, length = 150)
  private String clienteNombre;

  @Column(name = "cliente_documento", length = 40)
  private String clienteDocumento;

  @Column(name = "cliente_telefono", length = 20)
  private String clienteTelefono;

  @Column(name = "cliente_direccion", length = 200)
  private String clienteDireccion;

  @Column(name = "cliente_ciudad", length = 80)
  private String clienteCiudad;

  @Enumerated(EnumType.STRING)
  @Column(name = "moneda", nullable = false, length = 3)
  private Moneda moneda;

  @Column(name = "trm", precision = 19, scale = 6)
  private BigDecimal trm;

  @Column(name = "fecha_trm")
  private LocalDate fechaTrm;

  @Column(name = "tasa_ves", precision = 19, scale = 6)
  private BigDecimal tasaVes;

  @Column(name = "fecha_tasa_ves")
  private LocalDate fechaTasaVes;

  @Column(name = "descripcion", length = 2000)
  private String descripcion;

  @Column(name = "material", nullable = false, precision = 19, scale = 4)
  private BigDecimal material;

  @Column(name = "mano_obra", nullable = false, precision = 19, scale = 4)
  private BigDecimal manoObra;

  @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
  private BigDecimal subtotal;

  @Enumerated(EnumType.STRING)
  @Column(name = "descuento_tipo", nullable = false, length = 10)
  private TipoDescuento descuentoTipo;

  @Column(name = "descuento_valor", nullable = false, precision = 19, scale = 4)
  private BigDecimal descuentoValor;

  @Column(name = "descuento", nullable = false, precision = 19, scale = 4)
  private BigDecimal descuento;

  @Column(name = "total", nullable = false, precision = 19, scale = 4)
  private BigDecimal total;

  @Column(name = "costo", nullable = false, precision = 19, scale = 4)
  private BigDecimal costo;

  @Column(name = "utilidad", nullable = false, precision = 19, scale = 4)
  private BigDecimal utilidad;

  @Column(name = "porcentaje_utilidad", precision = 9, scale = 2)
  private BigDecimal porcentajeUtilidad;

  @Column(name = "total_usd", nullable = false, precision = 19, scale = 4)
  private BigDecimal totalUsd;

  @Column(name = "costo_usd", nullable = false, precision = 19, scale = 4)
  private BigDecimal costoUsd;

  @Column(name = "utilidad_usd", nullable = false, precision = 19, scale = 4)
  private BigDecimal utilidadUsd;

  @Column(name = "observaciones", length = 500)
  private String observaciones;

  @Column(name = "monedas_comprobante", length = 20)
  private String monedasComprobante;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 15)
  private EstadoCotizacion estado;

  @Column(name = "enviada_en")
  private Instant enviadaEn;

  @Column(name = "aprobada_en")
  private Instant aprobadaEn;

  @Column(name = "rechazada_en")
  private Instant rechazadaEn;

  @Enumerated(EnumType.STRING)
  @Column(name = "motivo_rechazo", length = 15)
  private MotivoRechazo motivoRechazo;

  @Column(name = "detalle_rechazo", length = 300)
  private String detalleRechazo;

  @Column(name = "vencida_el")
  private LocalDate vencidaEl;

  @Column(name = "documento_id")
  private Long documentoId;

  @Column(name = "documento_numero", length = 20)
  private String documentoNumero;

  @Column(name = "convertida_en")
  private Instant convertidaEn;

  @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private List<LineaCotizacion> lineas = new ArrayList<>();

  protected Cotizacion() {}

  /** Tasas de la fecha de la cotización (RN-04, P-49). */
  public record TasasCotizacion(
      BigDecimal trm, LocalDate fechaTrm, BigDecimal tasaVes, LocalDate fechaTasaVes) {}

  /**
   * Lo que se cotiza: igual al crear, al editar y al duplicar.
   *
   * @param fecha siempre hoy (P-49)
   * @param descripcion descripción del trabajo, en las de instalación (RF-83)
   * @param monedasComprobante otras monedas en que el PDF muestra los totales (como P-34)
   */
  public record Contenido(
      TipoCotizacion tipo,
      LocalDate fecha,
      int validezDias,
      CopiaCliente cliente,
      Moneda moneda,
      TasasCotizacion tasas,
      String descripcion,
      Descuento descuento,
      ResumenDocumento resumen,
      String observaciones,
      Set<Moneda> monedasComprobante,
      List<LineaCotizacion> lineas) {}

  /** Crea la cotización en Borrador (RF-80). */
  public static Cotizacion registrar(long numero, Contenido contenido) {
    Cotizacion cotizacion = new Cotizacion();
    cotizacion.numero = numero;
    cotizacion.numeroVersion = 1;
    cotizacion.estado = EstadoCotizacion.BORRADOR;
    cotizacion.aplicar(contenido);
    return cotizacion;
  }

  /**
   * Edita la cotización (RF-88, P-46): en Borrador se reemplaza su contenido; en En evaluación se
   * guarda como nueva versión y se devuelve la copia de la anterior para conservarla. En los demás
   * estados no se edita.
   *
   * @return la versión anterior, o vacío si estaba en Borrador
   */
  public VersionCotizacion editar(Contenido contenido) {
    VersionCotizacion anterior = null;
    if (estado == EstadoCotizacion.EN_EVALUACION) {
      anterior = new VersionCotizacion(id, numeroVersion, copia());
      numeroVersion++;
    } else if (estado != EstadoCotizacion.BORRADOR) {
      throw new TransicionNoPermitidaException("editar", estado);
    }
    aplicar(contenido);
    return anterior;
  }

  private void aplicar(Contenido contenido) {
    ReglasCotizacion.validarValidez(contenido.validezDias());
    ReglasCotizacion.validarContenido(
        contenido.tipo(),
        contenido.lineas().size(),
        contenido.resumen().manoDeObra(),
        contenido.descripcion());
    tipo = contenido.tipo();
    fecha = contenido.fecha();
    validezDias = contenido.validezDias();
    vence = ReglasCotizacion.vencimiento(fecha, validezDias);
    CopiaCliente cliente = contenido.cliente();
    clienteId = cliente.id();
    clienteTipo = cliente.tipo();
    clienteNombre = cliente.nombre();
    clienteDocumento = cliente.documento();
    clienteTelefono = cliente.telefono();
    clienteDireccion = cliente.direccion();
    clienteCiudad = cliente.ciudad();
    moneda = contenido.moneda();
    TasasCotizacion tasas = contenido.tasas();
    trm = tasas.trm();
    fechaTrm = tasas.fechaTrm();
    tasaVes = tasas.tasaVes();
    fechaTasaVes = tasas.fechaTasaVes();
    descripcion = tipo == TipoCotizacion.INSTALACION ? textoLargo(contenido.descripcion()) : null;
    descuentoTipo = contenido.descuento().tipo();
    descuentoValor = contenido.descuento().valor();
    ResumenDocumento resumen = contenido.resumen();
    material = resumen.material();
    manoObra = resumen.manoDeObra();
    subtotal = resumen.subtotal();
    descuento = resumen.descuento();
    total = resumen.total();
    costo = resumen.costo();
    utilidad = resumen.utilidad();
    porcentajeUtilidad = resumen.porcentajeUtilidad();
    totalUsd = resumen.totalUsd();
    costoUsd = resumen.costoUsd();
    utilidadUsd = resumen.utilidadUsd();
    observaciones = Textos.limpiar(contenido.observaciones());
    EnumSet<Moneda> adicionales = EnumSet.noneOf(Moneda.class);
    if (contenido.monedasComprobante() != null) {
      adicionales.addAll(contenido.monedasComprobante());
    }
    adicionales.remove(moneda);
    monedasComprobante =
        adicionales.isEmpty()
            ? null
            : adicionales.stream().map(Moneda::name).collect(Collectors.joining(","));
    reemplazarLineas(contenido.lineas());
  }

  /**
   * Conserva la fila de los productos que siguen (así no choca con la restricción de un producto
   * por cotización), agrega los nuevos y quita los que ya no están.
   */
  private void reemplazarLineas(List<LineaCotizacion> nuevas) {
    Map<Long, LineaCotizacion> actuales =
        lineas.stream()
            .collect(Collectors.toMap(LineaCotizacion::getProductoId, Function.identity()));
    Set<Long> siguen =
        nuevas.stream().map(LineaCotizacion::getProductoId).collect(Collectors.toSet());
    lineas.removeIf(linea -> !siguen.contains(linea.getProductoId()));
    for (LineaCotizacion nueva : nuevas) {
      LineaCotizacion actual = actuales.get(nueva.getProductoId());
      if (actual != null) {
        actual.actualizarDesde(nueva);
      } else {
        nueva.asignarCotizacion(this);
        lineas.add(nueva);
      }
    }
  }

  private ContenidoVersion copia() {
    return new ContenidoVersion(
        fecha,
        validezDias,
        vence,
        moneda,
        descripcion,
        manoObra,
        subtotal,
        descuento,
        total,
        observaciones,
        lineas.stream()
            .map(
                l ->
                    new ContenidoVersion.Linea(
                        l.getProductoId(),
                        l.getCodigo(),
                        l.getDescripcion(),
                        l.getUnidad(),
                        l.getCantidad(),
                        l.getPrecioUnitario(),
                        l.getSubtotal()))
            .toList());
  }

  /**
   * Se envió al cliente (sección 3.11, P-50): pasa de Borrador a En evaluación. Reenviar una que ya
   * está en evaluación (por ejemplo, una nueva versión) solo actualiza la fecha de envío.
   */
  public void enviar(Instant ahora) {
    if (estado != EstadoCotizacion.BORRADOR && estado != EstadoCotizacion.EN_EVALUACION) {
      throw new TransicionNoPermitidaException("enviar", estado);
    }
    estado = EstadoCotizacion.EN_EVALUACION;
    enviadaEn = ahora;
  }

  /** "Cliente aprobó" (RF-93), también sin haberla enviado (P-48). */
  public void aprobar(Instant ahora) {
    if (estado != EstadoCotizacion.BORRADOR && estado != EstadoCotizacion.EN_EVALUACION) {
      throw new TransicionNoPermitidaException("aprobar", estado);
    }
    estado = EstadoCotizacion.APROBADA;
    aprobadaEn = ahora;
  }

  /** El cliente no aceptó; motivo y detalle opcionales (P-51). */
  public void rechazar(MotivoRechazo motivo, String detalle, Instant ahora) {
    if (estado != EstadoCotizacion.BORRADOR
        && estado != EstadoCotizacion.EN_EVALUACION
        && estado != EstadoCotizacion.APROBADA) {
      throw new TransicionNoPermitidaException("rechazar", estado);
    }
    estado = EstadoCotizacion.RECHAZADA;
    motivoRechazo = motivo;
    detalleRechazo = Textos.limpiar(detalle);
    rechazadaEn = ahora;
  }

  /**
   * Tarea diaria (RN-13, P-47): una cotización en Borrador o En evaluación cuyo vencimiento ya pasó
   * queda Vencida.
   *
   * @return si quedó vencida
   */
  public boolean vencerSiCorresponde(LocalDate hoy) {
    boolean corresponde =
        (estado == EstadoCotizacion.BORRADOR || estado == EstadoCotizacion.EN_EVALUACION)
            && ReglasCotizacion.vencida(vence, hoy);
    if (corresponde) {
      estado = EstadoCotizacion.VENCIDA;
      vencidaEl = hoy;
    }
    return corresponde;
  }

  /**
   * Verifica que se pueda convertir en el documento pedido (RF-93, RN-14, P-54): debe estar
   * Aprobada, y el documento debe ser del mismo tipo y del mismo cliente.
   */
  public void validarConversion(TipoCotizacion tipoDocumento, Long clienteDocumento) {
    if (estado == EstadoCotizacion.CONVERTIDA) {
      throw new CotizacionNoConvertibleException(
          "La cotización " + consecutivo() + " ya fue convertida.");
    }
    if (estado != EstadoCotizacion.APROBADA) {
      throw new CotizacionNoConvertibleException(
          "La cotización "
              + consecutivo()
              + " está "
              + estado.descripcion()
              + ": márcala como aprobada antes de convertirla.");
    }
    if (tipoDocumento != tipo) {
      throw new CotizacionNoConvertibleException(
          "La cotización "
              + consecutivo()
              + " es de "
              + (tipo == TipoCotizacion.VENTA ? "venta de material" : "instalación")
              + ".");
    }
    if (!Objects.equals(clienteDocumento, clienteId)) {
      throw new CotizacionNoConvertibleException(
          "La cotización " + consecutivo() + " es de otro cliente.");
    }
  }

  /**
   * Queda Convertida y enlazada al documento generado (RF-95).
   *
   * @param documentoNumero consecutivo del documento, por ejemplo "I-0007"
   */
  public void convertir(
      TipoCotizacion tipoDocumento,
      Long clienteDocumento,
      Long documentoId,
      String documentoNumero,
      Instant ahora) {
    validarConversion(tipoDocumento, clienteDocumento);
    estado = EstadoCotizacion.CONVERTIDA;
    this.documentoId = documentoId;
    this.documentoNumero = documentoNumero;
    convertidaEn = ahora;
  }

  /**
   * Se anuló el documento generado (RF-74, CP-24): vuelve a Aprobada para convertirla de nuevo si
   * se requiere. Si no estaba convertida en ese documento, no cambia.
   */
  public void revertirConversion(Long documentoAnulado) {
    if (estado == EstadoCotizacion.CONVERTIDA && Objects.equals(documentoId, documentoAnulado)) {
      estado = EstadoCotizacion.APROBADA;
      documentoId = null;
      documentoNumero = null;
      convertidaEn = null;
    }
  }

  /** "COT-0001", o "COT-0001 v2" desde la segunda versión (P-46). */
  public String consecutivo() {
    String base = TipoDocumento.COTIZACION.consecutivo(numero);
    return numeroVersion > 1 ? base + " v" + numeroVersion : base;
  }

  /** "COT-0001", sin la versión: para el nombre del archivo y los enlaces. */
  public String consecutivoBase() {
    return TipoDocumento.COTIZACION.consecutivo(numero);
  }

  public Set<Moneda> getMonedasComprobante() {
    EnumSet<Moneda> monedas = EnumSet.noneOf(Moneda.class);
    if (monedasComprobante != null) {
      Arrays.stream(monedasComprobante.split(",")).map(Moneda::valueOf).forEach(monedas::add);
    }
    return monedas;
  }

  public CopiaCliente getCliente() {
    return new CopiaCliente(
        clienteId,
        clienteTipo,
        clienteNombre,
        clienteDocumento,
        clienteTelefono,
        clienteDireccion,
        clienteCiudad);
  }

  public TasasCotizacion getTasas() {
    return new TasasCotizacion(trm, fechaTrm, tasaVes, fechaTasaVes);
  }

  public ResumenDocumento getResumen() {
    return new ResumenDocumento(
        material,
        manoObra,
        subtotal,
        descuento,
        total,
        costo,
        utilidad,
        porcentajeUtilidad,
        totalUsd,
        costoUsd,
        utilidadUsd);
  }

  private static String textoLargo(String texto) {
    if (texto == null) {
      return null;
    }
    String limpio = texto.strip();
    return limpio.isEmpty() ? null : limpio;
  }

  public Long getId() {
    return id;
  }

  public Long getNumero() {
    return numero;
  }

  public int getNumeroVersion() {
    return numeroVersion;
  }

  public TipoCotizacion getTipo() {
    return tipo;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public int getValidezDias() {
    return validezDias;
  }

  public LocalDate getVence() {
    return vence;
  }

  public Long getClienteId() {
    return clienteId;
  }

  public Moneda getMoneda() {
    return moneda;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public TipoDescuento getDescuentoTipo() {
    return descuentoTipo;
  }

  public BigDecimal getDescuentoValor() {
    return descuentoValor;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public EstadoCotizacion getEstado() {
    return estado;
  }

  public Instant getEnviadaEn() {
    return enviadaEn;
  }

  public Instant getAprobadaEn() {
    return aprobadaEn;
  }

  public Instant getRechazadaEn() {
    return rechazadaEn;
  }

  public MotivoRechazo getMotivoRechazo() {
    return motivoRechazo;
  }

  public String getDetalleRechazo() {
    return detalleRechazo;
  }

  public LocalDate getVencidaEl() {
    return vencidaEl;
  }

  public Long getDocumentoId() {
    return documentoId;
  }

  public String getDocumentoNumero() {
    return documentoNumero;
  }

  public Instant getConvertidaEn() {
    return convertidaEn;
  }

  public List<LineaCotizacion> getLineas() {
    return lineas;
  }
}
