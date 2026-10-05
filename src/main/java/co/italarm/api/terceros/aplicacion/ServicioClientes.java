package co.italarm.api.terceros.aplicacion;

import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.terceros.dominio.Cliente;
import co.italarm.api.terceros.dominio.ClienteDocumentoDuplicadoException;
import co.italarm.api.terceros.dominio.DatosCliente;
import co.italarm.api.terceros.dominio.Documentos;
import co.italarm.api.terceros.dominio.TipoCliente;
import co.italarm.api.terceros.infraestructura.ClienteRepositorio;
import co.italarm.api.terceros.infraestructura.EspecificacionesTerceros;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clientes (sección 3.10): crear, editar y consultar. No se eliminan (P-11). */
@Service
public class ServicioClientes {

  private final ClienteRepositorio clientes;
  private final List<MovimientosCliente> movimientos;

  public ServicioClientes(ClienteRepositorio clientes, List<MovimientosCliente> movimientos) {
    this.clientes = clientes;
    this.movimientos = movimientos;
  }

  @Transactional(readOnly = true)
  public Page<ClienteVista> buscar(TipoCliente tipo, String buscar, Pageable pagina) {
    Page<Cliente> encontrados =
        clientes.findAll(EspecificacionesTerceros.clientes(tipo, buscar), pagina);
    Map<Long, MovimientosCliente.Resumen> resumen =
        resumen(encontrados.getContent().stream().map(Cliente::getId).toList());
    return encontrados.map(c -> ClienteVista.de(c, resumen.get(c.getId())));
  }

  @Transactional(readOnly = true)
  public ClienteVista detalle(Long id) {
    return vista(buscarCliente(id));
  }

  /** Documentos del cliente, del más reciente al más antiguo (RF-77). */
  @Transactional(readOnly = true)
  public HistorialClienteVista historial(Long id) {
    Cliente cliente = buscarCliente(id);
    List<MovimientosCliente.Movimiento> todos =
        movimientos.stream()
            .flatMap(m -> m.historial(id).stream())
            .sorted(
                Comparator.comparing(MovimientosCliente.Movimiento::fecha)
                    .thenComparing(MovimientosCliente.Movimiento::id)
                    .reversed())
            .toList();
    return new HistorialClienteVista(
        cliente.getId(),
        cliente.getNombre(),
        contar(todos, "VENTA"),
        contar(todos, "INSTALACION"),
        todos);
  }

  private static long contar(List<MovimientosCliente.Movimiento> todos, String tipo) {
    return todos.stream()
        .filter(m -> tipo.equals(m.tipo()) && !"ANULADA".equals(m.estado()))
        .count();
  }

  /** Suma lo que reporta cada módulo: cantidad y fecha del último documento. */
  private Map<Long, MovimientosCliente.Resumen> resumen(List<Long> ids) {
    Map<Long, MovimientosCliente.Resumen> total = new HashMap<>();
    if (ids.isEmpty()) {
      return total;
    }
    for (MovimientosCliente fuente : movimientos) {
      fuente
          .resumen(ids)
          .forEach(
              (id, parcial) ->
                  total.merge(
                      id,
                      parcial,
                      (a, b) ->
                          new MovimientosCliente.Resumen(
                              a.cantidad() + b.cantidad(),
                              a.ultimo() == null
                                      || (b.ultimo() != null && b.ultimo().isAfter(a.ultimo()))
                                  ? b.ultimo()
                                  : a.ultimo())));
    }
    return total;
  }

  private ClienteVista vista(Cliente cliente) {
    return ClienteVista.de(cliente, resumen(List.of(cliente.getId())).get(cliente.getId()));
  }

  @Transactional
  public ClienteVista crear(DatosCliente datos) {
    exigirDocumentoUnico(datos, null);
    return vista(clientes.saveAndFlush(Cliente.crear(datos)));
  }

  @Transactional
  public ClienteVista actualizar(Long id, DatosCliente datos, long version) {
    Cliente cliente = buscarCliente(id);
    cliente.verificarVersion(version);
    exigirDocumentoUnico(datos, id);
    cliente.actualizar(datos);
    clientes.flush();
    return vista(cliente);
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
