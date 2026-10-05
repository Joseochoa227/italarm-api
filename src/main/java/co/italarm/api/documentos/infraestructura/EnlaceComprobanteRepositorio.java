package co.italarm.api.documentos.infraestructura;

import co.italarm.api.documentos.dominio.EnlaceComprobante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnlaceComprobanteRepositorio extends JpaRepository<EnlaceComprobante, Long> {

  Optional<EnlaceComprobante> findByTokenHash(String tokenHash);
}
