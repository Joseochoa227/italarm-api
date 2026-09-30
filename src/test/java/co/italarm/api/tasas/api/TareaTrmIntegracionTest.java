package co.italarm.api.tasas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import co.italarm.api.tasas.aplicacion.ResultadoTrm;
import co.italarm.api.tasas.aplicacion.ServicioTrm;
import co.italarm.api.tasas.dominio.ResultadoEjecucion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Tarea de la TRM con la fuente simulada: éxito, falla, doble ejecución y P-13 (12.3). */
@ExtendWith(OutputCaptureExtension.class)
class TareaTrmIntegracionTest extends PruebaIntegracion {

  private static final Instant HOY = Instant.parse("2026-10-01T11:00:00Z");
  private static final LocalDate FECHA = LocalDate.of(2026, 10, 1);

  @Autowired ServicioTrm servicio;

  @BeforeEach
  void fijarFecha() {
    reloj.fijar(HOY);
  }

  private List<Map<String, Object>> ejecuciones() {
    return jdbc.queryForList(
        "select resultado, intento, detalle from ejecucion_tarea_trm order by id");
  }

  @Test
  void exito_guardaLaTrmDeHoyConFuenteSuperfinanciera() {
    fuenteTrm.responder("3912.45");

    ResultadoTrm resultado = servicio.actualizarTrmDeHoy();

    assertThat(resultado.resultado()).isEqualTo(ResultadoEjecucion.EXITO);
    assertThat(resultado.fecha()).isEqualTo(FECHA);
    Map<String, Object> tasa = jdbc.queryForMap("select * from tasa_cambio");
    assertThat(tasa.get("par")).isEqualTo("USD_COP");
    assertThat(tasa.get("fuente")).isEqualTo("SUPERFINANCIERA");
    assertThat(tasa.get("registrada_por")).isNull();
    assertThat(new java.math.BigDecimal(tasa.get("valor").toString()))
        .isEqualByComparingTo("3912.45");
    assertThat(ejecuciones())
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.get("resultado")).isEqualTo("EXITO");
              assertThat(e.get("intento")).isEqualTo(1);
            });
  }

  @Test
  void falla_registraLaEjecucionDejaErrorEnElLogYMarcaElFallo(CapturedOutput salida)
      throws Exception {
    fuenteTrm.fallar();

    ResultadoTrm resultado = servicio.actualizarTrmDeHoy();

    assertThat(resultado.resultado()).isEqualTo(ResultadoEjecucion.FALLO);
    assertThat(jdbc.queryForObject("select count(*) from tasa_cambio", Long.class)).isZero();
    assertThat(ejecuciones())
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.get("resultado")).isEqualTo("FALLO");
              assertThat(e.get("detalle")).isEqualTo("Servicio simulado sin respuesta");
            });
    assertThat(salida.getAll())
        .contains("ERROR")
        .contains("No se pudo obtener la TRM del 2026-10-01");

    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    mvc.perform(get("/api/v1/tasas/vigentes").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.trmAutomaticaFallo").value(true));
  }

  @Test
  void reintentoDespuesDeUnaFalla_guardaYCuentaElIntento() {
    fuenteTrm.fallar();
    servicio.actualizarTrmDeHoy();
    fuenteTrm.responder("3912.45");

    ResultadoTrm resultado = servicio.actualizarTrmDeHoy();

    assertThat(resultado.resultado()).isEqualTo(ResultadoEjecucion.EXITO);
    assertThat(ejecuciones()).extracting(e -> e.get("intento")).containsExactly(1, 2);
  }

  @Test
  void dobleEjecucion_laSegundaQuedaOmitidaYNoDuplica() {
    fuenteTrm.responder("3912.45");
    servicio.actualizarTrmDeHoy();

    ResultadoTrm segunda = servicio.actualizarTrmDeHoy();

    assertThat(segunda.resultado()).isEqualTo(ResultadoEjecucion.OMITIDA);
    assertThat(fuenteTrm.consultas()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from tasa_cambio", Long.class)).isEqualTo(1);
    assertThat(ejecuciones())
        .extracting(e -> e.get("resultado"))
        .containsExactly("EXITO", "OMITIDA");
  }

  @Test
  void p13_laTrmOficialReemplazaALaManualDelMismoDia() throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    mvc.perform(
            post("/api/v1/tasas/trm")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("valor", "3900", "confirmacion", "3900"))))
        .andExpect(status().isCreated());
    fuenteTrm.responder("3912.45");

    mvc.perform(
            post("/api/v1/tasas/trm/consultar").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resultado").value("EXITO"));

    long id = jdbc.queryForObject("select id from tasa_cambio", Long.class);
    mvc.perform(get("/api/v1/tasas/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.fuente").value("SUPERFINANCIERA"))
        .andExpect(jsonPath("$.valor").value("3912.450000"))
        .andExpect(jsonPath("$.correcciones[0].valorAnterior").value("3900.000000"))
        .andExpect(jsonPath("$.correcciones[0].automatica").value(true))
        .andExpect(jsonPath("$.correcciones[0].motivo").value("Reemplazada por la TRM oficial"));
  }
}
