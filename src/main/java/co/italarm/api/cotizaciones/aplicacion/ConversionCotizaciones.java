package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.CotizacionOrigenVista;
import co.italarm.api.comercial.aplicacion.OrigenCotizacion;
import co.italarm.api.cotizaciones.dominio.Cotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.cotizaciones.infraestructura.CotizacionRepositorio;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.TipoDocumento;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enlace de la cotización con la venta o instalación generada (RF-93 a RF-95, RF-74). Corre dentro
 * de la transacción del registro o de la anulación del documento, así que todo se guarda o nada.
 */
@Service
public class ConversionCotizaciones implements OrigenCotizacion {

  private final CotizacionRepositorio cotizaciones;
  private final FechaNegocio fechas;

  public ConversionCotizaciones(CotizacionRepositorio cotizaciones, FechaNegocio fechas) {
    this.cotizaciones = cotizaciones;
    this.fechas = fechas;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void validarConversion(Long cotizacionId, TipoDocumento tipo, Long clienteId) {
    bloquear(cotizacionId).validarConversion(tipo(tipo), clienteId);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void convertir(Long cotizacionId, DocumentoRef documento, Long clienteId) {
    bloquear(cotizacionId)
        .convertir(
            tipo(documento.tipo()),
            clienteId,
            documento.id(),
            documento.consecutivo(),
            fechas.ahora());
    cotizaciones.flush();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void revertir(Long cotizacionId, Long documentoId) {
    bloquear(cotizacionId).revertirConversion(documentoId);
    cotizaciones.flush();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CotizacionOrigenVista> origen(Long cotizacionId) {
    return cotizacionId == null
        ? Optional.empty()
        : cotizaciones
            .findById(cotizacionId)
            .map(c -> new CotizacionOrigenVista(c.getId(), c.consecutivo()));
  }

  private Cotizacion bloquear(Long id) {
    return cotizaciones
        .bloquear(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
  }

  private static TipoCotizacion tipo(TipoDocumento tipo) {
    return switch (tipo) {
      case VENTA -> TipoCotizacion.VENTA;
      case INSTALACION -> TipoCotizacion.INSTALACION;
      default -> throw new IllegalArgumentException("Una cotización no se convierte en " + tipo);
    };
  }
}
