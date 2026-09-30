# Preguntas para ITALARM

Registro de puntos no definidos o ambiguos en `docs/requerimientos.md` (AG-05). Cada punto tiene una propuesta, pero no se implementa hasta que ITALARM la confirme.

| # | Fase | Tema | Pregunta | Propuesta del agente | Estado |
|---|---|---|---|---|---|
| P-01 | 0 | Usuarios iniciales | ¿Cuáles son los nombres de usuario de Jose y Victor y cómo se entregan sus contraseñas iniciales? | Usuarios `jose` y `victor`. Contraseña inicial definida por ITALARM en las variables de entorno del hosting (nunca en el repositorio). Recomendación: que cada uno la cambie en su primer ingreso (no se obliga, porque el documento no lo pide). | Abierta |
| P-02 | 0 | Límite de intentos de ingreso (BP-20) | ¿Cuántos intentos fallidos se permiten y por cuánto tiempo se bloquea? | 5 intentos fallidos seguidos bloquean el usuario 15 minutos. Un ingreso correcto reinicia el contador. | Abierta |
| P-03 | 0 | Duración de la sesión | ¿Cuánto tiempo sin uso cierra la sesión? | 12 horas sin actividad (una jornada de instalación). La sesión sobrevive a los redespliegues. | Abierta |
| P-04 | 0 | Proveedor de hosting | ¿En qué proveedor se despliegan el backend y la base de datos (Render, Railway, DigitalOcean App Platform o AWS)? ¿Ya existe la cuenta a nombre de ITALARM? | Render o DigitalOcean App Platform, región EE. UU. Este, con PostgreSQL administrado del mismo proveedor. Se necesita acceso para configurar el despliegue desde GitHub Actions. | Abierta — bloquea el despliegue (T9) |
| P-05 | 0 | Dominio | ¿ITALARM ya tiene el dominio `italarm.com`? ¿Qué subdominios se usarán en pruebas? | `api-pruebas.italarm.com` y `app-pruebas.italarm.com`. Hace falta un dominio propio también en pruebas para que la cookie de sesión funcione entre frontend y backend. | Abierta — bloquea el despliegue (T9) |
| P-06 | 0 | Documentación de la API en producción | ¿Swagger UI debe quedar visible en producción? | Público en local y pruebas; en producción solo con sesión iniciada. | Abierta |
| P-07 | 0 | Política de contraseñas | ¿Qué requisitos mínimos debe tener una contraseña? | Mínimo 8 caracteres, distinta de la actual y del nombre de usuario. | Abierta |
