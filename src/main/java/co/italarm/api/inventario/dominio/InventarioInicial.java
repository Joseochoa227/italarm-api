package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.EntidadAuditable;
import co.italarm.api.shared.dominio.TipoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Documento de inventario inicial cargado desde Excel (RF-151). */
@Entity
@Table(name = "inventario_inicial")
public class InventarioInicial extends EntidadAuditable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "numero", nullable = false, updatable = false)
  private Long numero;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "archivo_nombre", updatable = false, length = 200)
  private String archivoNombre;

  @Column(name = "productos_creados", nullable = false, updatable = false)
  private int productosCreados;

  @Column(name = "clientes_creados", nullable = false, updatable = false)
  private int clientesCreados;

  @Column(name = "proveedores_creados", nullable = false, updatable = false)
  private int proveedoresCreados;

  @OneToMany(mappedBy = "documento", cascade = CascadeType.PERSIST)
  private List<LineaInventarioInicial> lineas = new ArrayList<>();

  protected InventarioInicial() {}

  public static InventarioInicial crear(
      long numero,
      LocalDate fecha,
      String archivoNombre,
      int productosCreados,
      int clientesCreados,
      int proveedoresCreados) {
    InventarioInicial documento = new InventarioInicial();
    documento.numero = numero;
    documento.fecha = fecha;
    documento.archivoNombre =
        archivoNombre == null || archivoNombre.length() <= 200
            ? archivoNombre
            : archivoNombre.substring(0, 200);
    documento.productosCreados = productosCreados;
    documento.clientesCreados = clientesCreados;
    documento.proveedoresCreados = proveedoresCreados;
    return documento;
  }

  public void agregarLinea(LineaInventarioInicial linea) {
    linea.asignarDocumento(this);
    lineas.add(linea);
  }

  public DocumentoRef documento() {
    return new DocumentoRef(
        TipoDocumento.INVENTARIO_INICIAL, id, TipoDocumento.INVENTARIO_INICIAL.consecutivo(numero));
  }

  public Long getId() {
    return id;
  }

  public Long getNumero() {
    return numero;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public String getArchivoNombre() {
    return archivoNombre;
  }

  public int getProductosCreados() {
    return productosCreados;
  }

  public int getClientesCreados() {
    return clientesCreados;
  }

  public int getProveedoresCreados() {
    return proveedoresCreados;
  }

  public List<LineaInventarioInicial> getLineas() {
    return lineas;
  }
}
