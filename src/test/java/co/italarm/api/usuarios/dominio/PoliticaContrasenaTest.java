package co.italarm.api.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PoliticaContrasenaTest {

  @ParameterizedTest
  @ValueSource(strings = {"Italarm#2026", "aB3$efgh", "Cámara.Domo2", "Ñandú-2026"})
  void aceptaContrasenasQueCumplenTodo(String contrasena) {
    assertThat(PoliticaContrasena.incumplimientos(contrasena)).isEmpty();
    assertThat(PoliticaContrasena.cumple(contrasena)).isTrue();
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "italarm#2026 | una mayúscula",
        "ITALARM#2026 | una minúscula",
        "Italarm#ab   | un número",
        "Italarm2026  | un signo",
        "aB3$efg      | al menos 8 caracteres"
      })
  void indicaQueRequisitoFalta(String contrasena, String requisito) {
    assertThat(PoliticaContrasena.incumplimientos(contrasena)).containsExactly(requisito);
    assertThat(PoliticaContrasena.cumple(contrasena)).isFalse();
  }

  @Test
  void rechazaContrasenasDemasiadoLargas() {
    String larga = "Aa1#" + "x".repeat(61);

    assertThat(PoliticaContrasena.incumplimientos(larga)).containsExactly("máximo 64 caracteres");
  }

  @Test
  void rechazaContrasenasQueExcedenElLimiteDeBcrypt() {
    // 60 caracteres, pero 76 bytes en UTF-8: BCrypt solo usa 72.
    String conTildes = "Aa1#" + "é".repeat(16) + "x".repeat(40);

    assertThat(conTildes).hasSize(60);
    assertThat(PoliticaContrasena.incumplimientos(conTildes))
        .containsExactly("máximo 64 caracteres");
  }

  @Test
  void rechazaLaContrasenaVacia() {
    assertThat(PoliticaContrasena.incumplimientos(""))
        .containsExactly(
            "al menos 8 caracteres", "una mayúscula", "una minúscula", "un número", "un signo");
    assertThat(PoliticaContrasena.incumplimientos(null)).hasSize(5);
  }

  @Test
  void losEspaciosNoCuentanComoSigno() {
    assertThat(PoliticaContrasena.incumplimientos("Italarm 2026")).containsExactly("un signo");
  }
}
