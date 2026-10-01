package co.italarm.api.compras.infraestructura;

import co.italarm.api.compras.dominio.Compra;
import co.italarm.api.shared.dominio.Moneda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Totales de las compras de un período por moneda (RF-47). */
@Repository
public class TotalesCompras {

  private final EntityManager entityManager;

  public TotalesCompras(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Suma de las compras que cumplen el filtro, por moneda de la factura. */
  public List<TotalPorMoneda> porMoneda(Specification<Compra> filtro) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> consulta = cb.createTupleQuery();
    Root<Compra> raiz = consulta.from(Compra.class);
    consulta
        .multiselect(
            raiz.get("moneda"),
            cb.sum(raiz.<BigDecimal>get("total")),
            cb.sum(raiz.<BigDecimal>get("totalUsd")),
            cb.count(raiz))
        .where(filtro.toPredicate(raiz, consulta, cb))
        .groupBy(raiz.get("moneda"))
        .orderBy(cb.asc(raiz.get("moneda")));
    return entityManager.createQuery(consulta).getResultList().stream()
        .map(
            fila ->
                new TotalPorMoneda(
                    fila.get(0, Moneda.class),
                    fila.get(1, BigDecimal.class),
                    fila.get(2, BigDecimal.class),
                    fila.get(3, Long.class)))
        .toList();
  }

  public record TotalPorMoneda(
      Moneda moneda, BigDecimal total, BigDecimal totalUsd, long compras) {}
}
