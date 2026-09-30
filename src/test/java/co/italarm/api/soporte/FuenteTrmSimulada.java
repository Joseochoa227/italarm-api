package co.italarm.api.soporte;

import co.italarm.api.tasas.aplicacion.FuenteTrm;
import co.italarm.api.tasas.aplicacion.FuenteTrmNoDisponibleException;
import co.italarm.api.tasas.aplicacion.TrmPublicada;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Fuente de la TRM controlable desde las pruebas: responde un valor o falla. */
public class FuenteTrmSimulada implements FuenteTrm {

  private volatile BigDecimal valor;
  private final AtomicInteger consultas = new AtomicInteger();

  public void responder(String trm) {
    this.valor = new BigDecimal(trm);
  }

  public void fallar() {
    this.valor = null;
  }

  public int consultas() {
    return consultas.get();
  }

  public void restablecer() {
    this.valor = null;
    consultas.set(0);
  }

  @Override
  public TrmPublicada consultar(LocalDate fecha) {
    consultas.incrementAndGet();
    if (valor == null) {
      throw new FuenteTrmNoDisponibleException("Servicio simulado sin respuesta");
    }
    return new TrmPublicada(valor, fecha, fecha);
  }
}
