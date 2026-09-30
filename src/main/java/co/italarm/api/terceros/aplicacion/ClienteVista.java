package co.italarm.api.terceros.aplicacion;

import co.italarm.api.terceros.dominio.Cliente;
import co.italarm.api.terceros.dominio.PrecioAplicado;
import co.italarm.api.terceros.dominio.TipoCliente;
import co.italarm.api.terceros.dominio.TipoDocumento;
import java.time.LocalDate;

/**
 * Cliente como lo ve el frontend. {@code cantidadMovimientos} y {@code fechaUltimoMovimiento} se
 * llenan cuando existan ventas, instalaciones y cotizaciones (fases 3 a 5).
 */
public record ClienteVista(
    Long id,
    TipoCliente tipo,
    PrecioAplicado precioAplicado,
    String precioAplicadoDescripcion,
    String nombre,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String telefono,
    String correo,
    String direccion,
    String ciudad,
    long cantidadMovimientos,
    LocalDate fechaUltimoMovimiento,
    long version) {

  static ClienteVista de(Cliente cliente) {
    return new ClienteVista(
        cliente.getId(),
        cliente.getTipo(),
        cliente.precioAplicado(),
        cliente.precioAplicado().descripcion(),
        cliente.getNombre(),
        cliente.getTipoDocumento(),
        cliente.getNumeroDocumento(),
        cliente.getTelefono(),
        cliente.getCorreo(),
        cliente.getDireccion(),
        cliente.getCiudad(),
        0,
        null,
        cliente.getVersion());
  }
}
