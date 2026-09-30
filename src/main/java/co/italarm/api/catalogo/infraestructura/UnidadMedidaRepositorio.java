package co.italarm.api.catalogo.infraestructura;

import co.italarm.api.catalogo.dominio.UnidadMedida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadMedidaRepositorio extends JpaRepository<UnidadMedida, Long> {

  List<UnidadMedida> findAllByOrderByNombreAsc();

  /**
   * Si otra unidad (distinta de {@code id}, que puede ser null) ya usa el nombre o la abreviatura.
   */
  @Query(
      "select count(u) > 0 from UnidadMedida u"
          + " where (lower(u.nombre) = lower(:nombre) or lower(u.abreviatura) = lower(:abreviatura))"
          + " and (:id is null or u.id <> :id)")
  boolean existeDuplicada(
      @Param("nombre") String nombre,
      @Param("abreviatura") String abreviatura,
      @Param("id") Long id);
}
