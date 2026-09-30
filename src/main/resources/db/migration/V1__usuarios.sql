-- Usuarios del sistema (RU-01). Se ingresa con el correo (P-01).
-- Las contraseñas no se escriben aquí: las asigna la API al arrancar desde ITALARM_CLAVE_INICIAL.
CREATE TABLE usuario (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    correo          VARCHAR(254) NOT NULL,
    contrasena_hash VARCHAR(100),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_correo_minusculas CHECK (correo = lower(correo)),
    CONSTRAINT fk_usuario_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);

CREATE INDEX ix_usuario_created_by ON usuario (created_by);
CREATE INDEX ix_usuario_updated_by ON usuario (updated_by);

INSERT INTO usuario (nombre, correo)
VALUES ('Jose', 'joseochoa227@gmail.com'),
       ('Victor', 'victor8amanuelvd@gmail.com');
