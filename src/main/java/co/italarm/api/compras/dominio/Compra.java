package co.italarm.api.compras.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Textos;
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
import java.util.List;

/**
 * Compra a un proveedor (RF-39). Guarda las tasas de su fecha (RF-32, RN-04). No se edita en
 * valores: solo se anula (RF-48, RF-70); la factura adjunta sí se puede cambiar.
 */
@Entity
@Table(name = "compra")
public class Compra extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "numero", nullable = false, updatable = false)
  private Long numero;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "proveedor_id", nullable = false, updatable = false)
  private Long proveedorId;

  @Column(name = "numero_factura", nullable = false, updatable = false, length = 50)
  private String numeroFactura;

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

  @Column(name = "total", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal total;

  @Column(name = "total_usd", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal totalUsd;

  @Column(name = "factura_clave", length = 300)
  private String facturaClave;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private EstadoCompra estado;

  @Column(name = "motivo_anulacion", length = 300)
  private String motivoAnulacion;

  @Column(name = "anulada_por")
  private Long anuladaPor;

  @Column(name = "anulada_en")
  private Instant anuladaEn;

  @OneToMany(mappedBy = "compra", cascade = CascadeType.PERSIST)
  @OrderBy("id")
  private List<LineaCompra> lineas = new ArrayList<>();

  protected Compra() {}

  public static Compra registrar(
      long numero,
      LocalDate fecha,
      Long proveedorId,
      String numeroFactura,
      Moneda moneda,
      BigDecimal trm,
      LocalDate fechaTrm,
      BigDecimal tasaVes,
      LocalDate fechaTasaVes,
      List<LineaCompra> lineas) {
    Compra compra = new Compra();
    compra.numero = numero;
    compra.fecha = fecha;
    compra.proveedorId = proveedorId;
    compra.numeroFactura = Textos.limpiar(numeroFactura);
    compra.moneda = moneda;
    compra.trm = trm;
    compra.fechaTrm = fechaTrm;
    compra.tasaVes = tasaVes;
    compra.fechaTasaVes = fechaTasaVes;
    compra.estado = EstadoCompra.ACTIVA;
    BigDecimal suma = BigDecimal.ZERO;
    BigDecimal sumaUsd = BigDecimal.ZERO;
    for (LineaCompra linea : lineas) {
      linea.asignarCompra(compra);
      compra.lineas.add(linea);
      suma = suma.add(linea.getSubtotal());
      sumaUsd = sumaUsd.add(linea.getCantidad().multiply(linea.getCostoUnitarioUsd()));
    }
    compra.total = Redondeo.paraAlmacenar(suma);
    compra.totalUsd = Redondeo.paraAlmacenar(sumaUsd);
    return compra;
  }

  /** Marca la compra como anulada, con motivo, usuario y hora (RF-73). */
  public void anular(String motivo, Long usuarioId, Instant ahora) {
    if (estado == EstadoCompra.ANULADA) {
      throw new CompraYaAnuladaException();
    }
    this.estado = EstadoCompra.ANULADA;
    this.motivoAnulacion = Textos.limpiar(motivo);
    this.anuladaPor = usuarioId;
    this.anuladaEn = ahora;
  }

  /** Asigna la factura adjunta y devuelve la clave de la anterior (para eliminarla), o null. */
  public String cambiarFactura(String clave) {
    String anterior = facturaClave;
    this.facturaClave = clave;
    return anterior;
  }

  public String consecutivo() {
    return TipoDocumento.COMPRA.consecutivo(numero);
  }

  public boolean estaAnulada() {
    return estado == EstadoCompra.ANULADA;
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

  public Long getProveedorId() {
    return proveedorId;
  }

  public String getNumeroFactura() {
    return numeroFactura;
  }

  public Moneda getMoneda() {
    return moneda;
  }

  public BigDecimal getTrm() {
    return trm;
  }

  public LocalDate getFechaTrm() {
    return fechaTrm;
  }

  public BigDecimal getTasaVes() {
    return tasaVes;
  }

  public LocalDate getFechaTasaVes() {
    return fechaTasaVes;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public BigDecimal getTotalUsd() {
    return totalUsd;
  }

  public String getFacturaClave() {
    return facturaClave;
  }

  public EstadoCompra getEstado() {
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

  public List<LineaCompra> getLineas() {
    return lineas;
  }
}
