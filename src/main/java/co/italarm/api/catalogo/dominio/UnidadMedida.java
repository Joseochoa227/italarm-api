package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Unidad de medida de los productos: Unidad, Metro, Par… (RF-148, P-09). */
@Entity
@Table(name = "unidad_medida")
public class UnidadMedida extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre", nullable = false, length = 40)
  private String nombre;

  @Column(name = "abreviatura", nullable = false, length = 10)
  private String abreviatura;

  @Column(name = "admite_decimales", nullable = false)
  private boolean admiteDecimales;

  protected UnidadMedida() {}

  public static UnidadMedida crear(String nombre, String abreviatura, boolean admiteDecimales) {
    UnidadMedida unidad = new UnidadMedida();
    unidad.actualizar(nombre, abreviatura, admiteDecimales);
    return unidad;
  }

  public void actualizar(String nuevoNombre, String nuevaAbreviatura, boolean admite) {
    this.nombre = Textos.limpiar(nuevoNombre);
    this.abreviatura = Textos.limpiar(nuevaAbreviatura);
    this.admiteDecimales = admite;
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getAbreviatura() {
    return abreviatura;
  }

  public boolean isAdmiteDecimales() {
    return admiteDecimales;
  }
}
