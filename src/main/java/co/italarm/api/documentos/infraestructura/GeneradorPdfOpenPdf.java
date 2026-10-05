package co.italarm.api.documentos.infraestructura;

import co.italarm.api.documentos.aplicacion.DocumentoPdf;
import co.italarm.api.documentos.aplicacion.GeneradorPdf;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** PDF de comprobantes y cotizaciones con OpenPDF (sección 9.1), en tamaño carta. */
@Component
public class GeneradorPdfOpenPdf implements GeneradorPdf {

  private static final Logger LOG = LoggerFactory.getLogger(GeneradorPdfOpenPdf.class);
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final Color AZUL = new Color(30, 58, 95);
  private static final Color GRIS = new Color(110, 110, 110);
  private static final Color FONDO = new Color(238, 242, 247);
  private static final BigDecimal OPACIDAD_MARCA = new BigDecimal("0.15");

  private final Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, AZUL);
  private final Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
  private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
  private final Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
  private final Font encabezadoTabla =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

  @Override
  public byte[] generar(DocumentoPdf documento) {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    Document pdf = new Document(PageSize.LETTER, 40, 40, 40, 50);
    PdfWriter escritor = PdfWriter.getInstance(pdf, salida);
    if (documento.marca() != null) {
      escritor.setPageEvent(new MarcaDeAgua(documento.marca()));
    }
    pdf.addTitle(documento.titulo() + " " + documento.consecutivo());
    pdf.addCreator(documento.empresa().nombre());
    pdf.open();
    pdf.add(encabezado(documento));
    pdf.add(cliente(documento.cliente()));
    pdf.add(items(documento.items()));
    pdf.add(totales(documento));
    if (documento.observaciones() != null) {
      Paragraph observaciones =
          new Paragraph("Observaciones: " + documento.observaciones(), normal);
      observaciones.setSpacingBefore(8);
      pdf.add(observaciones);
    }
    Paragraph pie = new Paragraph();
    pie.setSpacingBefore(18);
    for (String linea : documento.pie()) {
      pie.add(new Phrase(linea + "\n", pequena));
    }
    pdf.add(pie);
    pdf.close();
    return salida.toByteArray();
  }

  private PdfPTable encabezado(DocumentoPdf documento) {
    DocumentoPdf.Empresa empresa = documento.empresa();
    PdfPTable tabla = tabla(new int[] {1, 3, 2});
    tabla.setWidthPercentage(100);
    PdfPCell logo = sinBorde(new PdfPCell());
    Image imagen = logo(empresa.logo());
    if (imagen != null) {
      imagen.scaleToFit(70, 70);
      logo.addElement(imagen);
    }
    tabla.addCell(logo);

    PdfPCell datos = sinBorde(new PdfPCell());
    datos.addElement(new Paragraph(empresa.nombre(), titulo));
    agregar(datos, empresa.lema(), normal);
    List<String> contacto = new ArrayList<>();
    agregarSiHay(contacto, empresa.ciudad());
    agregarSiHay(contacto, empresa.telefono());
    agregarSiHay(contacto, empresa.correo());
    agregar(datos, contacto.isEmpty() ? null : String.join(" · ", contacto), pequena);
    agregar(datos, empresa.nit() == null ? null : "NIT " + empresa.nit(), pequena);
    tabla.addCell(datos);

    PdfPCell documentoCelda = sinBorde(new PdfPCell());
    documentoCelda.setHorizontalAlignment(Element.ALIGN_RIGHT);
    documentoCelda.addElement(derecha(documento.titulo().toUpperCase(), negrita));
    documentoCelda.addElement(derecha(documento.consecutivo(), titulo));
    documentoCelda.addElement(derecha("Fecha: " + FECHA.format(documento.fecha()), normal));
    if (documento.validoHasta() != null) {
      documentoCelda.addElement(
          derecha("Válida hasta: " + FECHA.format(documento.validoHasta()), normal));
    }
    tabla.addCell(documentoCelda);
    tabla.setSpacingAfter(14);
    return tabla;
  }

  private PdfPTable cliente(DocumentoPdf.Cliente cliente) {
    PdfPTable tabla = new PdfPTable(1);
    tabla.setWidthPercentage(100);
    PdfPCell celda = new PdfPCell();
    celda.setBackgroundColor(FONDO);
    celda.setBorder(Rectangle.NO_BORDER);
    celda.setPadding(8);
    celda.addElement(new Paragraph("Cliente: " + cliente.nombre(), negrita));
    agregar(celda, cliente.documento(), normal);
    agregar(
        celda, cliente.direccion() == null ? null : "Dirección: " + cliente.direccion(), normal);
    agregar(celda, cliente.telefono() == null ? null : "Teléfono: " + cliente.telefono(), normal);
    tabla.addCell(celda);
    tabla.setSpacingAfter(12);
    return tabla;
  }

  private PdfPTable items(List<DocumentoPdf.Item> items) {
    PdfPTable tabla = tabla(new int[] {6, 2, 2, 2});
    tabla.setWidthPercentage(100);
    tabla.setHeaderRows(1);
    for (String columna : List.of("Descripción", "Cantidad", "Valor unitario", "Total")) {
      PdfPCell celda = new PdfPCell(new Phrase(columna, encabezadoTabla));
      celda.setBackgroundColor(AZUL);
      celda.setPadding(5);
      celda.setBorder(Rectangle.NO_BORDER);
      tabla.addCell(celda);
    }
    for (DocumentoPdf.Item item : items) {
      PdfPCell descripcion = fila(new PdfPCell());
      descripcion.addElement(new Paragraph(item.descripcion(), normal));
      for (String detalle : item.detalles()) {
        descripcion.addElement(new Paragraph(detalle, pequena));
      }
      tabla.addCell(descripcion);
      tabla.addCell(numero(item.cantidad()));
      tabla.addCell(numero(item.valorUnitario()));
      tabla.addCell(numero(item.total()));
    }
    return tabla;
  }

  private PdfPTable totales(DocumentoPdf documento) {
    PdfPTable tabla = tabla(new int[] {8, 4});
    tabla.setWidthPercentage(100);
    tabla.setSpacingBefore(8);
    List<DocumentoPdf.Total> totales = documento.totales();
    for (int i = 0; i < totales.size(); i++) {
      Font fuente = i == totales.size() - 1 ? negrita : normal;
      PdfPCell etiqueta = sinBorde(new PdfPCell(new Phrase(totales.get(i).etiqueta(), fuente)));
      etiqueta.setHorizontalAlignment(Element.ALIGN_RIGHT);
      tabla.addCell(etiqueta);
      PdfPCell valor = sinBorde(new PdfPCell(new Phrase(totales.get(i).valor(), fuente)));
      valor.setHorizontalAlignment(Element.ALIGN_RIGHT);
      tabla.addCell(valor);
    }
    for (String equivalente : documento.equivalentes()) {
      PdfPCell celda = sinBorde(new PdfPCell(new Phrase(equivalente, pequena)));
      celda.setColspan(2);
      celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
      tabla.addCell(celda);
    }
    return tabla;
  }

  private static PdfPTable tabla(int[] anchos) {
    PdfPTable tabla = new PdfPTable(anchos.length);
    tabla.setWidths(anchos);
    return tabla;
  }

  private PdfPCell numero(String texto) {
    PdfPCell celda = fila(new PdfPCell(new Phrase(texto, normal)));
    celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
    return celda;
  }

  private static PdfPCell fila(PdfPCell celda) {
    celda.setBorder(Rectangle.BOTTOM);
    celda.setBorderColor(FONDO);
    celda.setPadding(5);
    return celda;
  }

  private static PdfPCell sinBorde(PdfPCell celda) {
    celda.setBorder(Rectangle.NO_BORDER);
    return celda;
  }

  private static Paragraph derecha(String texto, Font fuente) {
    Paragraph parrafo = new Paragraph(texto, fuente);
    parrafo.setAlignment(Element.ALIGN_RIGHT);
    return parrafo;
  }

  private static void agregar(PdfPCell celda, String texto, Font fuente) {
    if (texto != null && !texto.isBlank()) {
      celda.addElement(new Paragraph(texto, fuente));
    }
  }

  private static void agregarSiHay(List<String> lista, String texto) {
    if (texto != null && !texto.isBlank()) {
      lista.add(texto);
    }
  }

  /** El logo puede no ser legible por OpenPDF (por ejemplo WebP): el PDF sale sin él. */
  private static Image logo(byte[] contenido) {
    if (contenido == null) {
      return null;
    }
    try {
      return Image.getInstance(contenido);
    } catch (Exception e) {
      LOG.warn("No se pudo poner el logo en el PDF: {}", e.getMessage());
      return null;
    }
  }

  /** Texto grande y transparente cruzado sobre cada página (por ejemplo "ANULADA"). */
  private static final class MarcaDeAgua extends PdfPageEventHelper {

    private final String texto;

    MarcaDeAgua(String texto) {
      this.texto = texto;
    }

    @Override
    public void onEndPage(PdfWriter escritor, Document documento) {
      PdfContentByte contenido = escritor.getDirectContentUnder();
      contenido.saveState();
      PdfGState transparencia = new PdfGState();
      transparencia.setFillOpacity(OPACIDAD_MARCA.floatValue());
      contenido.setGState(transparencia);
      ColumnText.showTextAligned(
          contenido,
          Element.ALIGN_CENTER,
          new Phrase(
              texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 90, new Color(200, 0, 0))),
          PageSize.LETTER.getWidth() / 2,
          PageSize.LETTER.getHeight() / 2,
          45);
      contenido.restoreState();
    }
  }
}
