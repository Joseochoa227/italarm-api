package co.italarm.api.documentos.aplicacion;

import co.italarm.api.documentos.dominio.EnlaceComprobante;
import co.italarm.api.documentos.infraestructura.EnlaceComprobanteRepositorio;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.shared.dominio.TokenAleatorio;
import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enlaces públicos de los comprobantes para WhatsApp (RF-134, P-33): cualquiera que tenga el enlace
 * descarga el PDF sin sesión, hasta que vence.
 */
@Service
public class ServicioEnlacesComprobante {

  /** P-33: el enlace funciona 30 días. */
  public static final Duration VIGENCIA = Duration.ofDays(30);

  static final String RUTA = "/api/v1/comprobantes/";

  private final EnlaceComprobanteRepositorio enlaces;
  private final Map<TipoDocumento, FuenteComprobantes> fuentes;
  private final PropiedadesItalarm propiedades;
  private final Clock reloj;
  private final SecureRandom aleatorio = new SecureRandom();

  public ServicioEnlacesComprobante(
      EnlaceComprobanteRepositorio enlaces,
      List<FuenteComprobantes> fuentes,
      PropiedadesItalarm propiedades,
      Clock reloj) {
    this.enlaces = enlaces;
    this.fuentes =
        fuentes.stream().collect(Collectors.toMap(FuenteComprobantes::tipo, Function.identity()));
    this.propiedades = propiedades;
    this.reloj = reloj;
  }

  @Transactional
  public EnlaceCreado crear(TipoDocumento tipo, Long documentoId, Long usuarioId) {
    TokenAleatorio token = TokenAleatorio.generar(aleatorio);
    Instant ahora = reloj.instant();
    Instant vence = ahora.plus(VIGENCIA);
    enlaces.save(new EnlaceComprobante(token.hash(), tipo, documentoId, ahora, vence, usuarioId));
    String base = propiedades.almacenamiento().urlPublica().replaceAll("/+$", "");
    return new EnlaceCreado(base + RUTA + token.valor(), vence);
  }

  /** PDF del enlace, si existe y no ha vencido. */
  @Transactional(readOnly = true)
  public ArchivoGenerado descargar(String token) {
    EnlaceComprobante enlace =
        enlaces
            .findByTokenHash(TokenAleatorio.hashDe(token))
            .filter(e -> e.vigenteEn(reloj.instant()))
            .orElseThrow(
                () ->
                    new RecursoNoEncontradoException(
                        "El enlace no existe o ya venció. Pide uno nuevo a ITALARM."));
    FuenteComprobantes fuente = fuentes.get(enlace.getDocumentoTipo());
    if (fuente == null) {
      throw new RecursoNoEncontradoException("El documento no existe.");
    }
    return fuente.pdf(enlace.getDocumentoId());
  }
}
