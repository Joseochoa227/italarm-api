package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.EntidadMaestra;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Unidad física de un equipo, identificada por su número de serie (sección 3.4). */
@Entity
@Table(name = "serial")
public class Serial extends EntidadMaestra {

  private static final int LONGITUD_MAXIMA = 80;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Column(name = "numero", nullable = false, updatable = false, length = LONGITUD_MAXIMA)
  private String numero;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 15)
  private EstadoSerial estado;

  @Column(name = "fecha_entrada", nullable = false, updatable = false)
  private LocalDate fechaEntrada;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "tipo", column = @Column(name = "entrada_tipo", updatable = false)),
    @AttributeOverride(name = "id", column = @Column(name = "entrada_id", updatable = false)),
    @AttributeOverride(
        name = "consecutivo",
        column = @Column(name = "entrada_consecutivo", updatable = false))
  })
  private DocumentoRef documentoEntrada;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "tipo", column = @Column(name = "salida_tipo")),
    @AttributeOverride(name = "id", column = @Column(name = "salida_id")),
    @AttributeOverride(name = "consecutivo", column = @Column(name = "salida_consecutivo"))
  })
  private DocumentoRef documentoSalida;

  /**
   * Vencimiento de la garantía del equipo (RF-23); se llena al salir en una venta o instalación.
   */
  @Column(name = "vencimiento_garantia")
  private LocalDate vencimientoGarantia;

  protected Serial() {}

  public static Serial entrar(
      Long productoId, String numero, DocumentoRef entrada, LocalDate fecha) {
    Serial serial = new Serial();
    serial.productoId = productoId;
    serial.numero = normalizar(numero);
    serial.estado = EstadoSerial.EN_BODEGA;
    serial.documentoEntrada = entrada;
    serial.fechaEntrada = fecha;
    return serial;
  }

  /** P-22: sin espacios a los lados y en mayúsculas. */
  public static String normalizar(String numero) {
    String limpio = numero == null ? "" : numero.trim().toUpperCase(Locale.ROOT);
    if (limpio.isEmpty()) {
      throw new SerialInvalidoException("Hay un número de serie vacío.");
    }
    if (limpio.length() > LONGITUD_MAXIMA) {
      throw new SerialInvalidoException(
          "El serial " + limpio.substring(0, 20) + "… supera los 80 caracteres.");
    }
    return limpio;
  }

  /**
   * RF-20: la cantidad de seriales coincide con la cantidad y ninguno se repite. Devuelve la lista
   * normalizada.
   */
  public static List<String> validarLista(
      List<String> seriales, BigDecimal cantidad, String producto) {
    int recibidos = seriales == null ? 0 : seriales.size();
    if (cantidad.stripTrailingZeros().scale() > 0
        || cantidad.compareTo(BigDecimal.valueOf(recibidos)) != 0) {
      throw new SerialesNoCoincidenException(
          producto
              + ": se compraron "
              + cantidad.stripTrailingZeros().toPlainString()
              + " unidades y se ingresaron "
              + recibidos
              + " seriales.");
    }
    List<String> normalizados = new ArrayList<>();
    Set<String> vistos = new HashSet<>();
    for (String serial : seriales) {
      String limpio = normalizar(serial);
      if (!vistos.add(limpio)) {
        throw new SerialDuplicadoException(producto + ": el serial " + limpio + " está repetido.");
      }
      normalizados.add(limpio);
    }
    return normalizados;
  }

  public boolean estaDisponible() {
    return estado == EstadoSerial.EN_BODEGA;
  }

  /** Sale de bodega por un ajuste de pérdida, daño o garantía (RF-59). */
  public void darDeBaja(DocumentoRef ajuste) {
    exigirDisponible();
    this.estado = EstadoSerial.DADO_DE_BAJA;
    this.documentoSalida = ajuste;
  }

  /** Sale vendido (RF-22), con la garantía del equipo hasta {@code vencimiento} (RF-23). */
  public void vender(DocumentoRef venta, LocalDate vencimiento) {
    exigirDisponible();
    this.estado = EstadoSerial.VENDIDO;
    this.documentoSalida = venta;
    this.vencimientoGarantia = vencimiento;
  }

  /** Vuelve a bodega porque se anuló la venta con que salió (RF-72); pierde la garantía. */
  public void devolver(DocumentoRef venta) {
    if (estado != EstadoSerial.VENDIDO || !venta.equals(documentoSalida)) {
      throw new SerialNoDisponibleException(
          "El serial " + numero + " no salió con " + venta.consecutivo() + ".");
    }
    this.estado = EstadoSerial.EN_BODEGA;
    this.documentoSalida = null;
    this.vencimientoGarantia = null;
  }

  /** Se anuló la compra con que entró (RF-71). */
  public void anular() {
    exigirDisponible();
    this.estado = EstadoSerial.ANULADO;
  }

  private void exigirDisponible() {
    if (!estaDisponible()) {
      throw new SerialNoDisponibleException("El serial " + numero + " no está en bodega.");
    }
  }

  public Long getId() {
    return id;
  }

  public Long getProductoId() {
    return productoId;
  }

  public String getNumero() {
    return numero;
  }

  public EstadoSerial getEstado() {
    return estado;
  }

  public LocalDate getFechaEntrada() {
    return fechaEntrada;
  }

  public DocumentoRef getDocumentoEntrada() {
    return documentoEntrada;
  }

  public DocumentoRef getDocumentoSalida() {
    return documentoSalida;
  }

  public LocalDate getVencimientoGarantia() {
    return vencimientoGarantia;
  }
}
