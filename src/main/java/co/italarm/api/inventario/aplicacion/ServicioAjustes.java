package co.italarm.api.inventario.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.DatosProductoInventario;
import co.italarm.api.inventario.dominio.Ajuste;
import co.italarm.api.inventario.dominio.Serial;
import co.italarm.api.inventario.dominio.TipoAjuste;
import co.italarm.api.inventario.infraestructura.AjusteRepositorio;
import co.italarm.api.inventario.infraestructura.EspecificacionesInventario;
import co.italarm.api.inventario.infraestructura.SerialRepositorio;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ajustes de inventario (RF-58 a RF-62). No se editan ni se anulan (RF-70, P-24). */
@Service
public class ServicioAjustes {

  private final AjusteRepositorio ajustes;
  private final SerialRepositorio seriales;
  private final RegistroAjustes registro;
  private final ServicioIdempotencia idempotencia;
  private final ConsultaProductos productos;
  private final ConsultaUsuarios usuarios;

  public ServicioAjustes(
      AjusteRepositorio ajustes,
      SerialRepositorio seriales,
      RegistroAjustes registro,
      ServicioIdempotencia idempotencia,
      ConsultaProductos productos,
      ConsultaUsuarios usuarios) {
    this.ajustes = ajustes;
    this.seriales = seriales;
    this.registro = registro;
    this.idempotencia = idempotencia;
    this.productos = productos;
    this.usuarios = usuarios;
  }

  /** Registra el ajuste una sola vez por {@code Idempotency-Key} (RT-07) y devuelve su id. */
  public Long registrar(DatosAjuste datos, Long usuarioId, ClaveIdempotencia clave) {
    return idempotencia.ejecutar(clave, () -> registro.registrar(datos, usuarioId, clave));
  }

  @Transactional(readOnly = true)
  public Page<AjusteVista> listar(
      Long productoId, LocalDate desde, LocalDate hasta, Pageable pagina) {
    Page<Ajuste> encontrados =
        ajustes.findAll(EspecificacionesInventario.ajustes(productoId, desde, hasta), pagina);
    Map<Long, DatosProductoInventario> datos =
        productos.porId(
            encontrados.getContent().stream()
                .map(Ajuste::getProductoId)
                .collect(Collectors.toSet()));
    Map<Long, String> nombres =
        usuarios.nombres(
            encontrados.getContent().stream()
                .map(Ajuste::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    return encontrados.map(a -> vista(a, datos, nombres, seriales(a)));
  }

  @Transactional(readOnly = true)
  public AjusteVista detalle(Long id) {
    Ajuste ajuste =
        ajustes
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El ajuste no existe."));
    return vista(
        ajuste,
        productos.porId(List.of(ajuste.getProductoId())),
        ajuste.getCreatedBy() == null ? Map.of() : usuarios.nombres(List.of(ajuste.getCreatedBy())),
        seriales(ajuste));
  }

  private List<String> seriales(Ajuste ajuste) {
    DocumentoRef documento = ajuste.documento();
    List<Serial> encontrados =
        ajuste.getTipo() == TipoAjuste.ENTRADA
            ? seriales.findByDocumentoEntradaTipoAndDocumentoEntradaIdOrderById(
                documento.tipo(), documento.id())
            : seriales.findByDocumentoSalidaTipoAndDocumentoSalidaIdOrderById(
                documento.tipo(), documento.id());
    return encontrados.stream().map(Serial::getNumero).toList();
  }

  private static AjusteVista vista(
      Ajuste ajuste,
      Map<Long, DatosProductoInventario> datos,
      Map<Long, String> nombres,
      List<String> numeros) {
    DatosProductoInventario producto = datos.get(ajuste.getProductoId());
    return new AjusteVista(
        ajuste.getId(),
        ajuste.documento().consecutivo(),
        ajuste.getFecha(),
        new ProductoReferencia(producto.id(), producto.codigo(), producto.nombre()),
        ajuste.getTipo().name(),
        ajuste.getMotivo().name(),
        ajuste.getMotivo().etiqueta(),
        ajuste.getDescripcion(),
        Cantidades.sinCeros(ajuste.getCantidad()),
        producto.abreviatura(),
        new Dinero(ajuste.getCostoUnitarioUsd(), Moneda.USD),
        new Dinero(
            Redondeo.paraAlmacenar(ajuste.getCantidad().multiply(ajuste.getCostoUnitarioUsd())),
            Moneda.USD),
        numeros,
        nombres.get(ajuste.getCreatedBy()),
        ajuste.getCreatedAt());
  }
}
