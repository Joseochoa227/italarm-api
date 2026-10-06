package co.italarm.api.instalaciones.dominio;

import co.italarm.api.shared.dominio.CopiaCliente;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Garantia;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Textos;
import co.italarm.api.shared.dominio.TipoDescuento;
import co.italarm.api.shared.dominio.TipoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Instalación (sección 3.13): cliente, dirección, técnicos, trabajo, material, mano de obra, cobro
 * con utilidad y garantías. Guarda las tasas de su fecha (RN-04). No se edita en valores: solo se
 * anula; los datos descriptivos sí se pueden cambiar (RF-122, P-44).
 */
@Entity
@Table(name = "instalacion")
public class Instalacion extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "numero", nullable = false, updatable = false)
  private Long numero;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "cliente_id", nullable = false, updatable = false)
  private Long clienteId;

  @Column(name = "cliente_tipo", nullable = false, updatable = false, length = 15)
  private String clienteTipo;

  @Column(name = "cliente_nombre", nullable = false, updatable = false, length = 150)
  private String clienteNombre;

  @Column(name = "cliente_documento", updatable = false, length = 40)
  private String clienteDocumento;

  @Column(name = "cliente_telefono", updatable = false, length = 20)
  private String clienteTelefono;

  @Column(name = "cliente_direccion", updatable = false, length = 200)
  private String clienteDireccion;

  @Column(name = "cliente_ciudad", updatable = false, length = 80)
  private String clienteCiudad;

  @Column(name = "direccion", nullable = false, length = 200)
  private String direccion;

  @Column(name = "descripcion", nullable = false, length = 2000)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "moneda", nullable = false, updatable = false, length = 3)
  private Moneda moneda;

  @Column(name = "trm", updatable = false, precision = 19, scale = 6)
  private BigDecimal trm;

  @Column(name = "fecha_trm", updatable = false)
  private LocalDate fechaTrm;

  @Column(name = "tasa_ves", updatable = false, precision = 19, scale = 6)
  private BigDecimal tasaVes;

  @Column(name = "fecha_tasa_ves", updatable = false)
  private LocalDate fechaTasaVes;

  @Column(name = "material", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal material;

  @Column(name = "mano_obra", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal manoDeObra;

  @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal subtotal;

  @Enumerated(EnumType.STRING)
  @Column(name = "descuento_tipo", nullable = false, updatable = false, length = 10)
  private TipoDescuento descuentoTipo;

  @Column(name = "descuento_valor", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal descuentoValor;

  @Column(name = "descuento", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal descuento;

  @Column(name = "total", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal total;

  @Column(name = "costo", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal costo;

  @Column(name = "utilidad", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal utilidad;

  @Column(name = "porcentaje_utilidad", updatable = false, precision = 9, scale = 2)
  private BigDecimal porcentajeUtilidad;

  @Column(name = "total_usd", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal totalUsd;

  @Column(name = "costo_usd", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal costoUsd;

  @Column(name = "utilidad_usd", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal utilidadUsd;

  @Column(name = "garantia_mano_obra_meses", nullable = false, updatable = false)
  private int garantiaManoObraMeses;

  @Column(name = "vence_mano_obra", nullable = false, updatable = false)
  private LocalDate venceManoObra;

  @Column(name = "vence_equipos", nullable = false, updatable = false)
  private LocalDate venceEquipos;

  @Column(name = "condiciones_garantia", nullable = false, length = 2000)
  private String condicionesGarantia;

  @Column(name = "observaciones", length = 500)
  private String observaciones;

  @Column(name = "monedas_comprobante", length = 20)
  private String monedasComprobante;

  @Column(name = "cotizacion_id", updatable = false)
  private Long cotizacionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private EstadoInstalacion estado;

  @Column(name = "motivo_anulacion", length = 300)
  private String motivoAnulacion;

  @Column(name = "anulada_por")
  private Long anuladaPor;

  @Column(name = "anulada_en")
  private Instant anuladaEn;

  @ElementCollection
  @CollectionTable(name = "tecnico_instalacion", joinColumns = @JoinColumn(name = "instalacion_id"))
  @Column(name = "usuario_id")
  private Set<Long> tecnicos = new TreeSet<>();

  @OneToMany(mappedBy = "instalacion", cascade = CascadeType.PERSIST)
  @OrderBy("id")
  private List<LineaInstalacion> lineas = new ArrayList<>();

  protected Instalacion() {}

  /** Tasas guardadas con la instalación (RN-04). */
  public record TasasInstalacion(
      BigDecimal trm, LocalDate fechaTrm, BigDecimal tasaVes, LocalDate fechaTasaVes) {}

  /** Garantías: meses de mano de obra elegidos (1 a 3) y meses de equipos de Configuración. */
  public record Garantias(int manoObraMeses, int equiposMeses, String condiciones) {}

  /** Datos que se pueden corregir después de guardada (RF-122, P-44). */
  public record Descripcion(
      String direccion,
      String descripcion,
      Collection<Long> tecnicos,
      String condicionesGarantia,
      String observaciones,
      Set<Moneda> monedasComprobante) {}

  public static Instalacion registrar(
      long numero,
      LocalDate fecha,
      CopiaCliente cliente,
      Descripcion datos,
      Moneda moneda,
      TasasInstalacion tasas,
      Descuento descuento,
      ResumenDocumento resumen,
      Garantias garantias,
      List<LineaInstalacion> lineas) {
    ReglasInstalacion.validarContenido(lineas.size(), resumen.manoDeObra());
    ReglasInstalacion.validarMesesGarantia(garantias.manoObraMeses());
    Instalacion instalacion = new Instalacion();
    instalacion.numero = numero;
    instalacion.fecha = fecha;
    instalacion.clienteId = cliente.id();
    instalacion.clienteTipo = cliente.tipo();
    instalacion.clienteNombre = cliente.nombre();
    instalacion.clienteDocumento = cliente.documento();
    instalacion.clienteTelefono = cliente.telefono();
    instalacion.clienteDireccion = cliente.direccion();
    instalacion.clienteCiudad = cliente.ciudad();
    instalacion.moneda = moneda;
    instalacion.trm = tasas.trm();
    instalacion.fechaTrm = tasas.fechaTrm();
    instalacion.tasaVes = tasas.tasaVes();
    instalacion.fechaTasaVes = tasas.fechaTasaVes();
    instalacion.descuentoTipo = descuento.tipo();
    instalacion.descuentoValor = descuento.valor();
    instalacion.material = resumen.material();
    instalacion.manoDeObra = resumen.manoDeObra();
    instalacion.subtotal = resumen.subtotal();
    instalacion.descuento = resumen.descuento();
    instalacion.total = resumen.total();
    instalacion.costo = resumen.costo();
    instalacion.utilidad = resumen.utilidad();
    instalacion.porcentajeUtilidad = resumen.porcentajeUtilidad();
    instalacion.totalUsd = resumen.totalUsd();
    instalacion.costoUsd = resumen.costoUsd();
    instalacion.utilidadUsd = resumen.utilidadUsd();
    instalacion.garantiaManoObraMeses = garantias.manoObraMeses();
    instalacion.venceManoObra = Garantia.vencimiento(fecha, garantias.manoObraMeses());
    instalacion.venceEquipos = Garantia.vencimiento(fecha, garantias.equiposMeses());
    instalacion.estado = EstadoInstalacion.ACTIVA;
    instalacion.cambiarDescripcion(
        datos.direccion() == null ? cliente.direccion() : datos.direccion(),
        datos,
        garantias.condiciones());
    for (LineaInstalacion linea : lineas) {
      linea.asignarInstalacion(instalacion);
      instalacion.lineas.add(linea);
    }
    return instalacion;
  }

  /**
   * Corrige los datos descriptivos (RF-122, P-44): dirección, descripción, técnicos, condiciones de
   * garantía, observaciones y monedas adicionales del comprobante.
   */
  public void cambiarDescripcion(Descripcion datos) {
    Descripcion completa =
        new Descripcion(
            datos.direccion() == null ? direccion : datos.direccion(),
            datos.descripcion() == null ? descripcion : datos.descripcion(),
            datos.tecnicos(),
            datos.condicionesGarantia(),
            datos.observaciones(),
            datos.monedasComprobante());
    cambiarDescripcion(completa.direccion(), completa, condicionesGarantia);
  }

  /**
   * @param condicionesPorDefecto las de Configuración al registrar, o las actuales al editar
   */
  private void cambiarDescripcion(
      String direccionNueva, Descripcion datos, String condicionesPorDefecto) {
    ReglasInstalacion.validarTecnicos(datos.tecnicos());
    String limpia = Textos.limpiar(direccionNueva);
    if (limpia == null) {
      throw new InstalacionInvalidaException(
          "INSTALACION_SIN_DIRECCION", "Ingresa la dirección de la instalación.");
    }
    this.direccion = limpia;
    this.descripcion = Textos.limpiar(datos.descripcion());
    if (this.descripcion == null) {
      throw new InstalacionInvalidaException(
          "INSTALACION_SIN_DESCRIPCION", "Describe el trabajo realizado.");
    }
    this.condicionesGarantia =
        Textos.limpiar(datos.condicionesGarantia()) != null
            ? Textos.limpiar(datos.condicionesGarantia())
            : Textos.limpiar(condicionesPorDefecto);
    this.observaciones = Textos.limpiar(datos.observaciones());
    this.tecnicos.clear();
    this.tecnicos.addAll(datos.tecnicos());
    EnumSet<Moneda> adicionales = EnumSet.noneOf(Moneda.class);
    if (datos.monedasComprobante() != null) {
      adicionales.addAll(datos.monedasComprobante());
    }
    adicionales.remove(moneda);
    this.monedasComprobante =
        adicionales.isEmpty()
            ? null
            : adicionales.stream().map(Moneda::name).collect(Collectors.joining(","));
  }

  /** Marca la instalación como anulada, con motivo, usuario y hora (RF-73). */
  public void anular(String motivo, Long usuarioId, Instant ahora) {
    if (estaAnulada()) {
      throw new InstalacionYaAnuladaException();
    }
    this.estado = EstadoInstalacion.ANULADA;
    this.motivoAnulacion = Textos.limpiar(motivo);
    this.anuladaPor = usuarioId;
    this.anuladaEn = ahora;
  }

  public boolean estaAnulada() {
    return estado == EstadoInstalacion.ANULADA;
  }

  public String consecutivo() {
    return TipoDocumento.INSTALACION.consecutivo(numero);
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

  public TasasInstalacion getTasas() {
    return new TasasInstalacion(trm, fechaTrm, tasaVes, fechaTasaVes);
  }

  public ResumenDocumento getResumen() {
    return new ResumenDocumento(
        material,
        manoDeObra,
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

  public Set<Moneda> getMonedasComprobante() {
    EnumSet<Moneda> monedas = EnumSet.noneOf(Moneda.class);
    if (monedasComprobante != null) {
      Arrays.stream(monedasComprobante.split(",")).map(Moneda::valueOf).forEach(monedas::add);
    }
    return monedas;
  }

  public Long getId() {
    return id;
  }

  public Long getNumero() {
    return numero;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public Long getClienteId() {
    return clienteId;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public Moneda getMoneda() {
    return moneda;
  }

  public TipoDescuento getDescuentoTipo() {
    return descuentoTipo;
  }

  public BigDecimal getDescuentoValor() {
    return descuentoValor;
  }

  public int getGarantiaManoObraMeses() {
    return garantiaManoObraMeses;
  }

  public LocalDate getVenceManoObra() {
    return venceManoObra;
  }

  public LocalDate getVenceEquipos() {
    return venceEquipos;
  }

  public String getCondicionesGarantia() {
    return condicionesGarantia;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public Long getCotizacionId() {
    return cotizacionId;
  }

  public EstadoInstalacion getEstado() {
    return estado;
  }

  public String getMotivoAnulacion() {
    return motivoAnulacion;
  }

  public Long getAnuladaPor() {
    return anuladaPor;
  }

  public Instant getAnuladaEn() {
    return anuladaEn;
  }

  public Set<Long> getTecnicos() {
    return Set.copyOf(tecnicos);
  }

  public List<LineaInstalacion> getLineas() {
    return lineas;
  }
}
