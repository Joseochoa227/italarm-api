package co.italarm.api.garantias.aplicacion;

import co.italarm.api.garantias.dominio.ReclamoGarantia;
import co.italarm.api.garantias.dominio.ReclamoInvalidoException;
import co.italarm.api.garantias.infraestructura.ConsultaGarantiasSql;
import co.italarm.api.garantias.infraestructura.ConsultaGarantiasSql.FilaGarantia;
import co.italarm.api.garantias.infraestructura.EspecificacionesReclamos;
import co.italarm.api.garantias.infraestructura.ReclamoGarantiaRepositorio;
import co.italarm.api.inventario.aplicacion.ReclamosSerial;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.EstadoGarantia;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Garantia;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de garantías y reclamos (sección 3.14). */
@Service
public class ServicioGarantias implements ReclamosSerial {

  private final ConsultaGarantiasSql garantias;
  private final ReclamoGarantiaRepositorio reclamos;
  private final ConsultaUsuarios usuarios;
  private final FechaNegocio fechas;

  public ServicioGarantias(
      ConsultaGarantiasSql garantias,
      ReclamoGarantiaRepositorio reclamos,
      ConsultaUsuarios usuarios,
      FechaNegocio fechas) {
    this.garantias = garantias;
    this.reclamos = reclamos;
    this.usuarios = usuarios;
    this.fechas = fechas;
  }

  /**
   * Garantías de ventas e instalaciones no anuladas, de la que vence primero a la última (RF-123,
   * RF-124).
   *
   * @param tipo INSTALACION o VENTA
   */
  @Transactional(readOnly = true)
  public Page<GarantiaVista> consultar(
      EstadoGarantia estado, Long clienteId, String tipo, String serial, Pageable pagina) {
    LocalDate hoy = fechas.hoy();
    return garantias
        .buscar(new ConsultaGarantiasSql.Filtro(estado, clienteId, tipo, serial, hoy), pagina)
        .map(fila -> vista(fila, hoy));
  }

  /** Registra un reclamo sobre una instalación o un serial (RF-125, P-45). */
  @Transactional
  public ReclamoVista registrarReclamo(DatosReclamo datos) {
    if ((datos.instalacionId() == null) == (datos.serialId() == null)) {
      throw new ReclamoInvalidoException("El reclamo es sobre una instalación o sobre un serial.");
    }
    FilaGarantia garantia = garantia(datos.instalacionId(), datos.serialId());
    LocalDate hoy = fechas.hoy();
    ReclamoGarantia reclamo =
        reclamos.saveAndFlush(
            ReclamoGarantia.registrar(
                datos.instalacionId(),
                datos.serialId(),
                garantia.clienteId(),
                datos.fecha() == null ? hoy : datos.fecha(),
                hoy,
                garantia.vencimiento(),
                datos.problema(),
                datos.solucion()));
    return vista(reclamo, garantia);
  }

  /** Escribe o corrige la solución de un reclamo. */
  @Transactional
  public ReclamoVista cambiarSolucion(Long id, String solucion, long version) {
    ReclamoGarantia reclamo =
        reclamos
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El reclamo no existe."));
    reclamo.verificarVersion(version);
    reclamo.cambiarSolucion(solucion);
    reclamos.flush();
    return vista(reclamo, garantiaSiExiste(reclamo));
  }

  /** Reclamos registrados, del más reciente al más antiguo. */
  @Transactional(readOnly = true)
  public List<ReclamoVista> reclamos(Long instalacionId, Long serialId, Long clienteId) {
    return reclamos
        .findAll(
            EspecificacionesReclamos.filtrar(instalacionId, serialId, clienteId),
            Sort.by(Sort.Direction.DESC, "fecha", "id"))
        .stream()
        .map(r -> vista(r, garantiaSiExiste(r)))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReclamosSerial.Reclamo> deSerial(Long serialId) {
    return reclamos
        .findAll(
            EspecificacionesReclamos.filtrar(null, serialId, null),
            Sort.by(Sort.Direction.ASC, "fecha", "id"))
        .stream()
        .map(
            r ->
                new ReclamosSerial.Reclamo(
                    r.getId(), r.getFecha(), r.getProblema(), r.getSolucion(), r.isEnGarantia()))
        .toList();
  }

  private FilaGarantia garantia(Long instalacionId, Long serialId) {
    return (instalacionId != null
            ? garantias.deInstalacion(instalacionId)
            : garantias.deSerial(serialId))
        .orElseThrow(
            () ->
                new RecursoNoEncontradoException(
                    instalacionId != null
                        ? "La instalación no existe o está anulada."
                        : "El serial no ha salido en una venta o instalación vigente."));
  }

  private FilaGarantia garantiaSiExiste(ReclamoGarantia reclamo) {
    return (reclamo.getInstalacionId() != null
            ? garantias.deInstalacion(reclamo.getInstalacionId())
            : garantias.deSerial(reclamo.getSerialId()))
        .orElse(null);
  }

  private ReclamoVista vista(ReclamoGarantia reclamo, FilaGarantia garantia) {
    Map<Long, String> nombres =
        reclamo.getCreatedBy() == null
            ? Map.of()
            : usuarios.nombres(List.of(reclamo.getCreatedBy()));
    return new ReclamoVista(
        reclamo.getId(),
        reclamo.getInstalacionId() != null ? "INSTALACION" : "SERIAL",
        reclamo.getInstalacionId(),
        reclamo.getSerialId(),
        garantia == null ? null : garantia.serialNumero(),
        garantia == null ? null : documento(garantia).consecutivo(),
        reclamo.getClienteId(),
        garantia == null ? null : garantia.clienteNombre(),
        reclamo.getFecha(),
        reclamo.getProblema(),
        reclamo.getSolucion(),
        reclamo.isEnGarantia(),
        garantia == null ? null : garantia.vencimiento(),
        nombres.get(reclamo.getCreatedBy()),
        reclamo.getCreatedAt(),
        reclamo.getVersion());
  }

  private static GarantiaVista vista(FilaGarantia fila, LocalDate hoy) {
    return new GarantiaVista(
        fila.tipo(),
        fila.clase(),
        documento(fila),
        fila.fecha(),
        fila.clienteId(),
        fila.clienteNombre(),
        fila.serialId(),
        fila.serialNumero(),
        fila.productoNombre(),
        fila.vencimiento(),
        Garantia.estado(fila.vencimiento(), hoy),
        Garantia.diasRestantes(fila.vencimiento(), hoy));
  }

  private static DocumentoRef documento(FilaGarantia fila) {
    TipoDocumento tipo = TipoDocumento.valueOf(fila.tipo());
    return new DocumentoRef(tipo, fila.documentoId(), tipo.consecutivo(fila.documentoNumero()));
  }
}
