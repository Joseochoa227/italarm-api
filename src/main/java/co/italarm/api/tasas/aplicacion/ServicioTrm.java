package co.italarm.api.tasas.aplicacion;

import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.tasas.dominio.EjecucionTareaTrm;
import co.italarm.api.tasas.dominio.ResultadoEjecucion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Consulta automática de la TRM (RF-28). Es idempotente: si la TRM oficial del día ya está
 * guardada, no hace nada (BP-15). Cada ejecución queda registrada y cada falla deja un ERROR en el
 * log.
 */
@Service
public class ServicioTrm {

  private static final Logger LOG = LoggerFactory.getLogger(ServicioTrm.class);

  private final FuenteTrm fuente;
  private final RegistroTrm registro;
  private final FechaNegocio fechas;

  public ServicioTrm(FuenteTrm fuente, RegistroTrm registro, FechaNegocio fechas) {
    this.fuente = fuente;
    this.registro = registro;
    this.fechas = fechas;
  }

  public ResultadoTrm actualizarTrmDeHoy() {
    return actualizar(fechas.hoy());
  }

  public ResultadoTrm actualizar(LocalDate fecha) {
    Instant inicio = fechas.ahora();
    if (registro.tieneTrmOficial(fecha)) {
      return terminar(fecha, inicio, ResultadoEjecucion.OMITIDA, null, 0, "Ya estaba guardada");
    }
    int intento = registro.siguienteIntento(fecha);
    BigDecimal valor;
    try {
      TrmPublicada publicada = fuente.consultar(fecha);
      valor = Redondeo.paraCalculo(publicada.valor());
    } catch (RuntimeException e) {
      LOG.error(
          "No se pudo obtener la TRM del {} (intento {}): {}", fecha, intento, e.getMessage());
      return terminar(fecha, inicio, ResultadoEjecucion.FALLO, null, intento, e.getMessage());
    }
    try {
      if (!registro.guardarOficial(fecha, valor, fechas.ahora())) {
        return terminar(fecha, inicio, ResultadoEjecucion.OMITIDA, valor, 0, "Ya estaba guardada");
      }
    } catch (DataIntegrityViolationException e) {
      // Otra ejecución la guardó al mismo tiempo.
      return terminar(fecha, inicio, ResultadoEjecucion.OMITIDA, valor, 0, "Ya estaba guardada");
    }
    LOG.info("TRM del {} guardada: {}", fecha, valor);
    return terminar(fecha, inicio, ResultadoEjecucion.EXITO, valor, intento, null);
  }

  private ResultadoTrm terminar(
      LocalDate fecha,
      Instant inicio,
      ResultadoEjecucion resultado,
      BigDecimal valor,
      int intento,
      String detalle) {
    registro.registrarEjecucion(
        EjecucionTareaTrm.de(fecha, inicio, fechas.ahora(), resultado, intento, detalle));
    return new ResultadoTrm(fecha, resultado, valor, detalle);
  }
}
