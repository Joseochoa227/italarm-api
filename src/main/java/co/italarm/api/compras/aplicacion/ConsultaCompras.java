package co.italarm.api.compras.aplicacion;

import co.italarm.api.compras.dominio.Compra;
import co.italarm.api.compras.infraestructura.CompraRepositorio;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos de compras para otros módulos (ventas, cotizaciones). */
@Service
public class ConsultaCompras {

  private final CompraRepositorio compras;

  public ConsultaCompras(CompraRepositorio compras) {
    this.compras = compras;
  }

  /**
   * Tasas de la última compra no anulada de cada producto (P-32). Un producto que nunca se compró
   * no aparece.
   */
  @Transactional(readOnly = true)
  public Map<Long, TasasUltimaCompra> tasasUltimaCompra(Collection<Long> productoIds) {
    if (productoIds.isEmpty()) {
      return Map.of();
    }
    List<Object[]> ultimas = compras.ultimaCompraPorProducto(productoIds);
    Map<Long, Compra> porId =
        compras
            .findAllById(ultimas.stream().map(fila -> (Long) fila[1]).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(Compra::getId, Function.identity()));
    Map<Long, TasasUltimaCompra> resultado = new HashMap<>();
    for (Object[] fila : ultimas) {
      Compra compra = porId.get((Long) fila[1]);
      resultado.put(
          (Long) fila[0],
          new TasasUltimaCompra(
              compra.consecutivo(), compra.getFecha(), compra.getTrm(), compra.getTasaVes()));
    }
    return resultado;
  }
}
