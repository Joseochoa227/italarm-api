package co.italarm.api.documentos.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.documentos.aplicacion.ServicioArchivos;
import co.italarm.api.documentos.dominio.ArchivoDemasiadoGrandeException;
import co.italarm.api.documentos.dominio.ArchivoTipoNoPermitidoException;
import co.italarm.api.soporte.PruebaIntegracion;
import java.net.URI;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Descarga con enlace firmado en modo disco: válido, adulterado y vencido. */
class ArchivoLocalIntegracionTest extends PruebaIntegracion {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};

  @Autowired ServicioArchivos archivos;

  /**
   * La ruta tal cual la recibe el navegador (ya codificada; MockMvc no debe volver a codificarla).
   */
  private URI rutaRelativa(String url) {
    URI uri = URI.create(url);
    return URI.create(uri.getRawPath() + "?" + uri.getRawQuery());
  }

  @Test
  void entregaElArchivoConUnEnlaceValidoSinIniciarSesion() throws Exception {
    String clave = archivos.guardarImagen("pruebas", "imagen", PNG);

    mvc.perform(get(rutaRelativa(archivos.urlDe(clave))))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"))
        .andExpect(header().string("Cache-Control", "max-age=900, private"))
        .andExpect(content().bytes(PNG));
  }

  @Test
  void rechazaUnEnlaceAdulterado() throws Exception {
    String clave = archivos.guardarImagen("pruebas", "imagen", PNG);
    URI adulterado =
        URI.create(
            rutaRelativa(archivos.urlDe(clave)).toString().replaceAll("firma=[0-9a-f]", "firma=g"));

    mvc.perform(get(adulterado))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ENLACE_INVALIDO"));
  }

  @Test
  void rechazaUnEnlaceVencido() throws Exception {
    String clave = archivos.guardarImagen("pruebas", "imagen", PNG);
    URI enlace = rutaRelativa(archivos.urlDe(clave));

    reloj.avanzar(Duration.ofMinutes(16));

    mvc.perform(get(enlace))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ENLACE_INVALIDO"));
  }

  @Test
  void rechazaUnaClaveMaliciosa() throws Exception {
    mvc.perform(get("/api/v1/archivos?clave=../../etc/passwd&expira=9999999999&firma=abc"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ENLACE_INVALIDO"));
  }

  @Test
  void unArchivoEliminadoResponde404() throws Exception {
    String clave = archivos.guardarImagen("pruebas", "imagen", PNG);
    URI enlace = rutaRelativa(archivos.urlDe(clave));

    archivos.eliminarAlConfirmar(clave);

    mvc.perform(get(enlace))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
  }

  @Test
  void validaLaImagenAntesDeGuardarla() {
    Assertions.assertThatThrownBy(
            () -> archivos.guardarImagen("pruebas", "x", new byte[] {'M', 'Z'}))
        .isInstanceOf(ArchivoTipoNoPermitidoException.class);
    Assertions.assertThatThrownBy(
            () -> archivos.guardarImagen("pruebas", "x", new byte[6 * 1024 * 1024]))
        .isInstanceOf(ArchivoDemasiadoGrandeException.class);
    Assertions.assertThat(archivos.urlDe(null)).isNull();
  }
}
