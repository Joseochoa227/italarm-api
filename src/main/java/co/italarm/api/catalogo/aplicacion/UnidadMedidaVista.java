package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.catalogo.dominio.UnidadMedida;

/** Unidad de medida. {@code admiteDecimales}: si acepta cantidades con hasta 2 decimales. */
public record UnidadMedidaVista(
    Long id, String nombre, String abreviatura, boolean admiteDecimales, long version) {

  static UnidadMedidaVista de(UnidadMedida unidad) {
    return new UnidadMedidaVista(
        unidad.getId(),
        unidad.getNombre(),
        unidad.getAbreviatura(),
        unidad.isAdmiteDecimales(),
        unidad.getVersion());
  }
}
