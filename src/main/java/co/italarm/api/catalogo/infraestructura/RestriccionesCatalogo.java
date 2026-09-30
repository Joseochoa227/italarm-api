package co.italarm.api.catalogo.infraestructura;

import co.italarm.api.catalogo.dominio.CategoriaConProductosException;
import co.italarm.api.catalogo.dominio.CategoriaDuplicadaException;
import co.italarm.api.catalogo.dominio.ProductoCodigoDuplicadoException;
import co.italarm.api.catalogo.dominio.UnidadDuplicadaException;
import co.italarm.api.catalogo.dominio.UnidadEnUsoException;
import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.TipoError;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos del catálogo y su error de negocio (BP-09). */
@Component
public class RestriccionesCatalogo implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_categoria_nombre",
            TipoError.CONFLICTO,
            CategoriaDuplicadaException.CODIGO,
            "Ya existe una categoría con ese nombre."),
        new RestriccionConocida(
            "uq_unidad_medida_nombre",
            TipoError.CONFLICTO,
            UnidadDuplicadaException.CODIGO,
            "Ya existe una unidad de medida con ese nombre."),
        new RestriccionConocida(
            "uq_unidad_medida_abreviatura",
            TipoError.CONFLICTO,
            UnidadDuplicadaException.CODIGO,
            "Ya existe una unidad de medida con esa abreviatura."),
        new RestriccionConocida(
            "uq_producto_codigo",
            TipoError.CONFLICTO,
            ProductoCodigoDuplicadoException.CODIGO,
            "Ya existe un producto con ese código."),
        new RestriccionConocida(
            "fk_producto_categoria",
            TipoError.REGLA_NEGOCIO,
            CategoriaConProductosException.CODIGO,
            "La categoría tiene productos y no se puede eliminar."),
        new RestriccionConocida(
            "fk_producto_unidad_medida",
            TipoError.REGLA_NEGOCIO,
            UnidadEnUsoException.CODIGO,
            "La unidad de medida la usan productos y no se puede eliminar."));
  }
}
