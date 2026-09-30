package co.italarm.api.usuarios.aplicacion;

import co.italarm.api.usuarios.dominio.Usuario;

/** Usuario en la gestión de usuarios (RF-148). */
public record UsuarioVista(Long id, String nombre, String correo, boolean activo, long version) {

  static UsuarioVista de(Usuario usuario) {
    return new UsuarioVista(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getCorreo(),
        usuario.isActivo(),
        usuario.getVersion());
  }
}
