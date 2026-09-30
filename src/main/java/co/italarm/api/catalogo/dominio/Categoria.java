package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Categoría de productos (RF-15). */
@Entity
@Table(name = "categoria")
public class Categoria extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre", nullable = false, length = 80)
  private String nombre;

  protected Categoria() {}

  public static Categoria crear(String nombre) {
    Categoria categoria = new Categoria();
    categoria.renombrar(nombre);
    return categoria;
  }

  public void renombrar(String nuevoNombre) {
    this.nombre = Textos.limpiar(nuevoNombre);
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }
}
