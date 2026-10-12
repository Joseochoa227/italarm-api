# Copia y restauración en local (Windows)

Guía para no perder las pruebas de ITALARM en el computador (Fase 6, T6). Las copias automáticas diarias del hosting (RNF-05) se configuran en la Fase 7.

Una copia completa tiene dos partes:
1. **La base de datos PostgreSQL:** productos, documentos, tasas, usuarios, etc.
2. **La carpeta de archivos:** fotos de productos e instalaciones, logo y facturas de compra. Es la carpeta de `ITALARM_ALMACENAMIENTO_CARPETA` en el `.env`; por defecto, `almacenamiento` dentro de `italarm-api`.

Las dos se copian juntas: la base guarda solo el nombre de cada archivo.

## Antes de empezar

- Cierra la API (Ctrl + C en la terminal donde corre `.\mvnw.cmd spring-boot:run`), para que la copia no quede a medias.
- Los programas `pg_dump` y `pg_restore` vienen con PostgreSQL. En Windows están en `C:\Program Files\PostgreSQL\16\bin`. Si `pg_dump --version` no funciona en PowerShell, usa la ruta completa: `& "C:\Program Files\PostgreSQL\16\bin\pg_dump.exe" --version`.
- Los datos de conexión son los del `.env`: base (`ITALARM_DB_URL`, por ejemplo `italarm`), usuario (`ITALARM_DB_USUARIO`) y contraseña (`ITALARM_DB_CLAVE`). PostgreSQL pide la contraseña al ejecutar cada comando.

## Hacer una copia

En PowerShell, desde la carpeta `italarm-api`:

```powershell
$fecha = Get-Date -Format "yyyy-MM-dd_HHmm"
New-Item -ItemType Directory -Force -Path "C:\copias-italarm\$fecha"

# 1. Base de datos (formato comprimido de PostgreSQL)
pg_dump -h localhost -U <usuario> -d italarm -Fc -f "C:\copias-italarm\$fecha\italarm.dump"

# 2. Carpeta de archivos
Copy-Item -Recurse .\almacenamiento "C:\copias-italarm\$fecha\almacenamiento"
```

Cambia `<usuario>` por el de `ITALARM_DB_USUARIO`. Si tu carpeta de archivos es otra, usa la ruta del `.env`.

## Restaurar una copia

Reemplaza la base actual por la de la copia. Todo lo que se registró después de la copia se pierde.

```powershell
$copia = "C:\copias-italarm\2026-10-12_1530"   # la carpeta de la copia que quieres restaurar

# 1. Borrar y crear de nuevo la base
dropdb   -h localhost -U <usuario> italarm
createdb -h localhost -U <usuario> italarm

# 2. Restaurar la base
pg_restore -h localhost -U <usuario> -d italarm --no-owner "$copia\italarm.dump"

# 3. Restaurar la carpeta de archivos
Remove-Item -Recurse -Force .\almacenamiento
Copy-Item -Recurse "$copia\almacenamiento" .\almacenamiento
```

En pgAdmin se hace lo mismo con clic derecho sobre la base: **Backup…** y **Restore…** (formato *Custom*).

Después, arranca la API como siempre. Al iniciar, Flyway valida las migraciones; si la copia es de una versión anterior del sistema, aplica las migraciones que falten.

## Comprobar que la restauración funcionó

1. La API arranca sin errores; en la terminal aparece `Successfully validated` de Flyway.
2. Ingresa, abre Inicio y revisa que los indicadores y los últimos movimientos sean los de la copia.
3. Abre el detalle de una instalación con fotos: las fotos deben verse.

Esta restauración se probó el 12/10/2026 con la base de volumen de la Fase 6 (6.000 ventas, 20.000 seriales): la base restaurada conservó todos los datos y los consecutivos, y la API arrancó sobre ella sin cambios.
