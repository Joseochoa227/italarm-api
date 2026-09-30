package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Producto del catálogo (sección 3.3). El stock y el costo en USD los mueve el inventario a partir
 * de la Fase 2; al crearse queda con stock 0 y sin costo (RF-16).
 */
@Entity
@Table(name = "producto")
public class Producto extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "codigo", nullable = false, length = 30)
  private String codigo;

  @Column(name = "nombre", nullable = false, length = 150)
  private String nombre;

  @Column(name = "marca", length = 80)
  private String marca;

  @Column(name = "modelo", length = 80)
  private String modelo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "categoria_id", nullable = false)
  private Categoria categoria;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "unidad_medida_id", nullable = false)
  private UnidadMedida unidadMedida;

  @Column(name = "controla_serial", nullable = false)
  private boolean controlaSerial;

  @Column(name = "precio_instalador", nullable = false, precision = 19, scale = 4)
  private BigDecimal precioInstalador;

  @Column(name = "precio_cliente_final", nullable = false, precision = 19, scale = 4)
  private BigDecimal precioClienteFinal;

  @Enumerated(EnumType.STRING)
  @Column(name = "moneda_precio", nullable = false, length = 3)
  private Moneda monedaPrecio;

  @Column(name = "stock", nullable = false, precision = 14, scale = 3, updatable = false)
  private BigDecimal stock;

  @Column(name = "costo_actual_usd", precision = 19, scale = 4, updatable = false)
  private BigDecimal costoActualUsd;

  @Column(name = "stock_minimo", precision = 14, scale = 3)
  private BigDecimal stockMinimo;

  @Column(name = "descripcion", columnDefinition = "text")
  private String descripcion;

  @Column(name = "foto_clave", length = 300)
  private String fotoClave;

  @Column(name = "activo", nullable = false)
  private boolean activo;

  protected Producto() {}

  public static Producto crear(DatosProducto datos, Categoria categoria, UnidadMedida unidad) {
    Producto producto = new Producto();
    producto.stock = BigDecimal.ZERO;
    producto.activo = true;
    producto.aplicar(datos, categoria, unidad);
    return producto;
  }

  /**
   * Actualiza los datos. Si el producto ya tiene movimientos, no se puede cambiar si controla
   * serial ni su unidad de medida (P-17).
   */
  public void actualizar(
      DatosProducto datos,
      Categoria nuevaCategoria,
      UnidadMedida nuevaUnidad,
      boolean tieneMovimientos) {
    if (tieneMovimientos && datos.controlaSerial() != controlaSerial) {
      throw new ProductoCambioNoPermitidoException(
          "No se puede cambiar si el producto controla serial: ya tiene movimientos.");
    }
    boolean cambiaUnidad =
        unidadMedida != nuevaUnidad
            && (unidadMedida.getId() == null || !unidadMedida.getId().equals(nuevaUnidad.getId()));
    if (tieneMovimientos && cambiaUnidad) {
      throw new ProductoCambioNoPermitidoException(
          "No se puede cambiar la unidad de medida del producto: ya tiene movimientos.");
    }
    aplicar(datos, nuevaCategoria, nuevaUnidad);
  }

  private void aplicar(DatosProducto datos, Categoria nuevaCategoria, UnidadMedida nuevaUnidad) {
    this.codigo = normalizarCodigo(datos.codigo());
    this.nombre = Textos.limpiar(datos.nombre());
    this.marca = Textos.limpiar(datos.marca());
    this.modelo = Textos.limpiar(datos.modelo());
    this.categoria = nuevaCategoria;
    this.unidadMedida = nuevaUnidad;
    this.controlaSerial = datos.controlaSerial();
    this.precioInstalador = Redondeo.paraAlmacenar(datos.precioInstalador());
    this.precioClienteFinal = Redondeo.paraAlmacenar(datos.precioClienteFinal());
    this.monedaPrecio = datos.monedaPrecio() == null ? Moneda.USD : datos.monedaPrecio();
    this.stockMinimo =
        ReglaCantidad.validar(
            datos.stockMinimo(),
            nuevaUnidad.isAdmiteDecimales(),
            nuevaUnidad.getAbreviatura(),
            "Stock mínimo");
    this.descripcion =
        datos.descripcion() == null || datos.descripcion().isBlank()
            ? null
            : datos.descripcion().trim();
  }

  /** Código en mayúsculas y sin espacios a los lados (único sin distinguir mayúsculas). */
  public static String normalizarCodigo(String codigo) {
    return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
  }

  public boolean estaBajoMinimo() {
    return stockMinimo != null && stock.compareTo(stockMinimo) < 0;
  }

  public void desactivar() {
    this.activo = false;
  }

  public void activar() {
    this.activo = true;
  }

  /** Asigna la nueva foto y devuelve la clave de la anterior (para eliminarla), o null. */
  public String cambiarFoto(String nuevaClave) {
    String anterior = fotoClave;
    this.fotoClave = nuevaClave;
    return anterior;
  }

  /** Quita la foto y devuelve su clave (para eliminarla), o null. */
  public String quitarFoto() {
    return cambiarFoto(null);
  }

  public Dinero precioInstalador() {
    return new Dinero(precioInstalador, monedaPrecio);
  }

  public Dinero precioClienteFinal() {
    return new Dinero(precioClienteFinal, monedaPrecio);
  }

  public Long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getNombre() {
    return nombre;
  }

  public String getMarca() {
    return marca;
  }

  public String getModelo() {
    return modelo;
  }

  public Categoria getCategoria() {
    return categoria;
  }

  public UnidadMedida getUnidadMedida() {
    return unidadMedida;
  }

  public boolean isControlaSerial() {
    return controlaSerial;
  }

  public Moneda getMonedaPrecio() {
    return monedaPrecio;
  }

  public BigDecimal getStock() {
    return stock;
  }

  public BigDecimal getCostoActualUsd() {
    return costoActualUsd;
  }

  public BigDecimal getStockMinimo() {
    return stockMinimo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getFotoClave() {
    return fotoClave;
  }

  public boolean isActivo() {
    return activo;
  }
}
