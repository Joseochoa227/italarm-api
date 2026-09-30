package co.italarm.api.terceros.aplicacion;

import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.terceros.dominio.Cliente;
import co.italarm.api.terceros.dominio.ClienteDocumentoDuplicadoException;
import co.italarm.api.terceros.dominio.DatosCliente;
import co.italarm.api.terceros.dominio.Documentos;
import co.italarm.api.terceros.dominio.TipoCliente;
import co.italarm.api.terceros.infraestructura.ClienteRepositorio;
import co.italarm.api.terceros.infraestructura.EspecificacionesTerceros;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clientes (sección 3.10): crear, editar y consultar. No se eliminan (P-11). */
@Service
public class ServicioClientes {

  private final ClienteRepositorio clientes;

  public ServicioClientes(ClienteRepositorio clientes) {
    this.clientes = clientes;
  }

  @Transactional(readOnly = true)
  public Page<ClienteVista> buscar(TipoCliente tipo, String buscar, Pageable pagina) {
    return clientes
        .findAll(EspecificacionesTerceros.clientes(tipo, buscar), pagina)
        .map(ClienteVista::de);
  }

  @Transactional(readOnly = true)
  public ClienteVista detalle(Long id) {
    return ClienteVista.de(buscarCliente(id));
  }

  @Transactional
  public ClienteVista crear(DatosCliente datos) {
    exigirDocumentoUnico(datos, null);
    return ClienteVista.de(clientes.saveAndFlush(Cliente.crear(datos)));
  }

  @Transactional
  public ClienteVista actualizar(Long id, DatosCliente datos, long version) {
    Cliente cliente = buscarCliente(id);
    cliente.verificarVersion(version);
    exigirDocumentoUnico(datos, id);
    cliente.actualizar(datos);
    clientes.flush();
    return ClienteVista.de(cliente);
  }

  private void exigirDocumentoUnico(DatosCliente datos, Long id) {
    String numero = Documentos.normalizar(datos.numeroDocumento());
    if (datos.tipoDocumento() != null
        && numero != null
        && clientes.existeDocumento(datos.tipoDocumento(), numero, id)) {
      throw new ClienteDocumentoDuplicadoException();
    }
  }

  private Cliente buscarCliente(Long id) {
    return clientes
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe."));
  }
}
