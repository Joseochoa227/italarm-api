package co.italarm.api.terceros.infraestructura;

import co.italarm.api.terceros.dominio.Cliente;
import co.italarm.api.terceros.dominio.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepositorio
    extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

  /** Si otro cliente (distinto de {@code id}, que puede ser null) ya tiene ese documento. */
  @Query(
      "select count(c) > 0 from Cliente c where c.tipoDocumento = :tipo"
          + " and c.numeroDocumento = :numero and (:id is null or c.id <> :id)")
  boolean existeDocumento(
      @Param("tipo") TipoDocumento tipo, @Param("numero") String numero, @Param("id") Long id);
}
