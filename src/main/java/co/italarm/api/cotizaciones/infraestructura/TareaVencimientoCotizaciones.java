package co.italarm.api.cotizaciones.infraestructura;

import co.italarm.api.cotizaciones.aplicacion.ServicioCotizaciones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vencimiento automático de las cotizaciones (RN-13, P-47): todos los días a las 00:05 (hora de
 * Colombia) y al arrancar la API, por si el hosting la apagó a esa hora.
 */
@Component
public class TareaVencimientoCotizaciones {

  private static final Logger LOG = LoggerFactory.getLogger(TareaVencimientoCotizaciones.class);

  private final ServicioCotizaciones servicio;

  public TareaVencimientoCotizaciones(ServicioCotizaciones servicio) {
    this.servicio = servicio;
  }

  @Scheduled(cron = "0 5 0 * * *", zone = "America/Bogota")
  public void diaria() {
    ejecutar();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void alArrancar() {
    ejecutar();
  }

  private void ejecutar() {
    try {
      int vencidas = servicio.vencer();
      if (vencidas > 0) {
        LOG.info("{} cotizaciones pasaron a Vencida", vencidas);
      }
    } catch (RuntimeException e) {
      LOG.error("Error inesperado en el vencimiento de cotizaciones", e);
    }
  }
}
