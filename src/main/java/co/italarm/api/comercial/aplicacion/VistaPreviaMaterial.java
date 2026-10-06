package co.italarm.api.comercial.aplicacion;

import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.compras.aplicacion.ConsultaCompras;
import co.italarm.api.compras.aplicacion.TasasUltimaCompra;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Tasas;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Líneas de la vista previa de un documento con material (RF-64, RF-69, RF-99, RF-101): stock con
 * su aviso, costo con las tasas de hoy y de la última compra, y aviso si el precio queda por debajo
 * del costo.
 */
@Service
public class VistaPreviaMaterial {

  private final ConsultaCompras compras;

  public VistaPreviaMaterial(ConsultaCompras compras) {
    this.compras = compras;
  }

  /**
   * @param puedeGuardar false si alguna línea no tiene stock suficiente (RF-101)
   */
  public record Resultado(List<LineaVistaPrevia> lineas, boolean puedeGuardar) {}

  @Transactional(readOnly = true)
  public Resultado construir(List<MaterialPreparado> preparadas, Moneda moneda, Tasas conversion) {
    Map<Long, TasasUltimaCompra> ultimas =
        compras.tasasUltimaCompra(preparadas.stream().map(l -> l.producto().id()).toList());
    boolean puedeGuardar = true;
    List<LineaVistaPrevia> vistas = new ArrayList<>();
    for (MaterialPreparado linea : preparadas) {
      ProductoValorizado p = linea.producto();
      String avisoStock = null;
      if (linea.cantidad().compareTo(p.stock()) > 0) {
        avisoStock =
            "Stock insuficiente · quedan " + p.stock().toPlainString() + " " + p.abreviatura();
        puedeGuardar = false;
      }
      BigDecimal costoUsd = p.costoActualUsd() == null ? BigDecimal.ZERO : p.costoActualUsd();
      Dinero costoEnUsd = new Dinero(costoUsd, Moneda.USD);
      TasasUltimaCompra ultima = ultimas.get(p.id());
      BigDecimal costoEnMoneda = conversion.desdeUsd(costoUsd, moneda);
      vistas.add(
          new LineaVistaPrevia(
              p.id(),
              p.codigo(),
              p.nombre(),
              p.abreviatura(),
              p.controlaSerial(),
              PreparacionMaterial.cantidadVista(linea.cantidad()),
              p.stock(),
              avisoStock,
              new Dinero(linea.precioSugerido(), moneda),
              new Dinero(linea.precioUnitario(), moneda),
              conversion.equivalentes(
                  new Dinero(Redondeo.paraAlmacenar(linea.calculo().subtotal()), moneda)),
              conversion.equivalentes(costoEnUsd),
              ultima == null
                  ? null
                  : new Tasas(ultima.trm(), ultima.tasaVes()).equivalentes(costoEnUsd),
              ultima == null
                  ? null
                  : new LineaVistaPrevia.UltimaCompra(
                      ultima.consecutivo(), ultima.fecha(), ultima.trm(), ultima.tasaVes()),
              linea.precioUnitario().compareTo(costoEnMoneda) < 0
                  ? "El precio queda por debajo del costo."
                  : null));
    }
    return new Resultado(vistas, puedeGuardar);
  }
}
