package co.italarm.api.ventas.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.terceros.aplicacion.MovimientosCliente;
import co.italarm.api.ventas.dominio.LineaVenta;
import co.italarm.api.ventas.dominio.Venta;
import co.italarm.api.ventas.infraestructura.LineaVentaRepositorio;
import co.italarm.api.ventas.infraestructura.VentaRepositorio;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Las ventas de cada cliente para su listado e historial (RF-76, RF-77). */
@Service
public class MovimientosClienteVentas implements MovimientosCliente {

  private final VentaRepositorio ventas;
  private final LineaVentaRepositorio lineas;

  public MovimientosClienteVentas(VentaRepositorio ventas, LineaVentaRepositorio lineas) {
    this.ventas = ventas;
    this.lineas = lineas;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<Long, Resumen> resumen(Collection<Long> clienteIds) {
    Map<Long, Resumen> resultado = new HashMap<>();
    for (Object[] fila : ventas.resumenPorCliente(clienteIds)) {
      resultado.put((Long) fila[0], new Resumen((Long) fila[1], (LocalDate) fila[2]));
    }
    return resultado;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Movimiento> historial(Long clienteId) {
    List<Venta> delCliente = ventas.findByClienteIdOrderByFechaDescIdDesc(clienteId);
    Map<Long, List<LineaVenta>> porVenta =
        delCliente.isEmpty()
            ? Map.of()
            : lineas.deVentas(delCliente.stream().map(Venta::getId).toList()).stream()
                .collect(Collectors.groupingBy(LineaVenta::getVentaId));
    return delCliente.stream()
        .map(
            v ->
                new Movimiento(
                    "VENTA",
                    v.getId(),
                    v.consecutivo(),
                    v.getFecha(),
                    ServicioVentas.resumenProductos(porVenta.getOrDefault(v.getId(), List.of())),
                    new Dinero(v.getTotal(), v.getMoneda()),
                    v.getEstado().name()))
        .toList();
  }
}
