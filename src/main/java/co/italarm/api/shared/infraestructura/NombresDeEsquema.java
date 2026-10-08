package co.italarm.api.shared.infraestructura;

import io.swagger.v3.core.jackson.TypeNameResolver;
import java.util.Set;

/**
 * Nombres de esquema únicos en el contrato (D-05 del frontend): un registro anidado toma el nombre
 * de su contenedor, por ejemplo {@code CompraVista.Linea} → {@code CompraVistaLinea}. Sin esto,
 * springdoc publica una sola versión de los registros que se llaman igual en distintos módulos.
 */
final class NombresDeEsquema extends TypeNameResolver {

  @Override
  protected String nameForClass(Class<?> clase, Set<Options> opciones) {
    String nombre = super.nameForClass(clase, opciones);
    Class<?> contenedora = clase.getEnclosingClass();
    return contenedora == null ? nombre : nameForClass(contenedora, opciones) + nombre;
  }
}
