package co.italarm.api.tasas.infraestructura;

import co.italarm.api.tasas.dominio.CorreccionTasa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorreccionTasaRepositorio extends JpaRepository<CorreccionTasa, Long> {

  List<CorreccionTasa> findByTasaIdOrderByCorregidaEnAsc(Long tasaId);
}
