# Copias de seguridad en producción

Cómo funcionan las copias automáticas y cómo restaurar una (RNF-05, P-67). Para las copias del computador, ver `copias-locales.md`.

## Qué se copia y cuándo

| Qué | Cuándo | Dónde | Cuánto se guarda |
|---|---|---|---|
| Base de datos (Neon) | Todas las noches, 3:00 a. m. (Colombia) | `italarm-copias/base/AAAA-MM-DD.dump` en R2 | Los últimos 30 días y la del día 1 de cada mes durante un año |
| Fotos, logo y facturas | Cada domingo, 4:00 a. m. | `italarm-copias/archivos/` en R2 | Todo. Lo que se borra en la app sigue en la copia |

- **Quién las hace:** los flujos **Copia de la base de datos** y **Copia de los archivos** de GitHub Actions (`.github/workflows/copia-*.yml`), con los scripts de `scripts/copias/`.
- **Para correr una copia ya:** **Actions → el flujo → Run workflow**.
- **Si una falla:** GitHub envía un correo al dueño del repositorio. Revisa el detalle en Actions.

## Restaurar la base

### Opción A — en una rama de Neon (recomendada para probar)

Sirve para la prueba trimestral (P-67) o para mirar datos de otro día sin tocar producción.

1. **Descarga la copia:** en Cloudflare, **R2 → italarm-copias → base**, descarga el archivo de la fecha que necesitas.
2. **Crea una rama vacía en Neon:** **Branches → New branch**.
3. **Restaura el archivo en esa rama** desde tu computador (PowerShell, con PostgreSQL 16 instalado):

   ```powershell
   pg_restore --no-owner --no-privileges -d "postgresql://<usuario>:<clave>@<host-de-la-rama>/<base>?sslmode=require" C:\ruta\2026-10-13.dump
   ```

4. **Revisa los datos:** puedes apuntar la API local a esa rama (`ITALARM_DB_URL` en tu `.env`) o mirarlos en pgAdmin.
5. **Borra la rama** cuando termines.

### Opción B — reemplazar la base de producción (solo si se dañó)

Todo lo registrado después de la copia se pierde. Hazlo solo con los dos de acuerdo.

1. **Detén la API:** en Render, **Suspend** el servicio, para que nadie escriba mientras restauras.
2. **Crea una rama de respaldo:** en Neon, crea una rama del estado actual. Así nada se pierde si te equivocas.
3. **Restaura:** en la rama principal, borra el esquema y restaura la copia:

   ```powershell
   psql "postgresql://<usuario>:<clave>@<host>/<base>?sslmode=require" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
   pg_restore --no-owner --no-privileges -d "postgresql://<usuario>:<clave>@<host>/<base>?sslmode=require" C:\ruta\2026-10-13.dump
   ```

4. **Vuelve a encender la API:** en Render, **Resume**. Al arrancar, Flyway valida las migraciones.
5. **Comprueba:** ingresa y revisa Inicio y los últimos movimientos.

## Restaurar fotos

Si alguien quitó una foto por error, está en `italarm-copias/archivos/` con la misma ruta (por ejemplo `instalaciones/12/antes-…jpg`). Cópiala de vuelta a `italarm-archivos` con la misma ruta, desde el panel de R2 o con la AWS CLI.

## Prueba de restauración (cada 3 meses)

- Haz la opción A con la copia más reciente.
- Comprueba que la API local abre sobre la rama y que las cifras de Inicio coinciden con las de producción de ese día.
- Anota la fecha de la prueba en el plan de la Fase 7 (o en el CHANGELOG).
