package co.italarm.api.ventas.infraestructura;

import co.italarm.api.ventas.dominio.LineaVenta;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LineaVentaRepositorio extends JpaRepository<LineaVenta, Long> {

  /** Líneas de varias ventas de una vez (para los listados). */
  @Query("select l from LineaVenta l where l.venta.id in :ids order by l.id")
  List<LineaVenta> deVentas(@Param("ids") Collection<Long> ventaIds);
}
