package co.italarm.api.usuarios.infraestructura;

import co.italarm.api.usuarios.dominio.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

  Optional<Usuario> findByCorreo(String correo);

  List<Usuario> findByContrasenaHashIsNullOrderById();
}
