package co.italarm.api.instalaciones.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Datos de la instalación que no cumplen las reglas (fecha futura, sin técnicos, vacía…). */
public class InstalacionInvalidaException extends NegocioException {

  public static final String FECHA_FUTURA = "INSTALACION_FECHA_FUTURA";
  public static final String SIN_TECNICOS = "INSTALACION_SIN_TECNICOS";
  public static final String VACIA = "INSTALACION_VACIA";
  public static final String GARANTIA_INVALIDA = "INSTALACION_GARANTIA_INVALIDA";
  public static final String PRODUCTO_REPETIDO = "INSTALACION_PRODUCTO_REPETIDO";

  public InstalacionInvalidaException(String codigo, String mensaje) {
    super(TipoError.VALIDACION, codigo, mensaje);
  }
}
