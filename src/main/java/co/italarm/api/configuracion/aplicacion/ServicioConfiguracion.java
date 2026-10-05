package co.italarm.api.configuracion.aplicacion;

import co.italarm.api.configuracion.dominio.Configuracion;
import co.italarm.api.configuracion.dominio.DatosConfiguracion;
import co.italarm.api.configuracion.infraestructura.ConfiguracionRepositorio;
import co.italarm.api.documentos.aplicacion.ServicioArchivos;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos de la empresa y valores por defecto (sección 3.17). */
@Service
public class ServicioConfiguracion {

  private final ConfiguracionRepositorio configuraciones;
  private final ServicioArchivos archivos;

  public ServicioConfiguracion(
      ConfiguracionRepositorio configuraciones, ServicioArchivos archivos) {
    this.configuraciones = configuraciones;
    this.archivos = archivos;
  }

  @Transactional(readOnly = true)
  public ConfiguracionVista obtener() {
    return vista(cargar());
  }

  @Transactional
  public ConfiguracionVista actualizar(DatosConfiguracion datos, long version) {
    Configuracion configuracion = cargar();
    configuracion.verificarVersion(version);
    configuracion.actualizar(datos);
    configuraciones.flush();
    return vista(configuracion);
  }

  @Transactional
  public ConfiguracionVista cambiarLogo(byte[] contenido) {
    Configuracion configuracion = cargar();
    String clave = archivos.guardarImagen("configuracion", "logo", contenido);
    archivos.eliminarAlConfirmar(configuracion.cambiarLogo(clave));
    configuraciones.flush();
    return vista(configuracion);
  }

  @Transactional
  public ConfiguracionVista quitarLogo() {
    Configuracion configuracion = cargar();
    archivos.eliminarAlConfirmar(configuracion.quitarLogo());
    configuraciones.flush();
    return vista(configuracion);
  }

  /** Porcentaje de variación que dispara la alerta al registrar una tasa manual (RF-35c). */
  @Transactional(readOnly = true)
  public BigDecimal limiteVariacionTasa() {
    return cargar().getLimiteVariacionTasa();
  }

  /** Meses de garantía de los equipos con serial (RF-23, RF-147). */
  @Transactional(readOnly = true)
  public int garantiaEquiposMeses() {
    return cargar().getGarantiaEquiposMeses();
  }

  private Configuracion cargar() {
    return configuraciones
        .findById(Configuracion.ID_UNICO)
        .orElseThrow(() -> new IllegalStateException("Falta la fila de configuración (V2)"));
  }

  private ConfiguracionVista vista(Configuracion c) {
    return new ConfiguracionVista(
        c.getEmpresaNombre(),
        c.getEmpresaLema(),
        c.getEmpresaNit(),
        c.getEmpresaCiudad(),
        c.getEmpresaTelefono(),
        c.getEmpresaCorreo(),
        archivos.urlDe(c.getEmpresaLogoClave()),
        c.getLimiteVariacionTasa().stripTrailingZeros(),
        c.getValidezCotizacionDias(),
        c.getGarantiaManoObraMeses(),
        c.getGarantiaEquiposMeses(),
        c.getCondicionesGarantia(),
        c.getPiePdf(),
        c.getVersion());
  }
}
