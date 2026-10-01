package co.italarm.api.catalogo.infraestructura;

import co.italarm.api.catalogo.dominio.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Cantidad de productos y valor total en bodega (stock × costo actual) de un filtro (RF-49). */
@Repository
public class ValorInventario {

  private final EntityManager entityManager;

  public ValorInventario(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public Total calcular(Specification<Producto> filtro) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> consulta = cb.createTupleQuery();
    Root<Producto> raiz = consulta.from(Producto.class);
    Expression<BigDecimal> costo =
        cb.coalesce(raiz.<BigDecimal>get("costoActualUsd"), BigDecimal.ZERO);
    consulta
        .multiselect(cb.count(raiz), cb.sum(cb.prod(raiz.<BigDecimal>get("stock"), costo)))
        .where(filtro.toPredicate(raiz, consulta, cb));
    Tuple fila = entityManager.createQuery(consulta).getSingleResult();
    BigDecimal valor = fila.get(1, BigDecimal.class);
    return new Total(fila.get(0, Long.class), valor == null ? BigDecimal.ZERO : valor);
  }

  public record Total(long productos, BigDecimal valorUsd) {}
}
