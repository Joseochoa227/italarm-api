package co.italarm.api.compras.infraestructura;

import co.italarm.api.compras.dominio.Compra;
import co.italarm.api.compras.dominio.EstadoCompra;
import co.italarm.api.compras.dominio.LineaCompra;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de compras (RF-46, RF-47, RF-38). */
public final class EspecificacionesCompras {

  private EspecificacionesCompras() {}

  public static Specification<Compra> filtrar(
      Long proveedorId,
      Long productoId,
      LocalDate desde,
      LocalDate hasta,
      boolean incluirAnuladas) {
    Specification<Compra> especificacion =
        (raiz, consulta, cb) -> cb.between(raiz.get("fecha"), desde, hasta);
    if (proveedorId != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> cb.equal(raiz.get("proveedorId"), proveedorId));
    }
    if (productoId != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> {
                Subquery<Long> lineas = consulta.subquery(Long.class);
                Root<LineaCompra> linea = lineas.from(LineaCompra.class);
                lineas
                    .select(linea.get("id"))
                    .where(
                        cb.equal(linea.get("compra"), raiz),
                        cb.equal(linea.get("productoId"), productoId));
                return cb.exists(lineas);
              });
    }
    if (!incluirAnuladas) {
      especificacion = especificacion.and(activas());
    }
    return especificacion;
  }

  public static Specification<Compra> activas() {
    return (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), EstadoCompra.ACTIVA);
  }
}
