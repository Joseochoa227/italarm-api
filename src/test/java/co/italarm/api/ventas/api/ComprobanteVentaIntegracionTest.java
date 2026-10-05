package co.italarm.api.ventas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;

class ComprobanteVentaIntegracionTest extends PruebaInventario {

  private long ventaId;

  @BeforeEach
  void venderUnaCamaraConSerial() throws Exception {
    long proveedor = crearProveedor("Distribuidora Andina");
    long cliente = crearCliente("Juan Pérez", "INSTALADOR");
    long camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    long cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("HK-1", "HK-2")));
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));

    Map<String, Object> lineaCamara = new HashMap<>();
    lineaCamara.put("productoId", camara);
    lineaCamara.put("seriales", List.of("HK-1"));
    Map<String, Object> lineaCable = new HashMap<>();
    lineaCable.put("productoId", cable);
    lineaCable.put("cantidad", "12.5");
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", cliente);
    venta.put("moneda", "USD");
    venta.put("lineas", List.of(lineaCamara, lineaCable));
    venta.put("descuentoTipo", "PORCENTAJE");
    venta.put("descuentoValor", "10");
    venta.put("observaciones", "Entrega en obra");
    venta.put("monedasComprobante", List.of("COP", "VES"));
    ventaId =
        leer(enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated()))
            .get("id")
            .asLong();
  }

  private String texto(byte[] pdf) throws Exception {
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
    PdfReader lector = new PdfReader(pdf);
    StringBuilder texto = new StringBuilder();
    for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
      texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina));
    }
    return texto.toString();
  }

  private static byte[] png() throws Exception {
    BufferedImage imagen = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen, "png", salida);
    return salida.toByteArray();
  }

  @Test
  void elComprobanteTraeEmpresaClienteItemsSerialesTotalesYPie() throws Exception {
    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/configuracion/logo")
                .file(new MockMultipartFile("archivo", "logo.png", "image/png", png()))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    byte[] pdf =
        sinCuerpo(get("/api/v1/ventas/{id}/comprobante", ventaId))
            .andExpect(status().isOk())
            .andExpect(
                header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("V-0001.pdf")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    String texto = texto(pdf);
    assertThat(texto)
        .contains("ITALARM")
        .contains("COMPROBANTE DE VENTA")
        .contains("V-0001")
        .contains("01/10/2026")
        .contains("Juan Pérez")
        .contains("Calle 10 # 5-20")
        .contains("Producto CAM-S")
        .contains("Serial HK-1 · garantía hasta 01/01/2027")
        .contains("12,5 m")
        .contains("Descuento (10 %)")
        .contains("Total de contado")
        .contains("US$ 364,50")
        .contains("Total en COP: $ 1.458.000")
        .contains("TRM $ 4.000 del 01/10/2026")
        .contains("Total en VES")
        .contains("Entrega en obra")
        .contains("Documento no válido como factura")
        .doesNotContain("ANULADA");
  }

  @Test
  void ventaAnulada_elComprobanteLlevaLaMarca() throws Exception {
    enviar(post("/api/v1/ventas/{id}/anular", ventaId), Map.of("motivo", "Error de digitación"))
        .andExpect(status().isOk());

    String texto =
        texto(
            sinCuerpo(get("/api/v1/ventas/{id}/comprobante", ventaId))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());

    assertThat(texto).contains("ANULADA").contains("Error de digitación");
  }

  @Test
  void enlacePublico_funcionaSinSesionYVenceA30Dias() throws Exception {
    JsonNode enlace =
        leer(
            sinCuerpo(post("/api/v1/ventas/{id}/enlace", ventaId))
                .andExpect(status().isOk())
                .andExpect(
                    jsonPath("$.url", startsWith("http://localhost:8080/api/v1/comprobantes/")))
                .andExpect(jsonPath("$.venceEn").value("2026-10-31T15:00:00Z"))
                .andExpect(jsonPath("$.mensaje", containsString("comprobante de venta V-0001")))
                .andExpect(
                    jsonPath(
                        "$.whatsappUrl", startsWith("https://wa.me/573001234567?text=Hola+Juan"))));
    String ruta = URI.create(enlace.get("url").asText()).getPath();

    String texto =
        texto(
            mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(
                    header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
    assertThat(texto).contains("V-0001");

    mvc.perform(get("/api/v1/comprobantes/token-que-no-existe")).andExpect(status().isNotFound());
    reloj.avanzar(Duration.ofDays(30));
    mvc.perform(get(ruta))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail", containsString("venció")));
  }
}
