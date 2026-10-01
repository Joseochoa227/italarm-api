package co.italarm.api.cargainicial.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.dominio.ErrorCarga;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReglasArchivoCargaTest {

  private static final byte[] ZIP = {'P', 'K', 3, 4, 0};

  @Test
  void unXlsxDeMenosDe5Mb_seAcepta() {
    assertThatCode(() -> ReglasArchivoCarga.validar(ZIP)).doesNotThrowAnyException();
  }

  @Test
  void vacioONulo_seRechaza() {
    assertThatThrownBy(() -> ReglasArchivoCarga.validar(new byte[0]))
        .isInstanceOf(ArchivoCargaInvalidoException.class);
    assertThatThrownBy(() -> ReglasArchivoCarga.validar(null))
        .isInstanceOf(ArchivoCargaInvalidoException.class);
  }

  @Test
  void masDe5Mb_seRechaza() {
    byte[] grande = new byte[ReglasArchivoCarga.TAMANO_MAXIMO + 1];
    grande[0] = 'P';
    grande[1] = 'K';
    grande[2] = 3;
    grande[3] = 4;
    assertThatThrownBy(() -> ReglasArchivoCarga.validar(grande))
        .isInstanceOf(ArchivoCargaDemasiadoGrandeException.class)
        .extracting("codigo")
        .isEqualTo("ARCHIVO_DEMASIADO_GRANDE");
  }

  @Test
  void otroFormato_seRechaza() {
    assertThatThrownBy(() -> ReglasArchivoCarga.validar(new byte[] {'P', 'K'}))
        .isInstanceOf(ArchivoCargaInvalidoException.class);
    assertThatThrownBy(() -> ReglasArchivoCarga.validar("a,b".getBytes()))
        .isInstanceOf(ArchivoCargaInvalidoException.class);
  }

  @Test
  void elErrorDeCargaLlevaLaListaYCuentaLosErrores() {
    CargaInicialConErroresException uno =
        new CargaInicialConErroresException(List.of(new ErrorCarga("Productos", 2, "x")));
    CargaInicialConErroresException dos =
        new CargaInicialConErroresException(
            List.of(new ErrorCarga("Productos", 2, "x"), new ErrorCarga("Clientes", 3, "y")));

    assertThat(uno.getMessage()).startsWith("El archivo tiene 1 error;");
    assertThat(dos.getMessage()).startsWith("El archivo tiene 2 errores;");
    assertThat(dos.detalles()).hasSize(2);
  }
}
