package co.italarm.api.catalogo.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoriaYUnidadTest {

  @Test
  void normalizaLosEspaciosDelNombre() {
    Categoria categoria = Categoria.crear("  Fuentes   de  poder ");

    assertThat(categoria.getNombre()).isEqualTo("Fuentes de poder");

    categoria.renombrar(" Fuentes ");
    assertThat(categoria.getNombre()).isEqualTo("Fuentes");
  }

  @Test
  void laUnidadGuardaNombreAbreviaturaYDecimales() {
    UnidadMedida unidad = UnidadMedida.crear(" Metro ", " m ", true);

    assertThat(unidad.getNombre()).isEqualTo("Metro");
    assertThat(unidad.getAbreviatura()).isEqualTo("m");
    assertThat(unidad.isAdmiteDecimales()).isTrue();

    unidad.actualizar("Rollo", "rollo", false);
    assertThat(unidad.getNombre()).isEqualTo("Rollo");
    assertThat(unidad.getAbreviatura()).isEqualTo("rollo");
    assertThat(unidad.isAdmiteDecimales()).isFalse();
  }
}
