package co.italarm.api.tasas.infraestructura;

import co.italarm.api.tasas.aplicacion.ServicioTrm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea diaria de la TRM (RF-28, P-18): a las 6:00 a. m. (hora de Colombia) y, si falla, cada 30
 * minutos hasta las 12:00 m. Como es idempotente, las ejecuciones después del éxito no hacen nada.
 * También consulta al arrancar la API, por si el hosting la apagó durante la madrugada.
 */
@Component
@ConditionalOnProperty(name = "italarm.trm.programada", havingValue = "true", matchIfMissing = true)
public class TareaTrm {

  private static final Logger LOG = LoggerFactory.getLogger(TareaTrm.class);
  private static final String ZONA = "America/Bogota";

  private final ServicioTrm servicio;
  private final boolean alArrancar;

  public TareaTrm(
      ServicioTrm servicio, co.italarm.api.shared.infraestructura.PropiedadesItalarm propiedades) {
    this.servicio = servicio;
    this.alArrancar = propiedades.trm().alArrancar();
  }

  @Scheduled(cron = "0 0/30 6-11 * * *", zone = ZONA)
  public void consultarEnLaManana() {
    ejecutar();
  }

  @Scheduled(cron = "0 0 12 * * *", zone = ZONA)
  public void ultimoIntento() {
    ejecutar();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void alArrancar() {
    if (alArrancar) {
      ejecutar();
    }
  }

  private void ejecutar() {
    try {
      servicio.actualizarTrmDeHoy();
    } catch (RuntimeException e) {
      LOG.error("Error inesperado en la tarea de la TRM", e);
    }
  }
}
