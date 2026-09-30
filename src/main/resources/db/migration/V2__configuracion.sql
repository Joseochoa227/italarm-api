-- Datos de la empresa y valores por defecto (sección 3.17). Una sola fila.
CREATE TABLE configuracion (
    id                       INTEGER       NOT NULL DEFAULT 1 PRIMARY KEY,
    empresa_nombre           VARCHAR(100)  NOT NULL,
    empresa_lema             VARCHAR(150),
    empresa_nit              VARCHAR(30),
    empresa_ciudad           VARCHAR(80),
    empresa_telefono         VARCHAR(30),
    empresa_correo           VARCHAR(254),
    empresa_logo_clave       VARCHAR(300),
    limite_variacion_tasa    NUMERIC(7, 4) NOT NULL,
    validez_cotizacion_dias  INTEGER       NOT NULL,
    garantia_mano_obra_meses INTEGER       NOT NULL,
    garantia_equipos_meses   INTEGER       NOT NULL,
    condiciones_garantia     TEXT          NOT NULL,
    pie_pdf                  TEXT          NOT NULL,
    version                  BIGINT        NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by               BIGINT,
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by               BIGINT,
    CONSTRAINT ck_configuracion_fila_unica CHECK (id = 1),
    CONSTRAINT ck_configuracion_limite_variacion CHECK (limite_variacion_tasa > 0),
    CONSTRAINT ck_configuracion_validez CHECK (validez_cotizacion_dias IN (8, 15, 30)),
    CONSTRAINT ck_configuracion_garantia_mano_obra CHECK (garantia_mano_obra_meses BETWEEN 1 AND 3),
    CONSTRAINT ck_configuracion_garantia_equipos CHECK (garantia_equipos_meses BETWEEN 1 AND 3),
    CONSTRAINT fk_configuracion_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_configuracion_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);

CREATE INDEX ix_configuracion_created_by ON configuracion (created_by);
CREATE INDEX ix_configuracion_updated_by ON configuracion (updated_by);

-- NIT, ciudad, teléfono, correo y logo quedan pendientes (sección 15, punto 6).
INSERT INTO configuracion (id, empresa_nombre, empresa_lema, limite_variacion_tasa,
                           validez_cotizacion_dias, garantia_mano_obra_meses,
                           garantia_equipos_meses, condiciones_garantia, pie_pdf)
VALUES (1,
        'ITALARM',
        'Instalación de cámaras de seguridad',
        5,
        15,
        3,
        3,
        'No cubre daños por descargas eléctricas, humedad o manipulación de terceros.',
        'Pago de contado. Garantía de 3 meses en equipos y mano de obra. Documento no válido como factura.');
