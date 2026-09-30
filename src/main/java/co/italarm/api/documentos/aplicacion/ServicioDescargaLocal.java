package co.italarm.api.documentos.aplicacion;

import co.italarm.api.documentos.dominio.ClaveArchivo;
import co.italarm.api.documentos.dominio.EnlaceInvalidoException;
import co.italarm.api.documentos.dominio.FirmaEnlace;
import co.italarm.api.documentos.dominio.TipoImagen;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Entrega archivos del almacenamiento en disco a partir de un enlace firmado. Solo existe en modo
 * disco (desarrollo local); en S3 los enlaces los firma y sirve el propio proveedor.
 */
@Service
@ConditionalOnProperty(
    name = "italarm.almacenamiento.tipo",
    havingValue = "disco",
    matchIfMissing = true)
public class ServicioDescargaLocal {

  private final AlmacenamientoArchivos almacenamiento;
  private final FirmaEnlace firma;
  private final Clock reloj;

  public ServicioDescargaLocal(
      AlmacenamientoArchivos almacenamiento, FirmaEnlace firma, Clock reloj) {
    this.almacenamiento = almacenamiento;
    this.firma = firma;
    this.reloj = reloj;
  }

  public ArchivoDescargado descargar(String clave, long expira, String firmaRecibida) {
    try {
      ClaveArchivo.validar(clave);
    } catch (IllegalArgumentException e) {
      throw new EnlaceInvalidoException();
    }
    if (!firma.esValido(clave, expira, firmaRecibida, reloj.instant())) {
      throw new EnlaceInvalidoException();
    }
    byte[] contenido =
        almacenamiento
            .leer(clave)
            .orElseThrow(() -> new RecursoNoEncontradoException("El archivo ya no existe."));
    String tipo =
        TipoImagen.deExtension(clave)
            .map(TipoImagen::tipoContenido)
            .orElse("application/octet-stream");
    return new ArchivoDescargado(contenido, tipo);
  }
}
