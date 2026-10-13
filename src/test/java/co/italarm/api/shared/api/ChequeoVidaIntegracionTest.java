package co.italarm.api.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpointGroups;

/**
 * Chequeo que usan UptimeRobot y Render cada pocos minutos (Fase 7, P-04): responde sin sesión y
 * sin consultar la base, para que Neon pueda apagarse cuando nadie usa la app.
 */
class ChequeoVidaIntegracionTest extends PruebaIntegracion {

  @Autowired private HealthEndpointGroups grupos;

  @Test
  void livenessResponde_sinSesionYSinConsultarLaBase() throws Exception {
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));

    assertThat(grupos.get("liveness").isMember("db")).isFalse();
    assertThat(grupos.get("liveness").isMember("livenessState")).isTrue();
  }
}
