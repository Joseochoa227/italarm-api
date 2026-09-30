package co.italarm.api.documentos.dominio;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Firma HMAC-SHA256 de enlaces de descarga con vencimiento, equivalente a los enlaces firmados de
 * S3. Se usa en el almacenamiento en disco (perfil local).
 */
public final class FirmaEnlace {

  private static final String ALGORITMO = "HmacSHA256";

  private final SecretKeySpec clave;

  public FirmaEnlace(byte[] claveSecreta) {
    this.clave = new SecretKeySpec(claveSecreta.clone(), ALGORITMO);
  }

  public EnlaceFirmado firmar(String claveArchivo, Instant ahora, Duration duracion) {
    long expira = ahora.plus(duracion).getEpochSecond();
    return new EnlaceFirmado(claveArchivo, expira, calcular(claveArchivo, expira));
  }

  public boolean esValido(String claveArchivo, long expira, String firma, Instant ahora) {
    if (firma == null || ahora.getEpochSecond() > expira) {
      return false;
    }
    return MessageDigest.isEqual(
        calcular(claveArchivo, expira).getBytes(StandardCharsets.US_ASCII),
        firma.getBytes(StandardCharsets.US_ASCII));
  }

  private String calcular(String claveArchivo, long expira) {
    try {
      Mac mac = Mac.getInstance(ALGORITMO);
      mac.init(clave);
      byte[] resultado =
          mac.doFinal((claveArchivo + "|" + expira).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(resultado);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("No se pudo firmar el enlace", e);
    }
  }
}
