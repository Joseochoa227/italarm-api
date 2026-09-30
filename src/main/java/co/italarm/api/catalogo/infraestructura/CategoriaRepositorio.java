package co.italarm.api.catalogo.infraestructura;

import co.italarm.api.catalogo.dominio.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

  List<Categoria> findAllByOrderByNombreAsc();

  boolean existsByNombreIgnoreCase(String nombre);

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
