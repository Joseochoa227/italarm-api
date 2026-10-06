package co.italarm.api.instalaciones.infraestructura;

import co.italarm.api.instalaciones.dominio.FotoInstalacion;
import co.italarm.api.instalaciones.dominio.GrupoFoto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FotoInstalacionRepositorio extends JpaRepository<FotoInstalacion, Long> {

  List<FotoInstalacion> findByInstalacionIdOrderById(Long instalacionId);

  long countByInstalacionIdAndGrupo(Long instalacionId, GrupoFoto grupo);
}
