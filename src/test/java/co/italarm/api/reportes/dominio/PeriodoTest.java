package co.italarm.api.reportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeriodoTest {

  private static final LocalDate MIERCOLES = LocalDate.of(2026, 10, 7);

  @Test
  void semana_vaDeLunesADomingo() {
    Periodo semana = Periodo.de(TipoPeriodo.SEMANA, MIERCOLES);
    assertThat(semana.desde()).isEqualTo(LocalDate.of(2026, 10, 5));
    assertThat(semana.hasta()).isEqualTo(LocalDate.of(2026, 10, 11));
    assertThat(semana.anterior().desde()).isEqualTo(LocalDate.of(2026, 9, 28));
    assertThat(semana.siguiente().hasta()).isEqualTo(LocalDate.of(2026, 10, 18));
    assertThat(semana.tramos()).hasSize(1);
    assertThat(Periodo.de(TipoPeriodo.SEMANA, LocalDate.of(2026, 10, 11)).desde())
        .isEqualTo(LocalDate.of(2026, 10, 5));
  }

  @Test
  void mes_conSemanasPartidasAlInicioYAlFinal() {
    Periodo octubre = Periodo.de(TipoPeriodo.MES, MIERCOLES);
    assertThat(octubre.desde()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(octubre.hasta()).isEqualTo(LocalDate.of(2026, 10, 31));
    assertThat(octubre.agrupacion()).isEqualTo(Periodo.Agrupacion.SEMANA);

    List<Periodo.Tramo> tramos = octubre.tramos();
    assertThat(tramos).hasSize(5);
    assertThat(tramos.get(0))
        .isEqualTo(
            new Periodo.Tramo(
                "01/10 al 04/10", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4)));
    assertThat(tramos.get(4).desde()).isEqualTo(LocalDate.of(2026, 10, 26));
    assertThat(tramos.get(4).hasta()).isEqualTo(LocalDate.of(2026, 10, 31));
  }

  @Test
  void anio_vaPorMesYCambiaDeAnio() {
    Periodo anio = Periodo.de(TipoPeriodo.ANIO, MIERCOLES);
    assertThat(anio.desde()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(anio.hasta()).isEqualTo(LocalDate.of(2026, 12, 31));
    assertThat(anio.agrupacion()).isEqualTo(Periodo.Agrupacion.MES);
    assertThat(anio.tramos()).hasSize(12);
    assertThat(anio.tramos().get(0).etiqueta()).isEqualTo("Enero 2026");
    assertThat(anio.siguiente().desde()).isEqualTo(LocalDate.of(2027, 1, 1));
    assertThat(Periodo.de(TipoPeriodo.MES, LocalDate.of(2026, 1, 15)).anterior().desde())
        .isEqualTo(LocalDate.of(2025, 12, 1));
  }

  @Test
  void rangoLibre_porSemanaHasta3MesesYLuegoPorMes() {
    Periodo corto = Periodo.rango(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31));
    assertThat(corto.agrupacion()).isEqualTo(Periodo.Agrupacion.SEMANA);
    assertThat(corto.anterior()).isNull();
    assertThat(corto.siguiente()).isNull();
    Periodo largo = Periodo.rango(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 10, 31));
    assertThat(largo.agrupacion()).isEqualTo(Periodo.Agrupacion.MES);
    assertThat(largo.tramos().get(0).desde()).isEqualTo(LocalDate.of(2026, 7, 15));
    assertThat(largo.contiene(LocalDate.of(2026, 7, 14))).isFalse();
    assertThat(largo.contiene(LocalDate.of(2026, 10, 31))).isTrue();
  }

  @Test
  void rangoInvalido() {
    assertThatThrownBy(() -> Periodo.rango(MIERCOLES, MIERCOLES.minusDays(1)))
        .isInstanceOf(PeriodoInvalidoException.class);
    assertThatThrownBy(() -> Periodo.rango(null, MIERCOLES))
        .isInstanceOf(PeriodoInvalidoException.class);
    assertThatThrownBy(() -> Periodo.rango(MIERCOLES, MIERCOLES.plusYears(11)))
        .isInstanceOf(PeriodoInvalidoException.class);
    assertThatThrownBy(() -> Periodo.de(TipoPeriodo.RANGO, MIERCOLES))
        .isInstanceOf(PeriodoInvalidoException.class);
  }

  @Test
  void porcentajes() {
    assertThat(ReglasReportes.porcentaje(new BigDecimal("80"), new BigDecimal("140")))
        .isEqualByComparingTo("57.14");
    assertThat(ReglasReportes.porcentaje(BigDecimal.ONE, BigDecimal.ZERO)).isNull();
    assertThat(ReglasReportes.porcentajeAprobadas(3, 1)).isEqualByComparingTo("75");
    assertThat(ReglasReportes.porcentajeAprobadas(0, 0)).isNull();
    assertThat(ReglasReportes.nivelStock(new BigDecimal("3"), new BigDecimal("10")))
        .isEqualByComparingTo("30");
    assertThat(ReglasReportes.nivelStock(new BigDecimal("-1"), new BigDecimal("10")))
        .isEqualByComparingTo("0");
    assertThat(ReglasReportes.nivelStock(new BigDecimal("12"), new BigDecimal("10")))
        .isEqualByComparingTo("100");
    assertThat(ReglasReportes.nivelStock(BigDecimal.ONE, null)).isEqualByComparingTo("100");
  }
}
