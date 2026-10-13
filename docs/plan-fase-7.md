# Plan de la Fase 7 — Salida a producción en planes gratuitos (italarm-api)

> Estado: **aprobado por ITALARM el 13/10/2026**. Preguntas P-64 a P-71 respondidas: de acuerdo con las propuestas (ver `docs/preguntas.md`).
> La preparación en el repositorio (T1 a T3 y T8) se hace ya. El despliegue (T5 a T7) se hace **después** de que ITALARM valide la Fase 6 en local (plan de la Fase 6, sección 9).
> Base: `docs/requerimientos.md`, secciones 5 (RNF-02, RNF-05, RNF-08, RNF-10), 9 y 12.8, más P-04 y P-56.
> AG-12: nada se publica en producción sin la aprobación de ITALARM en el paso T5.

## 1. Objetivo y entregable

Poner el sistema en internet, sin costo, para que Jose y Victor lo usen desde el celular y el computador con los datos reales de ITALARM.

Entregable (12.8):
- sistema en producción con los datos reales;
- manual corto de uso;
- restauración de una copia de seguridad probada.

## 2. Arquitectura (P-04)

| Parte | Servicio (plan gratis) | Dirección |
|---|---|---|
| Frontend (italarm-web) | Cloudflare Pages | `https://<nombre>.pages.dev` |
| Backend (italarm-api) | Render, Web Service gratis con Docker, región Virginia | `https://<nombre>.onrender.com` |
| Base de datos | Neon (PostgreSQL 16), región AWS EE. UU. Este | Solo la conoce el backend |
| Fotos, logo y facturas | Cloudflare R2, bucket `italarm-archivos` | Enlaces firmados de corta duración |
| Copias de seguridad | GitHub Actions → Cloudflare R2, bucket `italarm-copias` | Privado |
| Monitoreo | UptimeRobot | Llama a `/actuator/health/liveness` cada 5 minutos |

**Cómo funciona:**
- El celular o el computador abre el frontend en Cloudflare.
- El frontend llama a la API en Render con el token de sesión.
- La API usa Neon para los datos y R2 para los archivos.
- UptimeRobot mantiene despierta la API y avisa si se cae.
- Cada noche, GitHub Actions saca una copia de la base y la guarda en R2.

**Lo que obliga el plan gratis:**
- **Render gratis se duerme a los 15 minutos sin uso.** UptimeRobot lo mantiene despierto llamando a un chequeo que **no** consulta la base. Así Neon se apaga cuando nadie usa la app y no gasta sus horas gratis.
- **512 MB de memoria:** la JVM y el pool de conexiones se ajustan (T1).
- **Neon gratis solo deja recuperar las últimas horas:** las copias diarias propias (T2) cubren RNF-05.
- **Sin disco persistente en Render:** los archivos van siempre a R2, que ya está implementado.
- **Las condiciones de los planes gratis pueden cambiar.** Cada pieza se puede mover a otro proveedor sin tocar el código, solo la configuración.

## 3. Qué queda fuera de esta fase

- Un dominio propio (P-66): se puede agregar después sin cambios de código.
- Un ambiente de pruebas en la nube (P-65): las pruebas siguen en local.
- El paso a Spring Boot 4: se planea aparte. Dependabot avisará de las actualizaciones.

## 4. Tareas

### T1. Ajustes del backend para el plan gratis — `build:` / `ci:`

- [x] **Perfil `prod`:**
  - pool de conexiones pequeño: 3 conexiones como máximo, ninguna fija, se cierran tras 1 minuto sin uso, para que Neon pueda apagarse;
  - conexión a Neon con `sslmode=require`;
  - Tomcat con pocos hilos.
- [x] **Imagen Docker** con memoria limitada para 512 MB: `MaxRAMPercentage` más bajo, recolector Serial y pila de hilos más pequeña.
  - Se prueba en local con `docker run --memory=512m` y los datos de volumen de la Fase 6: Inicio, reportes y un Excel grande sin quedarse sin memoria.
- [x] **Chequeo de vida:** `/actuator/health/liveness` responde sin consultar la base. Se agrega una prueba de esto.
  - El chequeo de la imagen y el de Render usan esa misma ruta.
- [x] **CI:** también en cada push a `dev`, no solo a `main`. Dependabot semanal para Maven y GitHub Actions (avisos de seguridad).
- [x] **Variables de producción** documentadas en `.env.example` y en la guía de despliegue. Los secretos solo van en Render y GitHub (AG-09).

### T2. Copias de seguridad automáticas (RNF-05) — `ci:`

- [x] **Flujo `copia-base.yml`:** todas las noches (P-67) saca un `pg_dump` comprimido de Neon y lo sube a `italarm-copias/base/` en R2.
  - Guarda las 30 copias diarias más recientes y una por mes durante 12 meses.
  - También se puede correr a mano desde GitHub.
- [x] **Flujo `copia-archivos.yml`:** una vez por semana copia `italarm-archivos` a `italarm-copias/archivos/` (fotos, logo y facturas).
- [x] Si una copia falla, GitHub envía un correo al dueño del repositorio.
- [x] **Guía `docs/copias-produccion.md`:** cómo ver las copias, descargar una y restaurarla en Neon o en local.

### T3. Guía de despliegue paso a paso — `docs:` ✅

`docs/despliegue.md` (hecha), para que ITALARM cree las cuentas a su nombre (RNF-10). Para cada paso dice qué botón tocar, qué copiar y dónde pegarlo:

1. **Cloudflare:** crear la cuenta, los buckets `italarm-archivos` e `italarm-copias`, y una clave de API de R2 con acceso solo a esos buckets.
2. **Neon:** crear el proyecto `italarm` en EE. UU. Este y copiar la URL de conexión.
3. **Render:**
   - crear el Web Service desde el repositorio `italarm-api`, rama `main`, con Docker, región Virginia y chequeo `/actuator/health/liveness`;
   - cargar las variables de entorno: base, R2, `ITALARM_CLAVE_INICIAL`, `ITALARM_ENLACES_CLAVE` (generada al azar), `ITALARM_URL_PUBLICA` y `ITALARM_CORS_ORIGENES`.
4. **GitHub:** guardar en el repositorio los secretos de las copias (URL de Neon y clave de R2).
5. **UptimeRobot:** crear el monitor cada 5 minutos con alerta por correo (P-70).
6. **Cloudflare Pages:** conectar `italarm-web` (rama `main`) con `VITE_API_URL` apuntando a Render.
7. **Revisión final:** recorrer la lista de verificación de la guía.

### T4. Cambios del frontend (en la sesión de italarm-web)

No se hacen en este repositorio. Se piden en la sesión del frontend, con esta lista:
- configuración de Cloudflare Pages: comando de build, carpeta `dist`, `VITE_API_URL`, y `_redirects` para que las rutas de la SPA funcionen al recargar;
- PWA instalable con el ícono de ITALARM (BF-13);
- aviso claro cuando la API tarda en responder (por ejemplo, si Render estaba despertando).

### T5. Primer despliegue (con ITALARM) — AG-12

- [ ] ITALARM sigue la guía de T3. Yo reviso cada paso con lo que me cuente o me muestre.
- [ ] **Prueba de humo en producción:**
  - ingresar y cambiar la contraseña;
  - registrar la tasa del bolívar y ver la TRM automática al día siguiente;
  - subir una foto;
  - descargar un PDF y abrir un enlace de WhatsApp sin sesión;
  - exportar un Excel;
  - ver que UptimeRobot marque la API arriba.
- [ ] La aprobación de ITALARM queda anotada en el plan antes de cargar los datos reales.

### T6. Datos reales y usuarios (P-68, P-69)

- [ ] La producción empieza **vacía** (solo las migraciones). Los datos de prueba del computador no se suben.
- [ ] Carga inicial con la plantilla de Excel: productos con stock, costo y seriales, clientes y proveedores (sección 3.18).
- [ ] Configuración: datos de la empresa, logo, condiciones de garantía y pie de los PDF.
- [ ] Jose y Victor ingresan con la contraseña inicial y la cambian el primer día.

### T7. Copia y restauración probadas en producción

- [ ] Correr a mano el flujo de copia y comprobar el archivo en R2.
- [ ] Restaurar esa copia en una rama nueva de Neon (o en el PostgreSQL local). Abrir la API apuntando a ella y ver los mismos datos.
- [ ] Repetir la prueba cada 3 meses (P-67), con un recordatorio en el manual.

### T8. Manual corto de uso (RNF-10) — `docs:`

- [x] `docs/manual-de-uso.md` (P-71), por pantalla, con lo que se hace en el día a día:
  - ingresar, registrar la tasa del bolívar, comprar, vender, instalar con fotos, cotizar y convertir;
  - anular, consultar garantías y reclamos;
  - reportes y Excel;
  - qué hacer si la app no carga: esperar un minuto o revisar UptimeRobot;
  - cómo pedir una copia.

## 5. Variables de entorno de producción

| Variable | Dónde se obtiene |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Fija |
| `ITALARM_DB_URL`, `ITALARM_DB_USUARIO`, `ITALARM_DB_CLAVE` | Neon (conexión directa, con `sslmode=require`) |
| `ITALARM_ALMACENAMIENTO=s3`, `ITALARM_S3_ENDPOINT`, `ITALARM_S3_REGION=auto`, `ITALARM_S3_BUCKET=italarm-archivos`, `ITALARM_S3_ACCESS_KEY`, `ITALARM_S3_SECRET_KEY` | Cloudflare R2 |
| `ITALARM_URL_PUBLICA` | La dirección de Render (para los enlaces de los comprobantes) |
| `ITALARM_CORS_ORIGENES` | La dirección de Cloudflare Pages |
| `ITALARM_CLAVE_INICIAL` | La elige ITALARM; solo sirve hasta que cada uno cambie su contraseña |

## 6. Pruebas

- Prueba automática: `/actuator/health/liveness` responde 200 sin consultar la base.
- Prueba local de memoria: la imagen Docker con `--memory=512m` y los datos de volumen. Se anotan el consumo y los tiempos.
- Los flujos de copia se prueban con la ejecución manual de GitHub contra una base de prueba antes de apuntarlos a producción.
- Los casos CP-01 a CP-29 siguen pasando en la CI.

## 7. Definición de terminado

- [ ] La API y el frontend responden en internet con HTTPS, y UptimeRobot los marca arriba durante una semana.
- [ ] Prueba de humo aprobada por ITALARM (T5).
- [ ] Datos reales cargados y contraseñas cambiadas (T6).
- [ ] Copias nocturnas funcionando y una restauración probada (T7).
- [ ] Manual de uso entregado (T8).
- [ ] CHANGELOG, README y CLAUDE.md actualizados.

## 8. Riesgos

- **Cambio de condiciones de un plan gratis:** cada pieza se cambia sin tocar el código. Alternativas:
  - API: Koyeb o Oracle Cloud;
  - base de datos: Supabase u otra PostgreSQL;
  - archivos: cualquier S3.
- **Memoria de 512 MB:** si no alcanza, el primer paso es bajar el límite de filas de los Excel. El segundo, un plan de pago de Render (unos US$ 7 al mes).
- **Render despertando:** si UptimeRobot falla, la primera petición tarda 30–60 segundos. Las tareas de la TRM y de vencimiento de cotizaciones corren también al arrancar, así que no se pierden.
- **Neon apagado de madrugada:** la primera consulta tarda menos de un segundo más; no afecta el uso.

## 9. Resultados de la preparación (13/10/2026)

### Memoria con 512 MB (T1)

**Cómo se midió:**
- La API corrió con `-XX:MaxRAM=512m` y las opciones de la imagen Docker, sobre los datos de volumen de la Fase 6. No se pudo usar el contenedor con `--memory=512m` porque Docker Hub limitó la descarga de la imagen de Java.
- Se pidieron Inicio, reportes y los Excel más grandes, tres veces cada uno.

| Opciones de la JVM | Memoria real máxima | Excel más grande |
|---|---|---|
| Montón al 60 %, C2 | 472 MB (poco margen) | 0,5 s |
| **Montón al 50 %, Serial, solo C1** (las de la imagen) | **410 MB** | 0,9 s |

Ningún error de memoria. Inicio y reportes siguen por debajo de 100 ms.

### Copias (T2)

`scripts/copias/copia-base.sh` y `copia-archivos.sh` se probaron contra un S3 simulado (moto) y PostgreSQL 16:
- la copia sube a `base/AAAA-MM-DD.dump`;
- la retención borra lo que tiene más de 30 días y conserva el día 1 de cada mes durante un año;
- la copia de archivos sincroniza el bucket;
- el `.dump` se restauró completo en una base nueva.

Los flujos de GitHub solo corren cuando la variable `COPIAS_ACTIVAS` vale `true`; se activa en el paso 4 de `docs/despliegue.md`.

### Pendiente (después de la validación en local)

- T4: pedir los cambios del frontend en su sesión.
- T5 a T7: seguir `docs/despliegue.md` con ITALARM, prueba de humo, datos reales y restauración en producción.
