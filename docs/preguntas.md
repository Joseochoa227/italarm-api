# Preguntas para ITALARM

Registro de puntos no definidos o ambiguos en `docs/requerimientos.md` (AG-05). Cada punto tiene una propuesta, pero no se implementa hasta que ITALARM la confirme.

| # | Fase | Tema | Pregunta | Propuesta del agente | Respuesta de ITALARM | Estado |
|---|---|---|---|---|---|---|
| P-01 | 0 | Usuarios iniciales | ¿Cuáles son los nombres de usuario de Jose y Victor y cómo se entregan sus contraseñas iniciales? | Usuarios `jose` y `victor`, con la contraseña inicial en variables de entorno del hosting. | Crear los usuarios en la base de datos con correo y contraseña: Jose `joseochoa227@gmail.com`, Victor `victor8amanuelvd@gmail.com`, ambos con la misma contraseña inicial por ahora (valor entregado por ITALARM, no se escribe en el repositorio). Se ingresa con el correo. La contraseña se asigna desde la variable de entorno `ITALARM_CLAVE_INICIAL` (AG-09). | Cerrada |
| P-02 | 0 | Límite de intentos de ingreso (BP-20) | ¿Cuántos intentos fallidos se permiten y por cuánto tiempo se bloquea? | 5 intentos y 15 minutos de bloqueo. | Sin límite. Riesgo aceptado (plan, sección 2). | Cerrada |
| P-03 | 0 | Duración de la sesión | ¿Cuánto tiempo sin uso cierra la sesión? | 12 horas sin actividad. | Sin límite. La sesión solo termina al cerrarla o al cambiar la contraseña. | Cerrada |
| P-04 | 0 | Proveedor de hosting | ¿En qué proveedor se despliegan el backend y la base de datos? | Render o DigitalOcean App Platform, región EE. UU. Este. | Aún no está definido. | Abierta — bloquea solo el despliegue a pruebas (T9) |
| P-05 | 0 | Dominio y sesión | ¿Se usará un dominio propio en pruebas (necesario para la cookie de sesión)? | Subdominios de `italarm.com`. | Manejar la sesión con `localStorage`. Se reemplaza la cookie + CSRF de la sección 9.1 por un token `Bearer`, que funciona con cualquier dominio. | Cerrada |
| P-06 | 0 | Swagger en producción | ¿Swagger UI debe quedar visible en producción? | Solo con sesión iniciada. | Sí, visible (público). | Cerrada |
| P-07 | 0 | Política de contraseñas | ¿Qué requisitos mínimos debe tener una contraseña? | Mínimo 8 caracteres. | Mayúscula, minúscula, número y un signo. | Cerrada |
| P-08 | 0 | Longitud mínima de contraseña | La respuesta a P-07 no indica longitud mínima. ¿Se exige alguna? | Mínimo 8 caracteres, además de los requisitos de P-07. La contraseña inicial acordada se acepta solo como inicial porque no tiene signo. | — | Abierta — se aplica la propuesta salvo que ITALARM indique otra cosa |
