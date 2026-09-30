package co.italarm.api.terceros.dominio;

import java.util.regex.Pattern;

/**
 * Teléfono / WhatsApp con indicativo internacional (P-10), por ejemplo {@code +573001234567}. Si se
 * escribe sin indicativo se asume Colombia (+57). Se guarda solo con dígitos y el signo +, que es
 * el formato que necesita WhatsApp.
 */
public final class Telefono {

  private static final Pattern SEPARADORES = Pattern.compile("[\\s().-]");
  private static final Pattern FORMATO = Pattern.compile("\\+[1-9][0-9]{7,14}");
  private static final String INDICATIVO_COLOMBIA = "+57";

  private Telefono() {}

  public static String normalizar(String escrito) {
    String limpio = escrito == null ? "" : SEPARADORES.matcher(escrito).replaceAll("");
    String conIndicativo;
    if (limpio.startsWith("+")) {
      conIndicativo = limpio;
    } else if (limpio.startsWith("00")) {
      conIndicativo = "+" + limpio.substring(2);
    } else {
      conIndicativo = INDICATIVO_COLOMBIA + limpio;
    }
    if (!FORMATO.matcher(conIndicativo).matches()) {
      throw new TelefonoInvalidoException();
    }
    return conIndicativo;
  }

  /** Igual que {@link #normalizar}, pero un teléfono vacío queda como {@code null}. */
  public static String normalizarOpcional(String escrito) {
    return escrito == null || escrito.isBlank() ? null : normalizar(escrito);
  }
}
