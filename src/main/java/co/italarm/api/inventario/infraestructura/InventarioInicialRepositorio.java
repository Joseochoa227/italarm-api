package co.italarm.api.inventario.infraestructura;

import co.italarm.api.inventario.dominio.InventarioInicial;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventarioInicialRepositorio extends JpaRepository<InventarioInicial, Long> {

  @EntityGraph(attributePaths = "lineas")
  List<InventarioInicial> findAllByOrderByIdDesc();
}
