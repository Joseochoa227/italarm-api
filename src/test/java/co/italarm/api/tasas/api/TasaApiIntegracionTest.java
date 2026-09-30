package co.italarm.api.tasas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TasaApiIntegracionTest extends PruebaIntegracion {

  /** 1 de octubre de 2026, 8:00 a. m. en Bogotá. */
  private static final Instant HOY = Instant.parse("2026-10-01T13:00:00Z");

  private String token;
  private long idJose;

  @BeforeEach
  void preparar() throws Exception {
    reloj.fijar(HOY);
    token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    idJose =
        jdbc.queryForObject("select id from usuario where correo = ?", Long.class, CORREO_JOSE);
  }

  private void tasaPrevia(String par, String fecha, String valor) {
    String fuente = par.equals("USD_COP") ? "SUPERFINANCIERA" : "MANUAL";
    jdbc.update(
        "insert into tasa_cambio (par, fecha, valor, fuente, registrada_por, registrada_en)"
            + " values (?, cast(? as date), cast(? as numeric), ?, ?, now())",
        par,
        fecha,
        valor,
        fuente,
        fuente.equals("MANUAL") ? idJose : null);
  }

  private static Map<String, Object> manual(String valor, String confirmacion, boolean aceptar) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("valor", valor);
    datos.put("confirmacion", confirmacion);
    datos.put("aceptarVariacion", aceptar);
    return datos;
  }

  private ResultActions publicar(String ruta, Object cuerpo) throws Exception {
    return mvc.perform(
        post(ruta)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(cuerpo)));
  }

  private ResultActions obtener(String ruta) throws Exception {
    return mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  private long cantidadTasas() {
    return jdbc.queryForObject("select count(*) from tasa_cambio", Long.class);
  }

  @Test
  void cp10_tasaDelBolivarDigitadaDistintaDosVeces_noSeGuarda() throws Exception {
    publicar("/api/v1/tasas/ves", manual("36.50", "36.05", false))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("TASA_NO_CONFIRMADA"))
        .andExpect(
            jsonPath("$.detail").value("Las dos tasas digitadas no coinciden. Digítala de nuevo."));

    assertThat(cantidadTasas()).isZero();
  }

  @Test
  void cp11_tasaAnterior50YNueva500_exigeAceptarLaVariacion() throws Exception {
    tasaPrevia("USD_VES", "2026-09-30", "50");

    publicar("/api/v1/tasas/vista-previa", Map.of("par", "USD_VES", "valor", "500"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anterior").value("50.000000"))
        .andExpect(jsonPath("$.porcentaje").value("900.00"))
        .andExpect(jsonPath("$.limite").value("5.0000"))
        .andExpect(jsonPath("$.superaLimite").value(true));

    publicar("/api/v1/tasas/ves", manual("500", "500", false))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("TASA_VARIACION_NO_ACEPTADA"));
    assertThat(cantidadTasas()).isEqualTo(1);

    publicar("/api/v1/tasas/ves", manual("500", "500", true))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.valor").value("500.000000"))
        .andExpect(jsonPath("$.fecha").value("2026-10-01"))
        .andExpect(jsonPath("$.fuente").value("MANUAL"))
        .andExpect(jsonPath("$.registradaPor").value("Jose"));
  }

  @Test
  void cp12_sinTasaDelBolivarDeHoy_usaLaUltimaYAvisa() throws Exception {
    tasaPrevia("USD_VES", "2026-09-29", "36.5");
    tasaPrevia("USD_COP", "2026-10-01", "3912.45");

    obtener("/api/v1/tasas/vigentes")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hoy").value("2026-10-01"))
        .andExpect(jsonPath("$.bolivar.valor").value("36.500000"))
        .andExpect(jsonPath("$.bolivar.fecha").value("2026-09-29"))
        .andExpect(jsonPath("$.bolivar.esDeHoy").value(false))
        .andExpect(jsonPath("$.bolivar.registradaPor").value("Jose"))
        .andExpect(
            jsonPath("$.bolivar.aviso")
                .value(
                    "No se ha registrado la tasa del bolívar de hoy. Se está usando la del"
                        + " 29/09/2026."))
        .andExpect(jsonPath("$.trm.esDeHoy").value(true))
        .andExpect(jsonPath("$.trm.fuente").value("SUPERFINANCIERA"))
        .andExpect(jsonPath("$.trm.aviso").value(nullValue()))
        .andExpect(jsonPath("$.trmAutomaticaFallo").value(false));
  }

  @Test
  void sinNingunaTasaLoIndica() throws Exception {
    obtener("/api/v1/tasas/vigentes")
        .andExpect(jsonPath("$.trm.valor").value(nullValue()))
        .andExpect(
            jsonPath("$.trm.aviso")
                .value("No hay TRM registrada. Regístrala para convertir a pesos."))
        .andExpect(jsonPath("$.bolivar.esDeHoy").value(false));
  }

  @Test
  void laFechaDelDiaEsLaDeColombia() throws Exception {
    // 23:30 del 1 de octubre en Bogotá = 04:30 UTC del 2 de octubre.
    reloj.fijar(Instant.parse("2026-10-02T04:30:00Z"));

    publicar("/api/v1/tasas/ves", manual("36.5", "36.5", false))
        .andExpect(jsonPath("$.fecha").value("2026-10-01"));
  }

  @Test
  void laTasaDelBolivarSeRegistraUnaVezPorDia() throws Exception {
    publicar("/api/v1/tasas/ves", manual("36.5", "36.5", false)).andExpect(status().isCreated());

    publicar("/api/v1/tasas/ves", manual("36.6", "36.6", false))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("TASA_YA_REGISTRADA"));
  }

  @Test
  void laTrmManualSoloSiNoEstaLaOficial() throws Exception {
    tasaPrevia("USD_COP", "2026-10-01", "3912.45");

    publicar("/api/v1/tasas/trm", manual("3900", "3900", false))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("TRM_AUTOMATICA_DISPONIBLE"));
  }

  @Test
  void laTrmManualSeRegistraSiFaltaLaOficial() throws Exception {
    tasaPrevia("USD_COP", "2026-09-30", "3910");

    publicar("/api/v1/tasas/trm", manual("3912.45", "3912.45", false))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.par").value("USD_COP"))
        .andExpect(jsonPath("$.fuente").value("MANUAL"));
  }

  @Test
  void corrigeUnaTasaYGuardaElHistorial() throws Exception {
    String registrada =
        publicar("/api/v1/tasas/ves", manual("36.5", "36.5", false))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(registrada).get("id").asLong();
    String tokenVictor = ingresar(CORREO_VICTOR, CLAVE_INICIAL);

    Map<String, Object> correccion = manual("36.05", "36.05", false);
    correccion.put("motivo", "Error de digitación");
    mvc.perform(
            post("/api/v1/tasas/" + id + "/corregir")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenVictor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(correccion)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valor").value("36.050000"))
        .andExpect(jsonPath("$.registradaPor").value("Jose"))
        .andExpect(jsonPath("$.correcciones", hasSize(1)))
        .andExpect(jsonPath("$.correcciones[0].valorAnterior").value("36.500000"))
        .andExpect(jsonPath("$.correcciones[0].valorNuevo").value("36.050000"))
        .andExpect(jsonPath("$.correcciones[0].corregidaPor").value("Victor"))
        .andExpect(jsonPath("$.correcciones[0].motivo").value("Error de digitación"))
        .andExpect(jsonPath("$.correcciones[0].automatica").value(false));

    publicar("/api/v1/tasas/" + id + "/corregir", manual("36.05", "36.05", false))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("TASA_SIN_CAMBIO"));
    publicar("/api/v1/tasas/" + id + "/corregir", manual("360", "360", false))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("TASA_VARIACION_NO_ACEPTADA"));
    publicar("/api/v1/tasas/" + id + "/corregir", manual("36", "36.1", false))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("TASA_NO_CONFIRMADA"));
  }

  @Test
  void laVistaPreviaDeUnaCorreccionComparaConEsaTasa() throws Exception {
    tasaPrevia("USD_VES", "2026-09-20", "40");
    long id = jdbc.queryForObject("select id from tasa_cambio", Long.class);

    publicar("/api/v1/tasas/vista-previa", Map.of("par", "USD_VES", "valor", "41", "tasaId", id))
        .andExpect(jsonPath("$.anterior").value("40.000000"))
        .andExpect(jsonPath("$.porcentaje").value("2.50"))
        .andExpect(jsonPath("$.superaLimite").value(false));
  }

  @Test
  void consultaElHistorialPorParYFechas() throws Exception {
    tasaPrevia("USD_VES", "2026-09-28", "36.1");
    tasaPrevia("USD_VES", "2026-09-29", "36.2");
    tasaPrevia("USD_VES", "2026-09-30", "36.3");
    tasaPrevia("USD_COP", "2026-09-30", "3910");

    obtener("/api/v1/tasas?par=USD_VES&desde=2026-09-29&hasta=2026-09-30")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(2))
        .andExpect(jsonPath("$.contenido[0].fecha").value("2026-09-30"))
        .andExpect(jsonPath("$.contenido[1].fecha").value("2026-09-29"));
    obtener("/api/v1/tasas").andExpect(jsonPath("$.totalElementos").value(4));
    obtener("/api/v1/tasas/999999").andExpect(status().isNotFound());
  }

  @Test
  void validaLosDatosDeLaTasa() throws Exception {
    publicar("/api/v1/tasas/ves", manual("0", "0", false))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("TASA_INVALIDA"));
    publicar("/api/v1/tasas/ves", Map.of("valor", "36.5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("confirmacion"));
  }
}
