package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.instalaciones.infraestructura.InstalacionRepositorio;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.terceros.aplicacion.MovimientosCliente;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Las instalaciones de cada cliente para su listado e historial (RF-76, RF-77, P-36). */
@Service
public class MovimientosClienteInstalaciones implements MovimientosCliente {

  private final InstalacionRepositorio instalaciones;

  public MovimientosClienteInstalaciones(InstalacionRepositorio instalaciones) {
    this.instalaciones = instalaciones;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<Long, Resumen> resumen(Collection<Long> clienteIds) {
    Map<Long, Resumen> resultado = new HashMap<>();
    for (Object[] fila : instalaciones.resumenPorCliente(clienteIds)) {
      resultado.put((Long) fila[0], new Resumen((Long) fila[1], (LocalDate) fila[2]));
    }
    return resultado;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Movimiento> historial(Long clienteId) {
    return instalaciones.findByClienteIdOrderByFechaDescIdDesc(clienteId).stream()
        .map(
            i ->
                new Movimiento(
                    "INSTALACION",
                    i.getId(),
                    i.consecutivo(),
                    i.getFecha(),
                    i.getDescripcion(),
                    new Dinero(i.getResumen().total(), i.getMoneda()),
                    i.getEstado().name()))
        .toList();
  }
}
