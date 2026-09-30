package co.italarm.api.shared.dominio;

/** Códigos de error generales, comunes a todos los módulos (RT-05). */
public final class CodigoError {

  public static final String VALIDACION = "VALIDACION";
  public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
  public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";
  public static final String RECURSO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";
  public static final String METODO_NO_PERMITIDO = "METODO_NO_PERMITIDO";
  public static final String FORMATO_NO_SOPORTADO = "FORMATO_NO_SOPORTADO";
  public static final String MODIFICADO_POR_OTRO_USUARIO = "MODIFICADO_POR_OTRO_USUARIO";
  public static final String ERROR_INTERNO = "ERROR_INTERNO";
  public static final String ARCHIVO_DEMASIADO_GRANDE = "ARCHIVO_DEMASIADO_GRANDE";
  public static final String DATOS_EN_CONFLICTO = "DATOS_EN_CONFLICTO";

  private CodigoError() {}
}
