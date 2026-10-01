package co.italarm.api.inventario.infraestructura;

import co.italarm.api.inventario.dominio.HistorialCosto;
import co.italarm.api.shared.dominio.TipoDocumento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialCostoRepositorio extends JpaRepository<HistorialCosto, Long> {

  List<HistorialCosto> findByProductoIdOrderByIdDesc(Long productoId);

  Optional<HistorialCosto> findFirstByProductoIdAndDocumentoTipoAndDocumentoIdOrderByIdAsc(
      Long productoId, TipoDocumento tipo, Long documentoId);

  /** El último cambio de costo por compra: de ahí sale la tasa de la última compra (RF-69). */
  Optional<HistorialCosto> findFirstByProductoIdAndDocumentoTipoOrderByIdDesc(
      Long productoId, TipoDocumento tipo);
}
