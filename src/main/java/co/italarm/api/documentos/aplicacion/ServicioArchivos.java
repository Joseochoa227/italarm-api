package co.italarm.api.documentos.aplicacion;

import co.italarm.api.documentos.dominio.ClaveArchivo;
import co.italarm.api.documentos.dominio.TipoImagen;
import co.italarm.api.documentos.dominio.ValidadorImagen;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Guarda imágenes validadas y entrega enlaces firmados. Lo usan los demás módulos. */
@Service
public class ServicioArchivos {

  private static final Logger LOG = LoggerFactory.getLogger(ServicioArchivos.class);

  private final AlmacenamientoArchivos almacenamiento;

  public ServicioArchivos(AlmacenamientoArchivos almacenamiento) {
    this.almacenamiento = almacenamiento;
  }

  /**
   * Valida la imagen (tipo real y tamaño) y la guarda con una clave generada por el sistema, por
   * ejemplo {@code productos/12/foto-3f9a2c1b.jpg}. Devuelve la clave.
   */
  public String guardarImagen(String carpeta, String nombreBase, byte[] contenido) {
    TipoImagen tipo = ValidadorImagen.validar(contenido);
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    String clave =
        ClaveArchivo.validar(carpeta + "/" + nombreBase + "-" + sufijo + "." + tipo.extension());
    almacenamiento.guardar(clave, contenido, tipo.tipoContenido());
    return clave;
  }

  /** Enlace firmado para mostrar el archivo, o {@code null} si no hay archivo. */
  public String urlDe(String clave) {
    return clave == null ? null : almacenamiento.urlFirmada(clave);
  }

  /**
   * Elimina el archivo cuando la transacción en curso se confirme, para no perderlo si la operación
   * se revierte. Si no hay transacción, lo elimina de inmediato.
   */
  public void eliminarAlConfirmar(String clave) {
    if (clave == null) {
      return;
    }
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              eliminarSinFallar(clave);
            }
          });
    } else {
      eliminarSinFallar(clave);
    }
  }

  private void eliminarSinFallar(String clave) {
    try {
      almacenamiento.eliminar(clave);
    } catch (RuntimeException e) {
      LOG.warn("No se pudo eliminar el archivo {}; queda huérfano", clave, e);
    }
  }
}
