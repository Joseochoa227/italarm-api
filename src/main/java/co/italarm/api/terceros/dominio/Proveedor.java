package co.italarm.api.terceros.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Proveedor (sección 3.6). Su moneda habitual se propone al registrar una compra (RF-40). */
@Entity
@Table(name = "proveedor")
public class Proveedor extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre", nullable = false, length = 150)
  private String nombre;

  @Column(name = "nit", length = 30)
  private String nit;

  @Column(name = "telefono", length = 20)
  private String telefono;

  @Column(name = "correo", length = 254)
  private String correo;

  @Column(name = "ciudad", length = 80)
  private String ciudad;

  @Enumerated(EnumType.STRING)
  @Column(name = "moneda_habitual", nullable = false, length = 3)
  private Moneda monedaHabitual;

  protected Proveedor() {}

  public static Proveedor crear(DatosProveedor datos) {
    Proveedor proveedor = new Proveedor();
    proveedor.actualizar(datos);
    return proveedor;
  }

  public void actualizar(DatosProveedor datos) {
    this.nombre = Textos.limpiar(datos.nombre());
    this.nit = Documentos.normalizar(datos.nit());
    this.telefono = Telefono.normalizarOpcional(datos.telefono());
    this.correo = Textos.limpiar(datos.correo());
    this.ciudad = Textos.limpiar(datos.ciudad());
    this.monedaHabitual = datos.monedaHabitual();
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getNit() {
    return nit;
  }

  public String getTelefono() {
    return telefono;
  }

  public String getCorreo() {
    return correo;
  }

  public String getCiudad() {
    return ciudad;
  }

  public Moneda getMonedaHabitual() {
    return monedaHabitual;
  }
}
