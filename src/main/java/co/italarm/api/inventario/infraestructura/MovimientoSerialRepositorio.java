package co.italarm.api.inventario.infraestructura;

import co.italarm.api.inventario.dominio.MovimientoSerial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoSerialRepositorio extends JpaRepository<MovimientoSerial, Long> {

  List<MovimientoSerial> findBySerialIdOrderByIdAsc(Long serialId);
}
