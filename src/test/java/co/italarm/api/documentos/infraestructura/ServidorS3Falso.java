package co.italarm.api.documentos.infraestructura;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servidor S3 mínimo en memoria para probar {@link AlmacenamientoS3} con el SDK real de AWS (rutas
 * de estilo {@code /bucket/clave}). Reemplaza a MinIO, que no está disponible como contenedor.
 */
final class ServidorS3Falso implements AutoCloseable {

  record Objeto(byte[] contenido, String tipoContenido) {}

  private final HttpServer servidor;
  final Map<String, Objeto> objetos = new ConcurrentHashMap<>();

  ServidorS3Falso() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/", this::atender);
    servidor.start();
  }

  String url() {
    return "http://127.0.0.1:" + servidor.getAddress().getPort();
  }

  private void atender(HttpExchange intercambio) throws IOException {
    String ruta = intercambio.getRequestURI().getPath();
    try (intercambio) {
      switch (intercambio.getRequestMethod()) {
        case "PUT" -> {
          byte[] cuerpo = intercambio.getRequestBody().readAllBytes();
          String firmaCuerpo = intercambio.getRequestHeaders().getFirst("x-amz-content-sha256");
          if (firmaCuerpo != null && firmaCuerpo.startsWith("STREAMING-")) {
            cuerpo = decodificarAwsChunked(cuerpo);
          }
          objetos.put(
              ruta, new Objeto(cuerpo, intercambio.getRequestHeaders().getFirst("Content-Type")));
          intercambio.getResponseHeaders().add("ETag", "\"etag\"");
          intercambio.sendResponseHeaders(200, -1);
        }
        case "GET" -> {
          Objeto objeto = objetos.get(ruta);
          if (objeto == null) {
            responderNoExiste(intercambio);
          } else {
            intercambio.getResponseHeaders().add("Content-Type", objeto.tipoContenido());
            intercambio.sendResponseHeaders(200, objeto.contenido().length);
            try (OutputStream salida = intercambio.getResponseBody()) {
              salida.write(objeto.contenido());
            }
          }
        }
        case "DELETE" -> {
          objetos.remove(ruta);
          intercambio.sendResponseHeaders(204, -1);
        }
        default -> intercambio.sendResponseHeaders(405, -1);
      }
    }
  }

  /** Decodifica el formato aws-chunked: "tamaño-hex;chunk-signature=...\r\ndatos\r\n"... */
  private static byte[] decodificarAwsChunked(byte[] cuerpo) {
    java.io.ByteArrayOutputStream datos = new java.io.ByteArrayOutputStream();
    int posicion = 0;
    while (posicion < cuerpo.length) {
      int finLinea = indiceDe(cuerpo, posicion);
      String encabezado =
          new String(cuerpo, posicion, finLinea - posicion, StandardCharsets.US_ASCII);
      int tamano = Integer.parseInt(encabezado.split(";")[0].trim(), 16);
      if (tamano == 0) {
        break;
      }
      datos.write(cuerpo, finLinea + 2, tamano);
      posicion = finLinea + 2 + tamano + 2;
    }
    return datos.toByteArray();
  }

  private static int indiceDe(byte[] cuerpo, int desde) {
    for (int i = desde; i < cuerpo.length - 1; i++) {
      if (cuerpo[i] == '\r' && cuerpo[i + 1] == '\n') {
        return i;
      }
    }
    throw new IllegalArgumentException("Cuerpo aws-chunked mal formado");
  }

  private static void responderNoExiste(HttpExchange intercambio) throws IOException {
    byte[] error =
        ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>NoSuchKey</Code>"
                + "<Message>No existe</Message></Error>")
            .getBytes(StandardCharsets.UTF_8);
    intercambio.getResponseHeaders().add("Content-Type", "application/xml");
    intercambio.sendResponseHeaders(404, error.length);
    try (OutputStream salida = intercambio.getResponseBody()) {
      salida.write(error);
    }
  }

  @Override
  public void close() {
    servidor.stop(0);
  }
}
