package co.italarm.api.documentos.aplicacion;

/**
 * Nombre de la empresa para el encabezado de los Excel. Lo implementa el módulo de configuración,
 * para que documentos no dependa de él.
 */
public interface NombreEmpresa {

  String nombre();
}
