package co.italarm.api.catalogo.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ProductoApiIntegracionTest extends PruebaIntegracion {

  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};

  private String token;

  @BeforeEach
  void iniciarSesion() throws Exception {
    token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
  }

  private ResultActions enviar(MockHttpServletRequestBuilder peticion, Object cuerpo)
      throws Exception {
    return mvc.perform(
        peticion
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(cuerpo)));
  }

  private ResultActions sinCuerpo(MockHttpServletRequestBuilder peticion) throws Exception {
    return mvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  @Test
  void creaUnProductoConStockCeroYSinCosto() throws Exception {
    Map<String, Object> datos = ProductosDePrueba.datos(" cam-d2 ", 1, 1);
    datos.put("controlaSerial", true);
    datos.put("stockMinimo", "5");
    datos.put("modelo", "DS-2CE56D0T");

    enviar(post("/api/v1/productos"), datos)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigo").value("CAM-D2"))
        .andExpect(jsonPath("$.categoria.nombre").value("Cámaras"))
        .andExpect(jsonPath("$.unidadMedida.abreviatura").value("und"))
        .andExpect(jsonPath("$.controlaSerial").value(true))
        .andExpect(jsonPath("$.precioInstalador.monto").value("20.0000"))
        .andExpect(jsonPath("$.precioInstalador.moneda").value("USD"))
        .andExpect(jsonPath("$.precioClienteFinal.monto").value("25.5000"))
        .andExpect(jsonPath("$.stock").value("0"))
        .andExpect(jsonPath("$.stockMinimo").value("5"))
        .andExpect(jsonPath("$.bajoMinimo").value(true))
        .andExpect(jsonPath("$.costoActual").value(nullValue()))
        .andExpect(jsonPath("$.fotoUrl").value(nullValue()))
        .andExpect(jsonPath("$.activo").value(true));
  }

  @Test
  void elPrecioPuedeEstarEnOtraMoneda() throws Exception {
    Map<String, Object> datos = ProductosDePrueba.datos("CABLE-UTP", 4, 2);
    datos.put("monedaPrecio", "COP");
    datos.put("precioInstalador", "1800");
    datos.put("stockMinimo", "100.5");

    enviar(post("/api/v1/productos"), datos)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.precioInstalador.moneda").value("COP"))
        .andExpect(jsonPath("$.stockMinimo").value("100.5"));
  }

  @Test
  void validaLosDatosConMensajesEnEspanol() throws Exception {
    Map<String, Object> datos = ProductosDePrueba.datos("", 1, 1);
    datos.put("precioInstalador", "-1");
    datos.put("precioClienteFinal", "1.12345");

    enviar(post("/api/v1/productos"), datos)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACION"))
        .andExpect(
            jsonPath("$.errores[?(@.campo=='precioInstalador')].mensaje")
                .value("El precio no puede ser negativo."))
        .andExpect(
            jsonPath("$.errores[?(@.campo=='precioClienteFinal')].mensaje")
                .value("El precio admite máximo 4 decimales."));
  }

  @Test
  void elStockMinimoRespetaLaUnidad() throws Exception {
    Map<String, Object> datos = ProductosDePrueba.datos("CONECTOR", 7, 1);
    datos.put("stockMinimo", "2.5");

    enviar(post("/api/v1/productos"), datos)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"));
  }

  @Test
  void elCodigoEsUnicoSinDistinguirMayusculas() throws Exception {
    ProductosDePrueba.crear(mvc, token, "CAM-D2", 1, 1);

    enviar(post("/api/v1/productos"), ProductosDePrueba.datos("cam-d2", 1, 1))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("PRODUCTO_CODIGO_DUPLICADO"))
        .andExpect(jsonPath("$.detail").value("Ya existe un producto con el código CAM-D2."));
  }

  @Test
  void rechazaCategoriaOUnidadInexistente() throws Exception {
    enviar(post("/api/v1/productos"), ProductosDePrueba.datos("X", 999, 1))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CATEGORIA_NO_EXISTE"));
    enviar(post("/api/v1/productos"), ProductosDePrueba.datos("X", 1, 999))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("UNIDAD_NO_EXISTE"));
  }

  @Test
  void editaConControlDeVersion() throws Exception {
    long id = ProductosDePrueba.crear(mvc, token, "DVR-4", 2, 1);
    Map<String, Object> datos = ProductosDePrueba.datos("DVR-4", 2, 1);
    datos.put("nombre", "Grabador DVR 4 canales");
    datos.put("version", 0);

    enviar(put("/api/v1/productos/" + id), datos)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Grabador DVR 4 canales"))
        .andExpect(jsonPath("$.version").value(1));
    enviar(put("/api/v1/productos/" + id), datos)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("MODIFICADO_POR_OTRO_USUARIO"));

    Long editadoPor =
        jdbc.queryForObject("select updated_by from producto where id = ?", Long.class, id);
    Long idJose =
        jdbc.queryForObject("select id from usuario where correo = ?", Long.class, CORREO_JOSE);
    assertThat(editadoPor).isEqualTo(idJose);
  }

  @Test
  void desactivaActivaYElimina() throws Exception {
    long id = ProductosDePrueba.crear(mvc, token, "BALUN", 5, 3);

    sinCuerpo(post("/api/v1/productos/" + id + "/desactivar"))
        .andExpect(jsonPath("$.activo").value(false));
    sinCuerpo(get("/api/v1/productos?activo=true"))
        .andExpect(jsonPath("$.totalElementos").value(0));
    sinCuerpo(get("/api/v1/productos?activo=false"))
        .andExpect(jsonPath("$.totalElementos").value(1));
    sinCuerpo(post("/api/v1/productos/" + id + "/activar"))
        .andExpect(jsonPath("$.activo").value(true));

    sinCuerpo(delete("/api/v1/productos/" + id)).andExpect(status().isNoContent());
    sinCuerpo(get("/api/v1/productos/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
  }

  @Test
  void buscaFiltraYPagina() throws Exception {
    ProductosDePrueba.crear(mvc, token, "CAM-D2", 1, 1);
    ProductosDePrueba.crear(mvc, token, "CAM-B4", 1, 1);
    ProductosDePrueba.crear(mvc, token, "DISCO-1T", 3, 1);
    ProductosDePrueba.crear(mvc, token, "CABLE_UTP", 4, 2);

    sinCuerpo(get("/api/v1/productos?size=2&sort=codigo,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contenido", hasSize(2)))
        .andExpect(jsonPath("$.contenido[0].codigo").value("CABLE_UTP"))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.tamano").value(2))
        .andExpect(jsonPath("$.totalElementos").value(4))
        .andExpect(jsonPath("$.totalPaginas").value(2));
    sinCuerpo(get("/api/v1/productos?categoriaId=1"))
        .andExpect(jsonPath("$.totalElementos").value(2));
    sinCuerpo(get("/api/v1/productos?buscar=cam-"))
        .andExpect(jsonPath("$.totalElementos").value(2));
    sinCuerpo(get("/api/v1/productos?buscar=hikVISION"))
        .andExpect(jsonPath("$.totalElementos").value(4));
    // "_" y "%" se buscan literalmente, no como comodines.
    sinCuerpo(get("/api/v1/productos?buscar=m_d")).andExpect(jsonPath("$.totalElementos").value(0));
    sinCuerpo(get("/api/v1/productos?buscar=e_u")).andExpect(jsonPath("$.totalElementos").value(1));
    sinCuerpo(get("/api/v1/productos?buscar=%25")).andExpect(jsonPath("$.totalElementos").value(0));
  }

  @Test
  void rechazaUnOrdenPorUnCampoInexistente() throws Exception {
    sinCuerpo(get("/api/v1/productos?sort=clave"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACION"));
  }

  @Test
  void subeReemplazaYQuitaLaFoto() throws Exception {
    long id = ProductosDePrueba.crear(mvc, token, "CAM-D2", 1, 1);

    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/productos/" + id + "/foto")
                .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", JPEG))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.fotoUrl")
                .value(startsWith("http://localhost:8080/api/v1/archivos?clave=productos%2F")));
    String primera =
        jdbc.queryForObject("select foto_clave from producto where id = ?", String.class, id);

    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/productos/" + id + "/foto")
                .file(new MockMultipartFile("archivo", "otra.jpg", "image/jpeg", JPEG))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
    String segunda =
        jdbc.queryForObject("select foto_clave from producto where id = ?", String.class, id);
    Path carpeta = Path.of("target/almacenamiento-pruebas");
    assertThat(segunda)
        .isNotEqualTo(primera)
        .startsWith("productos/" + id + "/foto-")
        .endsWith(".jpg");
    assertThat(carpeta.resolve(primera)).doesNotExist();
    assertThat(carpeta.resolve(segunda)).exists();

    sinCuerpo(delete("/api/v1/productos/" + id + "/foto"))
        .andExpect(jsonPath("$.fotoUrl").value(nullValue()));
    assertThat(Files.exists(carpeta.resolve(segunda))).isFalse();
  }

  @Test
  void rechazaUnArchivoQueNoEsImagen() throws Exception {
    long id = ProductosDePrueba.crear(mvc, token, "CAM-D2", 1, 1);

    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/productos/" + id + "/foto")
                .file(
                    new MockMultipartFile(
                        "archivo", "virus.jpg", "image/jpeg", new byte[] {'M', 'Z', 0}))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ARCHIVO_TIPO_NO_PERMITIDO"));
  }

  @Test
  void laBaseDeDatosImpideStockNegativoYCodigosRepetidos() throws Exception {
    long id = ProductosDePrueba.crear(mvc, token, "CAM-D2", 1, 1);

    assertThatThrownBy(() -> jdbc.update("update producto set stock = -1 where id = ?", id))
        .hasMessageContaining("ck_producto_stock");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into producto (codigo, nombre, categoria_id, unidad_medida_id,"
                        + " precio_instalador, precio_cliente_final) values ('CAM-D2 ', 'x', 1, 1, 1, 1)"))
        .hasMessageContaining("ck_producto_codigo");
  }
}
