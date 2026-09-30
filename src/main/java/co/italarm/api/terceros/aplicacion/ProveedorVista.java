package co.italarm.api.terceros.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.terceros.dominio.Proveedor;

/** Proveedor como lo ve el frontend. */
public record ProveedorVista(
    Long id,
    String nombre,
    String nit,
    String telefono,
    String correo,
    String ciudad,
    Moneda monedaHabitual,
    long version) {

  static ProveedorVista de(Proveedor proveedor) {
    return new ProveedorVista(
        proveedor.getId(),
        proveedor.getNombre(),
        proveedor.getNit(),
        proveedor.getTelefono(),
        proveedor.getCorreo(),
        proveedor.getCiudad(),
        proveedor.getMonedaHabitual(),
        proveedor.getVersion());
  }
}
