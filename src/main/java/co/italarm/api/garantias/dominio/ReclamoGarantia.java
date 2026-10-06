package co.italarm.api.garantias.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Reclamo de garantía sobre una instalación o un serial (RF-125, P-45): nota de seguimiento con la
 * fecha, el problema y la solución. No se borra; la solución se puede escribir después.
 */
@Entity
@Table(name = "reclamo_garantia")
public class ReclamoGarantia extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "instalacion_id", updatable = false)
  private Long instalacionId;

  @Column(name = "serial_id", updatable = false)
  private Long serialId;

  @Column(name = "cliente_id", nullable = false, updatable = false)
  private Long clienteId;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "problema", nullable = false, updatable = false, length = 2000)
  private String problema;

  @Column(name = "solucion", length = 2000)
  private String solucion;

  @Column(name = "en_garantia", nullable = false, updatable = false)
  private boolean enGarantia;

  protected ReclamoGarantia() {}

  /**
   * @param vencimiento fin de la garantía reclamada: si la fecha del reclamo es posterior, queda
   *     "fuera de garantía" (P-45)
   */
  public static ReclamoGarantia registrar(
      Long instalacionId,
      Long serialId,
      Long clienteId,
      LocalDate fecha,
      LocalDate hoy,
      LocalDate vencimiento,
      String problema,
      String solucion) {
    if ((instalacionId == null) == (serialId == null)) {
      throw new ReclamoInvalidoException("El reclamo es sobre una instalación o sobre un serial.");
    }
    if (fecha.isAfter(hoy)) {
      throw new ReclamoInvalidoException("La fecha del reclamo no puede ser posterior a hoy.");
    }
    String texto = Textos.limpiar(problema);
    if (texto == null) {
      throw new ReclamoInvalidoException("Describe el problema.");
    }
    ReclamoGarantia reclamo = new ReclamoGarantia();
    reclamo.instalacionId = instalacionId;
    reclamo.serialId = serialId;
    reclamo.clienteId = clienteId;
    reclamo.fecha = fecha;
    reclamo.problema = texto;
    reclamo.solucion = Textos.limpiar(solucion);
    reclamo.enGarantia = !fecha.isAfter(vencimiento);
    return reclamo;
  }

  /** Escribe o corrige la solución (P-45). */
  public void cambiarSolucion(String nueva) {
    this.solucion = Textos.limpiar(nueva);
  }

  public Long getId() {
    return id;
  }

  public Long getInstalacionId() {
    return instalacionId;
  }

  public Long getSerialId() {
    return serialId;
  }

  public Long getClienteId() {
    return clienteId;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public String getProblema() {
    return problema;
  }

  public String getSolucion() {
    return solucion;
  }

  public boolean isEnGarantia() {
    return enGarantia;
  }
}
