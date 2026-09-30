package co.italarm.api.tasas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Registro de una ejecución de la tarea de la TRM (BP-15). */
@Entity
@Table(name = "ejecucion_tarea_trm")
public class EjecucionTareaTrm {

  private static final int LONGITUD_DETALLE = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fecha_objetivo", nullable = false)
  private LocalDate fechaObjetivo;

  @Column(name = "inicio", nullable = false)
  private Instant inicio;

  @Column(name = "fin", nullable = false)
  private Instant fin;

  @Enumerated(EnumType.STRING)
  @Column(name = "resultado", nullable = false, length = 10)
  private ResultadoEjecucion resultado;

  @Column(name = "intento", nullable = false)
  private int intento;

  @Column(name = "detalle", length = LONGITUD_DETALLE)
  private String detalle;

  protected EjecucionTareaTrm() {}

  public static EjecucionTareaTrm de(
      LocalDate fecha,
      Instant inicio,
      Instant fin,
      ResultadoEjecucion resultado,
      int intento,
      String detalle) {
    EjecucionTareaTrm ejecucion = new EjecucionTareaTrm();
    ejecucion.fechaObjetivo = fecha;
    ejecucion.inicio = inicio;
    ejecucion.fin = fin;
    ejecucion.resultado = resultado;
    ejecucion.intento = intento;
    ejecucion.detalle =
        detalle == null || detalle.length() <= LONGITUD_DETALLE
            ? detalle
            : detalle.substring(0, LONGITUD_DETALLE);
    return ejecucion;
  }

  public Long getId() {
    return id;
  }

  public LocalDate getFechaObjetivo() {
    return fechaObjetivo;
  }

  public ResultadoEjecucion getResultado() {
    return resultado;
  }

  public int getIntento() {
    return intento;
  }

  public String getDetalle() {
    return detalle;
  }

  public Instant getInicio() {
    return inicio;
  }

  public Instant getFin() {
    return fin;
  }
}
