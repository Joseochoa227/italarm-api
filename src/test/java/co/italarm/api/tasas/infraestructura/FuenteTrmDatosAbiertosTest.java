package co.italarm.api.tasas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import co.italarm.api.tasas.aplicacion.FuenteTrmNoDisponibleException;
import co.italarm.api.tasas.aplicacion.TrmPublicada;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La consulta a datos.gov.co contra un servidor HTTP simulado con el formato del conjunto. */
class FuenteTrmDatosAbiertosTest {

  private HttpServer servidor;
  private final AtomicReference<String> respuesta = new AtomicReference<>();
  private final AtomicReference<Integer> estado = new AtomicReference<>(200);
  private final AtomicReference<String> consultaRecibida = new AtomicReference<>();

  @BeforeEach
  void iniciar() throws Exception {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext(
        "/resource/32sa-8pi3.json",
        intercambio -> {
          consultaRecibida.set(
              URLDecoder.decode(intercambio.getRequestURI().getRawQuery(), StandardCharsets.UTF_8));
          byte[] cuerpo = respuesta.get().getBytes(StandardCharsets.UTF_8);
          intercambio.getResponseHeaders().add("Content-Type", "application/json");
          intercambio.sendResponseHeaders(estado.get(), cuerpo.length);
          try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
          }
        });
    servidor.start();
  }

  @AfterEach
  void detener() {
    servidor.stop(0);
  }

  private FuenteTrmDatosAbiertos fuente() {
    String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/resource/32sa-8pi3.json";
    return new FuenteTrmDatosAbiertos(
        new PropiedadesItalarm(
            new PropiedadesItalarm.Cors(List.of()),
            new PropiedadesItalarm.Usuarios(null),
            new PropiedadesItalarm.Almacenamiento(
                "disco",
                "x",
                "http://localhost",
                null,
                Duration.ofMinutes(1),
                new PropiedadesItalarm.S3(null, "auto", null, null, null)),
            new PropiedadesItalarm.Trm(url, false, false, Duration.ofSeconds(2))));
  }

  @Test
  void tomaLaTrmCuyaVigenciaCubreLaFecha() {
    respuesta.set(
        "[{\"valor\":\"3912.45\",\"unidad\":\"COP\",\"vigenciadesde\":\"2026-10-03T00:00:00.000\","
            + "\"vigenciahasta\":\"2026-10-05T00:00:00.000\"}]");

    TrmPublicada trm = fuente().consultar(LocalDate.of(2026, 10, 4));

    assertThat(trm.valor()).isEqualByComparingTo("3912.45");
    assertThat(trm.vigenciaDesde()).isEqualTo(LocalDate.of(2026, 10, 3));
    assertThat(trm.vigenciaHasta()).isEqualTo(LocalDate.of(2026, 10, 5));
    assertThat(consultaRecibida.get())
        .contains(
            "$where=vigenciadesde <= '2026-10-04T00:00:00.000' AND vigenciahasta >= '2026-10-04T00:00:00.000'")
        .contains("$order=vigenciadesde DESC")
        .contains("$limit=1");
  }

  @Test
  void sinDatosParaLaFechaFalla() {
    respuesta.set("[]");

    assertThatThrownBy(() -> fuente().consultar(LocalDate.of(2026, 10, 4)))
        .isInstanceOf(FuenteTrmNoDisponibleException.class)
        .hasMessage("La fuente no tiene la TRM del 2026-10-04");
  }

  @Test
  void unErrorDelServidorFalla() {
    respuesta.set("{}");
    estado.set(503);

    assertThatThrownBy(() -> fuente().consultar(LocalDate.of(2026, 10, 4)))
        .isInstanceOf(FuenteTrmNoDisponibleException.class)
        .hasMessageStartingWith("La fuente de la TRM no respondió");
  }

  @Test
  void unaRespuestaInvalidaFalla() {
    respuesta.set("[{\"valor\":\"abc\",\"vigenciadesde\":\"x\",\"vigenciahasta\":\"y\"}]");
    assertThatThrownBy(() -> fuente().consultar(LocalDate.of(2026, 10, 4)))
        .isInstanceOf(FuenteTrmNoDisponibleException.class)
        .hasMessage("La respuesta de la TRM no es válida");

    respuesta.set(
        "[{\"valor\":\"1\",\"unidad\":\"USD\",\"vigenciadesde\":\"2026-10-04T00:00:00.000\","
            + "\"vigenciahasta\":\"2026-10-04T00:00:00.000\"}]");
    assertThatThrownBy(() -> fuente().consultar(LocalDate.of(2026, 10, 4)))
        .hasMessage("Unidad inesperada en la TRM: USD");

    respuesta.set(
        "[{\"valor\":\"0\",\"vigenciadesde\":\"2026-10-04T00:00:00.000\","
            + "\"vigenciahasta\":\"2026-10-04T00:00:00.000\"}]");
    assertThatThrownBy(() -> fuente().consultar(LocalDate.of(2026, 10, 4)))
        .hasMessage("La fuente devolvió una TRM no positiva");
  }
}
