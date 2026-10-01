package co.italarm.api.compras.infraestructura;

import co.italarm.api.compras.dominio.LineaCompra;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LineaCompraRepositorio extends JpaRepository<LineaCompra, Long> {

  /** Líneas de varias compras de una vez (para el listado). */
  @Query("select l from LineaCompra l where l.compra.id in :ids order by l.id")
  List<LineaCompra> deCompras(@Param("ids") Collection<Long> compraIds);
}
