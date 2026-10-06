package co.italarm.api.ventas.dominio;

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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Venta de material (sección 3.12). Guarda las tasas de su fecha (RN-04), el costo del material al
 * momento de la salida y la utilidad (RF-68). No se edita en valores: solo se anula (RF-106); las
 * observaciones y las monedas adicionales del comprobante sí se pueden cambiar (RF-70, P-34).
 */
@Entity
@Table(name = "venta")
public class Venta extends EntidadMaestra {

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

  @Column(name = "observaciones", length = 500)
  private String observaciones;

  @Column(name = "monedas_comprobante", length = 20)
  private String monedasComprobante;

  @Column(name = "cotizacion_id", updatable = false)
  private Long cotizacionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private EstadoVenta estado;

  @Column(name = "motivo_anulacion", length = 300)
  private String motivoAnulacion;

  @Column(name = "anulada_por")
  private Long anuladaPor;

  @Column(name = "anulada_en")
  private Instant anuladaEn;

  @OneToMany(mappedBy = "venta", cascade = CascadeType.PERSIST)
  @OrderBy("id")
  private List<LineaVenta> lineas = new ArrayList<>();

  protected Venta() {}

  /** Tasas guardadas con la venta (RN-04). */
  public record TasasVenta(
      BigDecimal trm, LocalDate fechaTrm, BigDecimal tasaVes, LocalDate fechaTasaVes) {}

  public static Venta registrar(
      long numero,
      LocalDate fecha,
      CopiaCliente cliente,
      Moneda moneda,
      TasasVenta tasas,
      Descuento descuento,
      ResumenDocumento resumen,
      String observaciones,
      Set<Moneda> monedasComprobante,
      List<LineaVenta> lineas) {
    if (lineas.isEmpty()) {
      throw new VentaInvalidaException(
          VentaInvalidaException.SIN_LINEAS, "La venta debe tener al menos un producto.");
    }
    Venta venta = new Venta();
    venta.numero = numero;
    venta.fecha = fecha;
    venta.clienteId = cliente.id();
    venta.clienteTipo = cliente.tipo();
    venta.clienteNombre = cliente.nombre();
    venta.clienteDocumento = cliente.documento();
    venta.clienteTelefono = cliente.telefono();
    venta.clienteDireccion = cliente.direccion();
    venta.clienteCiudad = cliente.ciudad();
    venta.moneda = moneda;
    venta.trm = tasas.trm();
    venta.fechaTrm = tasas.fechaTrm();
    venta.tasaVes = tasas.tasaVes();
    venta.fechaTasaVes = tasas.fechaTasaVes();
    venta.descuentoTipo = descuento.tipo();
    venta.descuentoValor = descuento.valor();
    venta.subtotal = resumen.subtotal();
    venta.descuento = resumen.descuento();
    venta.total = resumen.total();
    venta.costo = resumen.costo();
    venta.utilidad = resumen.utilidad();
    venta.porcentajeUtilidad = resumen.porcentajeUtilidad();
    venta.totalUsd = resumen.totalUsd();
    venta.costoUsd = resumen.costoUsd();
    venta.utilidadUsd = resumen.utilidadUsd();
    venta.cambiarDescripcion(observaciones, monedasComprobante);
    venta.estado = EstadoVenta.ACTIVA;
    for (LineaVenta linea : lineas) {
      linea.asignarVenta(venta);
      venta.lineas.add(linea);
    }
    return venta;
  }

  /**
   * Cambia los datos descriptivos (RF-70): observaciones y monedas adicionales del comprobante
   * (P-34). La moneda de la venta no se repite en las adicionales.
   */
  public void cambiarDescripcion(String observaciones, Set<Moneda> monedas) {
    this.observaciones = Textos.limpiar(observaciones);
    EnumSet<Moneda> adicionales = EnumSet.noneOf(Moneda.class);
    if (monedas != null) {
      adicionales.addAll(monedas);
    }
    adicionales.remove(moneda);
    this.monedasComprobante =
        adicionales.isEmpty()
            ? null
            : adicionales.stream().map(Moneda::name).collect(Collectors.joining(","));
  }

  /** Marca la venta como anulada, con motivo, usuario y hora (RF-73). */
  public void anular(String motivo, Long usuarioId, Instant ahora) {
    if (estaAnulada()) {
      throw new VentaYaAnuladaException();
    }
    this.estado = EstadoVenta.ANULADA;
    this.motivoAnulacion = Textos.limpiar(motivo);
    this.anuladaPor = usuarioId;
    this.anuladaEn = ahora;
  }

  public boolean estaAnulada() {
    return estado == EstadoVenta.ANULADA;
  }

  public String consecutivo() {
    return TipoDocumento.VENTA.consecutivo(numero);
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

  public Moneda getMoneda() {
    return moneda;
  }

  public TasasVenta getTasas() {
    return new TasasVenta(trm, fechaTrm, tasaVes, fechaTasaVes);
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public TipoDescuento getDescuentoTipo() {
    return descuentoTipo;
  }

  public BigDecimal getDescuentoValor() {
    return descuentoValor;
  }

  public BigDecimal getDescuento() {
    return descuento;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public BigDecimal getCosto() {
    return costo;
  }

  public BigDecimal getUtilidad() {
    return utilidad;
  }

  public BigDecimal getPorcentajeUtilidad() {
    return porcentajeUtilidad;
  }

  public BigDecimal getTotalUsd() {
    return totalUsd;
  }

  public BigDecimal getCostoUsd() {
    return costoUsd;
  }

  public BigDecimal getUtilidadUsd() {
    return utilidadUsd;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public Long getCotizacionId() {
    return cotizacionId;
  }

  public EstadoVenta getEstado() {
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

  public List<LineaVenta> getLineas() {
    return lineas;
  }
}
