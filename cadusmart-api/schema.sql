-- CaduSmart · esquema PostgreSQL (Neon)

CREATE TYPE user_role AS ENUM ('PERSONAL', 'COMERCIO_ADMIN', 'COMERCIO_EMPLEADO');

CREATE TABLE commerces (
  id            SERIAL PRIMARY KEY,
  razon_social  TEXT NOT NULL
);

CREATE TABLE users (
  id                    SERIAL PRIMARY KEY,
  commerce_id           INT REFERENCES commerces(id) ON DELETE CASCADE, -- NULL si PERSONAL
  nombre                TEXT NOT NULL,
  email                 TEXT NOT NULL UNIQUE,
  password_hash         TEXT NOT NULL,
  role                  user_role NOT NULL,
  active                BOOLEAN NOT NULL DEFAULT TRUE,
  must_change_password  BOOLEAN NOT NULL DEFAULT FALSE, -- contraseña temporal / restablecida
  alert_days            INT NOT NULL DEFAULT 2 CHECK (alert_days BETWEEN 1 AND 30),
  notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Eliminar usuario (hard delete) borra también sus productos
CREATE TABLE products (
  id           SERIAL PRIMARY KEY,                 -- se muestra como P-0142
  owner_id     INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  commerce_id  INT REFERENCES commerces(id) ON DELETE CASCADE,
  name         TEXT NOT NULL,
  category     TEXT NOT NULL,
  quantity     INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
  lot          TEXT,
  barcode      TEXT,
  expiry_date  DATE NOT NULL,
  consumed_at  TIMESTAMPTZ,                        -- estado Consumido/Vendido
  deleted_at   TIMESTAMPTZ,                        -- borrado lógico
  deleted_by   INT REFERENCES users(id) ON DELETE SET NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_owner   ON products(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_expiry  ON products(expiry_date);

-- El estado (Fresco / Por vencer / Vencido) NO se guarda: se calcula
-- con expiry_date y users.alert_days. Solo consumed_at es estado manual.
--
-- Reglas de endpoints (resumen):
--  GET    /products                 -> deleted_at IS NULL
--  DELETE /products/:id             -> UPDATE deleted_at = now()   (solo PERSONAL y COMERCIO_ADMIN)
--  GET    /products/deleted         -> solo COMERCIO_ADMIN
--  DELETE /products/:id/purge       -> borrado definitivo, solo COMERCIO_ADMIN
--  GET/POST/PUT/DELETE /users       -> solo COMERCIO_ADMIN
--  POST   /users/:id/reset-password -> COMERCIO_ADMIN (cualquiera de su comercio)
--  POST   /me/reset-password        -> COMERCIO_EMPLEADO (la suya)
