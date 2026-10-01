package co.italarm.api.shared.infraestructura;

import co.italarm.api.shared.dominio.RegistroIdempotencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotenciaRepositorio extends JpaRepository<RegistroIdempotencia, Long> {

  Optional<RegistroIdempotencia> findByUsuarioIdAndOperacionAndClave(
      Long usuarioId, String operacion, String clave);
}
