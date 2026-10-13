# Guía de despliegue en planes gratuitos

Paso a paso para publicar ITALARM en internet sin costo (Fase 7, P-04). Las cuentas se crean a nombre de ITALARM (RNF-10). Toma alrededor de una hora la primera vez.

**Arquitectura**

| Parte | Dónde |
|---|---|
| Frontend | Cloudflare Pages |
| Backend | Render |
| Base de datos | Neon |
| Fotos, logo y facturas | Cloudflare R2 |
| Copias de seguridad | GitHub Actions → R2 |
| Alertas | UptimeRobot |

Los nombres de los botones pueden cambiar un poco con el tiempo; si algo no coincide, busca la opción con un nombre parecido.

> **Nunca** pegues claves o contraseñas en el repositorio, en un chat ni en un documento compartido. Van solo en los formularios de Render y de GitHub que indica esta guía (AG-09). Mientras las copias de un servicio a otro, guárdalas en un bloc de notas que borres al terminar.

## 0. Antes de empezar

- [ ] La Fase 6 está validada en local (plan de la Fase 6, sección 9).
- [ ] En **italarm-api**, `dev` pasa a `main`:
  - en GitHub, abre **Pull requests → New pull request**, con base `main` y comparar `dev`;
  - cuando la verificación (CI) esté en verde, toca **Merge**.
- [ ] Haz lo mismo en **italarm-web** cuando su sesión tenga lista la configuración para Cloudflare Pages (plan de la Fase 7, T4).

Render y Cloudflare publican solos cada cambio que llegue a `main` (P-64).

## 1. Cloudflare R2: fotos y copias

1. Crea la cuenta en https://dash.cloudflare.com. Para activar R2, Cloudflare puede pedir un medio de pago, aunque no cobra dentro del plan gratis (10 GB).
2. Entra a **R2 Object Storage** y crea dos buckets, en la ubicación *Eastern North America*:
   - `italarm-archivos`: fotos, logo y facturas;
   - `italarm-copias`: copias de seguridad.
3. En **R2 → Manage API tokens**, crea un token:
   - permiso **Object Read & Write**;
   - aplicado **solo** a esos dos buckets.
4. Copia en tu bloc de notas:
   - **Access Key ID**;
   - **Secret Access Key**: solo se muestra una vez;
   - el **endpoint S3**: `https://<id-de-cuenta>.r2.cloudflarestorage.com`.

## 2. Neon: base de datos

1. Crea la cuenta en https://neon.tech.
2. Crea un proyecto:
   - nombre `italarm`;
   - **PostgreSQL 16**;
   - región **AWS US East (N. Virginia)**.
3. En **Connection details**, elige la conexión **directa**, no la "pooled". Copia:
   - el **host** (algo como `ep-xxxx.us-east-1.aws.neon.tech`);
   - la **base** (por ejemplo `neondb`);
   - el **usuario** y la **contraseña**;
   - la **URL completa** que empieza por `postgresql://`, para las copias.
4. Arma la URL para la API así:
   `jdbc:postgresql://<host>/<base>?sslmode=require`

## 3. Render: backend

1. Crea la cuenta en https://render.com, entrando con GitHub, y autoriza el acceso al repositorio `italarm-api`.
2. Toca **New → Web Service** y elige `italarm-api`.
3. Configura:
   - **Branch:** `main`;
   - **Language / Runtime:** Docker; usa el `Dockerfile` del repositorio;
   - **Region:** Virginia (US East);
   - **Instance type:** Free;
   - **Health Check Path** (en *Advanced*): `/actuator/health/liveness`;
   - **Auto-Deploy:** sí.
4. Agrega las variables de entorno:

   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `ITALARM_DB_URL` | La URL `jdbc:postgresql://…?sslmode=require` del paso 2 |
   | `ITALARM_DB_USUARIO` | El usuario de Neon |
   | `ITALARM_DB_CLAVE` | La contraseña de Neon |
   | `ITALARM_ALMACENAMIENTO` | `s3` |
   | `ITALARM_S3_ENDPOINT` | El endpoint S3 de R2 |
   | `ITALARM_S3_REGION` | `auto` |
   | `ITALARM_S3_BUCKET` | `italarm-archivos` |
   | `ITALARM_S3_ACCESS_KEY` | El Access Key ID de R2 |
   | `ITALARM_S3_SECRET_KEY` | El Secret Access Key de R2 |
   | `ITALARM_CLAVE_INICIAL` | Una contraseña inicial fuerte, que eliges tú (P-69) |
   | `ITALARM_URL_PUBLICA` | La dirección que Render le da al servicio, por ejemplo `https://italarm-api.onrender.com` (ponla cuando la conozcas) |
   | `ITALARM_CORS_ORIGENES` | Por ahora `http://localhost:5173`; en el paso 6 la cambias por la de Cloudflare Pages |

5. Toca **Create Web Service**.
   - El primer despliegue tarda unos minutos: construye la imagen, crea las tablas en Neon (Flyway) y crea a Jose y Victor con la contraseña inicial.
   - Cuando termine, actualiza `ITALARM_URL_PUBLICA` con la dirección real, si no la tenías.
6. **Comprueba:**
   - `https://<tu-api>.onrender.com/actuator/health/liveness` responde `{"status":"UP"}`;
   - `https://<tu-api>.onrender.com/swagger-ui.html` abre.

## 4. GitHub: copias de seguridad automáticas

1. En el repositorio `italarm-api` en GitHub, entra a **Settings → Secrets and variables → Actions**.
2. En la pestaña **Secrets**, agrega:

   | Secreto | Valor |
   |---|---|
   | `COPIA_DB_URL` | La URL `postgresql://…` completa de Neon (paso 2) |
   | `R2_ENDPOINT` | El endpoint S3 de R2 |
   | `R2_ACCESS_KEY_ID` | El Access Key ID de R2 |
   | `R2_SECRET_ACCESS_KEY` | El Secret Access Key de R2 |

3. En la pestaña **Variables**, agrega:
   - `COPIAS_BUCKET` = `italarm-copias`;
   - `ARCHIVOS_BUCKET` = `italarm-archivos`;
   - `COPIAS_ACTIVAS` = `true`.
4. En **Actions**, abre **Copia de la base de datos** y toca **Run workflow**. En unos minutos debe terminar en verde, y en R2 aparece `italarm-copias/base/<fecha>.dump`. Haz lo mismo con **Copia de los archivos**.
5. Desde ahí, la base se copia cada noche a las 3:00 a. m. y los archivos cada domingo. Si una copia falla, GitHub envía un correo al dueño del repositorio.

## 5. UptimeRobot: mantener la API despierta y alertas

1. Crea la cuenta en https://uptimerobot.com, con un correo que revisen los dos (P-70).
2. Crea un monitor:
   - **tipo:** HTTP(s);
   - **URL:** `https://<tu-api>.onrender.com/actuator/health/liveness`;
   - **intervalo:** 5 minutos.
3. En **Alert contacts** puedes agregar el correo de Victor.

Este monitor evita que Render duerma la API, y avisa por correo si deja de responder.

## 6. Cloudflare Pages: frontend

1. En Cloudflare, entra a **Workers & Pages → Create → Pages → Connect to Git** y elige `italarm-web`.
2. Configura:
   - **Production branch:** `main`;
   - **Build command:** `npm run build`;
   - **Build output directory:** `dist`;
   - **Variable de entorno:** `VITE_API_URL` = `https://<tu-api>.onrender.com`.
3. Toca **Save and Deploy**. Cloudflare te da la dirección, por ejemplo `https://italarm.pages.dev`.
4. Vuelve a Render y cambia `ITALARM_CORS_ORIGENES` por esa dirección exacta, sin `/` al final. Render se reinicia solo.

## 7. Prueba de humo (T5)

Desde el celular y el computador, en la dirección de Cloudflare Pages:

- [ ] Ingresar con tu correo y la contraseña inicial; cambiarla. Victor hace lo mismo.
- [ ] Registrar la tasa del bolívar. Al día siguiente, la TRM debe aparecer sola.
- [ ] Crear un producto con foto: la foto se ve, y en R2 aparece en `italarm-archivos`.
- [ ] Registrar una venta de prueba, descargar el PDF y abrir el enlace de WhatsApp en otro teléfono sin sesión. Luego anularla.
- [ ] Exportar un Excel.
- [ ] Instalar la app en el celular desde el navegador ("Agregar a la pantalla de inicio").
- [ ] UptimeRobot muestra la API arriba.

Si todo está bien, anota la aprobación en el plan de la Fase 7 y sigue con los datos reales (T6): configuración de la empresa, logo y carga inicial con la plantilla de Excel.

## Si algo falla

- **Render no arranca:** revisa **Logs** en Render.
  - `password authentication failed`: el usuario o la contraseña de Neon no coinciden.
  - `Connection refused`: revisa el host de `ITALARM_DB_URL` y que tenga `?sslmode=require`.
- **El frontend dice que no puede conectar:** `ITALARM_CORS_ORIGENES` debe ser exactamente la dirección de Pages, y `VITE_API_URL` la de Render, ambas con `https://`.
- **Las fotos no suben:** revisa las cuatro variables `ITALARM_S3_*` y que el token de R2 tenga permiso sobre `italarm-archivos`.
- **La app tarda un minuto en abrir:** Render estaba dormido (UptimeRobot no lo llamó). Revisa que el monitor esté activo.
