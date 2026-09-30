package co.italarm.api.tasas.aplicacion;

import co.italarm.api.tasas.dominio.EjecucionTareaTrm;
import co.italarm.api.tasas.dominio.FuenteTasa;
import co.italarm.api.tasas.dominio.ParMoneda;
import co.italarm.api.tasas.dominio.ResultadoEjecucion;
import co.italarm.api.tasas.dominio.TasaCambio;
import co.italarm.api.tasas.infraestructura.CorreccionTasaRepositorio;
import co.italarm.api.tasas.infraestructura.EjecucionTareaTrmRepositorio;
import co.italarm.api.tasas.infraestructura.TasaCambioRepositorio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones transaccionales de la tarea de la TRM. Están separadas de {@link ServicioTrm} para
 * que la consulta HTTP a la fuente no ocurra dentro de una transacción de base de datos.
 */
@Service
public class RegistroTrm {

  private static final Logger LOG = LoggerFactory.getLogger(RegistroTrm.class);

  private final TasaCambioRepositorio tasas;
  private final CorreccionTasaRepositorio correcciones;
  private final EjecucionTareaTrmRepositorio ejecuciones;

  public RegistroTrm(
      TasaCambioRepositorio tasas,
      CorreccionTasaRepositorio correcciones,
      EjecucionTareaTrmRepositorio ejecuciones) {
    this.tasas = tasas;
    this.correcciones = correcciones;
    this.ejecuciones = ejecuciones;
  }

  @Transactional(readOnly = true)
  public boolean tieneTrmOficial(LocalDate fecha) {
    return tasas
        .findByParAndFecha(ParMoneda.USD_COP, fecha)
        .filter(t -> t.getFuente() == FuenteTasa.SUPERFINANCIERA)
        .isPresent();
  }

  /** Número de este intento del día (las omitidas no cuentan). */
  @Transactional(readOnly = true)
  public int siguienteIntento(LocalDate fecha) {
    return (int) ejecuciones.countByFechaObjetivoAndResultadoNot(fecha, ResultadoEjecucion.OMITIDA)
        + 1;
  }

  /**
   * Guarda la TRM oficial del día. Si había una manual, la reemplaza y deja la corrección
   * automática (P-13). Devuelve false si ya había una oficial (otra ejecución se adelantó).
   */
  @Transactional
  public boolean guardarOficial(LocalDate fecha, BigDecimal valor, Instant ahora) {
    Optional<TasaCambio> existente = tasas.findByParAndFecha(ParMoneda.USD_COP, fecha);
    if (existente.isEmpty()) {
      tasas.save(TasaCambio.registrarOficial(fecha, valor, ahora));
      return true;
    }
    TasaCambio tasa = existente.get();
    if (tasa.getFuente() == FuenteTasa.SUPERFINANCIERA) {
      return false;
    }
    BigDecimal manual = tasa.getValor();
    correcciones.save(tasa.reemplazarPorOficial(valor, ahora));
    LOG.info("La TRM oficial del {} ({}) reemplazó a la manual ({})", fecha, valor, manual);
    return true;
  }

  @Transactional
  public void registrarEjecucion(EjecucionTareaTrm ejecucion) {
    ejecuciones.save(ejecucion);
  }
}
