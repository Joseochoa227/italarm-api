package co.italarm.api.tasas.infraestructura;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import co.italarm.api.tasas.aplicacion.ServicioTrm;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TareaTrmTest {

  private static PropiedadesItalarm propiedades(boolean alArrancar) {
    return new PropiedadesItalarm(
        new PropiedadesItalarm.Cors(List.of()),
        new PropiedadesItalarm.Usuarios(null),
        new PropiedadesItalarm.Almacenamiento(
            "disco",
            "x",
            "http://localhost",
            null,
            Duration.ofMinutes(1),
            new PropiedadesItalarm.S3(null, "auto", null, null, null)),
        new PropiedadesItalarm.Trm("http://x", true, alArrancar, Duration.ofSeconds(1)));
  }

  @Test
  void lasEjecucionesProgramadasConsultanLaTrmDeHoy() {
    ServicioTrm servicio = mock(ServicioTrm.class);
    TareaTrm tarea = new TareaTrm(servicio, propiedades(true));

    tarea.consultarEnLaManana();
    tarea.ultimoIntento();
    tarea.alArrancar();

    verify(servicio, times(3)).actualizarTrmDeHoy();
  }

  @Test
  void alArrancarSoloConsultaSiEstaActivado() {
    ServicioTrm servicio = mock(ServicioTrm.class);

    new TareaTrm(servicio, propiedades(false)).alArrancar();

    verify(servicio, never()).actualizarTrmDeHoy();
  }

  @Test
  void unErrorInesperadoNoDetieneLaTarea() {
    ServicioTrm servicio = mock(ServicioTrm.class);
    doThrow(new IllegalStateException("base de datos caída")).when(servicio).actualizarTrmDeHoy();

    new TareaTrm(servicio, propiedades(true)).consultarEnLaManana();

    verify(servicio).actualizarTrmDeHoy();
  }
}
