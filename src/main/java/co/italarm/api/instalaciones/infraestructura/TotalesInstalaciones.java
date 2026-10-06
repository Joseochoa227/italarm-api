package co.italarm.api.instalaciones.infraestructura;

import co.italarm.api.instalaciones.dominio.Instalacion;
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

/** Totales de las instalaciones de un filtro por moneda (RF-121, RF-73). */
@Repository
public class TotalesInstalaciones {

  private final EntityManager entityManager;

  public TotalesInstalaciones(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public List<TotalPorMoneda> porMoneda(Specification<Instalacion> filtro) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> consulta = cb.createTupleQuery();
    Root<Instalacion> raiz = consulta.from(Instalacion.class);
    consulta
        .multiselect(
            raiz.get("moneda"),
            cb.sum(raiz.<BigDecimal>get("material")),
            cb.sum(raiz.<BigDecimal>get("manoDeObra")),
            cb.sum(raiz.<BigDecimal>get("total")),
            cb.sum(raiz.<BigDecimal>get("costo")),
            cb.sum(raiz.<BigDecimal>get("utilidad")),
            cb.sum(raiz.<BigDecimal>get("totalUsd")),
            cb.sum(raiz.<BigDecimal>get("utilidadUsd")),
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
                    fila.get(3, BigDecimal.class),
                    fila.get(4, BigDecimal.class),
                    fila.get(5, BigDecimal.class),
                    fila.get(6, BigDecimal.class),
                    fila.get(7, BigDecimal.class),
                    fila.get(8, Long.class)))
        .toList();
  }

  public record TotalPorMoneda(
      Moneda moneda,
      BigDecimal material,
      BigDecimal manoDeObra,
      BigDecimal total,
      BigDecimal costo,
      BigDecimal utilidad,
      BigDecimal totalUsd,
      BigDecimal utilidadUsd,
      long instalaciones) {}
}
