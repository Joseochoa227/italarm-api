-- Sesiones abiertas con token (P-05). Solo se guarda el hash SHA-256 del token.
-- No vencen (P-03): terminan al cerrar sesión o al cambiar la contraseña.
CREATE TABLE sesion (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL,
    agente_usuario VARCHAR(300),
    creada_en      TIMESTAMPTZ  NOT NULL,
    ultimo_uso     TIMESTAMPTZ  NOT NULL,
    revocada_en    TIMESTAMPTZ,
    CONSTRAINT uq_sesion_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_sesion_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX ix_sesion_usuario_activa ON sesion (usuario_id) WHERE revocada_en IS NULL;
