package co.italarm.api.terceros.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import co.italarm.api.shared.dominio.Moneda;
import org.junit.jupiter.api.Test;

class ProveedorTest {

  @Test
  void normalizaLosDatosYGuardaLaMonedaHabitual() {
    Proveedor proveedor =
        Proveedor.crear(
            new DatosProveedor(
                " Importadora  XYZ ", " 900.555.111-2 ", "", null, "Bogotá", Moneda.COP));

    assertThat(proveedor.getNombre()).isEqualTo("Importadora XYZ");
    assertThat(proveedor.getNit()).isEqualTo("900555111-2");
    assertThat(proveedor.getTelefono()).isNull();
    assertThat(proveedor.getMonedaHabitual()).isEqualTo(Moneda.COP);

    proveedor.actualizar(
        new DatosProveedor("Importadora XYZ", null, "3001234567", "a@b.co", null, Moneda.USD));

    assertThat(proveedor.getNit()).isNull();
    assertThat(proveedor.getTelefono()).isEqualTo("+573001234567");
    assertThat(proveedor.getCorreo()).isEqualTo("a@b.co");
    assertThat(proveedor.getMonedaHabitual()).isEqualTo(Moneda.USD);
  }
}
