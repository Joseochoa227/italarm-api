package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Tasa de cambio de un par en un día (sección 3.5). Hay una sola por par y fecha. Los documentos
 * copian el valor al guardarse, así que corregirla no cambia lo ya registrado (RN-04).
 */
@Entity
@Table(name = "tasa_cambio")
public class TasaCambio extends EntidadMaestra {

  static final String MOTIVO_REEMPLAZO_OFICIAL = "Reemplazada por la TRM oficial";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "par", nullable = false, length = 7, updatable = false)
  private ParMoneda par;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "valor", nullable = false, precision = 19, scale = 6)
  private BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(name = "fuente", nullable = false, length = 20)
  private FuenteTasa fuente;

  @Column(name = "registrada_por")
  private Long registradaPor;

  @Column(name = "registrada_en", nullable = false)
  private Instant registradaEn;

  protected TasaCambio() {}

  public static TasaCambio registrarManual(
      ParMoneda par, LocalDate fecha, BigDecimal valor, Long usuarioId, Instant ahora) {
    TasaCambio tasa = new TasaCambio();
    tasa.par = par;
    tasa.fecha = fecha;
    tasa.valor = valor;
    tasa.fuente = FuenteTasa.MANUAL;
    tasa.registradaPor = usuarioId;
    tasa.registradaEn = ahora;
    return tasa;
  }

  /** TRM publicada por la Superintendencia Financiera (RF-28). */
  public static TasaCambio registrarOficial(LocalDate fecha, BigDecimal valor, Instant ahora) {
    TasaCambio tasa = new TasaCambio();
    tasa.par = ParMoneda.USD_COP;
    tasa.fecha = fecha;
    tasa.valor = valor;
    tasa.fuente = FuenteTasa.SUPERFINANCIERA;
    tasa.registradaEn = ahora;
    return tasa;
  }

  /** Corrige el valor dejando registro del anterior, el nuevo y el usuario (RF-36). */
  public CorreccionTasa corregir(
      BigDecimal nuevoValor, Long usuarioId, String motivo, Instant ahora) {
    CorreccionTasa correccion =
        CorreccionTasa.manual(id, valor, nuevoValor, motivo, usuarioId, ahora);
    this.valor = nuevoValor;
    return correccion;
  }

  /**
   * La TRM oficial reemplaza a la registrada a mano el mismo día (P-13). Queda como corrección
   * automática.
   */
  public CorreccionTasa reemplazarPorOficial(BigDecimal valorOficial, Instant ahora) {
    CorreccionTasa correccion =
        CorreccionTasa.automatica(id, valor, valorOficial, MOTIVO_REEMPLAZO_OFICIAL, ahora);
    this.valor = valorOficial;
    this.fuente = FuenteTasa.SUPERFINANCIERA;
    this.registradaPor = null;
    this.registradaEn = ahora;
    return correccion;
  }

  public Long getId() {
    return id;
  }

  public ParMoneda getPar() {
    return par;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public FuenteTasa getFuente() {
    return fuente;
  }

  public Long getRegistradaPor() {
    return registradaPor;
  }

  public Instant getRegistradaEn() {
    return registradaEn;
  }
}
