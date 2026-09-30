package co.italarm.api.tasas.infraestructura;

import co.italarm.api.tasas.dominio.EjecucionTareaTrm;
import co.italarm.api.tasas.dominio.ResultadoEjecucion;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EjecucionTareaTrmRepositorio extends JpaRepository<EjecucionTareaTrm, Long> {

  boolean existsByFechaObjetivoAndResultado(LocalDate fecha, ResultadoEjecucion resultado);

  long countByFechaObjetivoAndResultadoNot(LocalDate fecha, ResultadoEjecucion resultado);

  List<EjecucionTareaTrm> findByFechaObjetivoOrderByIdAsc(LocalDate fecha);
}
