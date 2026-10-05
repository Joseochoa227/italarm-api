package co.italarm.api.terceros.aplicacion;

import co.italarm.api.terceros.dominio.Cliente;
import co.italarm.api.terceros.dominio.PrecioAplicado;
import co.italarm.api.terceros.infraestructura.ClienteRepositorio;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos de clientes para los documentos de otros módulos (ventas). */
@Service
public class ConsultaClientes {

  private final ClienteRepositorio clientes;

  public ConsultaClientes(ClienteRepositorio clientes) {
    this.clientes = clientes;
  }

  @Transactional(readOnly = true)
  public Optional<ClienteDocumento> porId(Long id) {
    return clientes.findById(id).map(ConsultaClientes::documento);
  }

  private static ClienteDocumento documento(Cliente c) {
    return new ClienteDocumento(
        c.getId(),
        c.getTipo().name(),
        c.precioAplicado() == PrecioAplicado.INSTALADOR,
        c.precioAplicado().descripcion(),
        c.getNombre(),
        c.getTipoDocumento() == null || c.getNumeroDocumento() == null
            ? null
            : c.getTipoDocumento().name() + " " + c.getNumeroDocumento(),
        c.getTelefono(),
        c.getDireccion(),
        c.getCiudad());
  }
}
