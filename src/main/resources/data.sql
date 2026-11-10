-- =========================================================
-- DATOS SEMILLA: BASE DE DATOS NUEVA
-- =========================================================
-- Solo se precargan catálogos y cuentas de prueba:
-- - 3 torres y 800 apartamentos (Torre 1, números 101-900)
-- - 2 roles (ROLE_ADMIN, ROLE_RESIDENT)
-- - 27 usuarios: admin@test.com, residente@test.com y 25 residentes de prueba
--
-- NO hay convocatorias, postulaciones ni documentos: todo se crea desde la
-- aplicación. El número de postulación sigue el formato
-- POS-{convocatoria}-{consecutivo} (p. ej. POS-1-0001).
--
-- Contraseña de todos los usuarios: 123456 (hash bcrypt).

-- =========================================================
-- TOWERS
-- =========================================================

INSERT INTO towers (
    id,
    name
)
VALUES
    (1, 'Torre 1'),
    (2, 'Torre 2'),
    (3, 'Torre 3');

ALTER TABLE towers ALTER COLUMN id RESTART WITH 4;

-- =========================================================
-- APARTMENTS (ids 1-800, números 101-900, Torre 1)
-- =========================================================

INSERT INTO apartments (
    id,
    number,
    enabled,
    tower_id
)
SELECT
    X,
    CAST(100 + X AS VARCHAR),
    TRUE,
    1
FROM SYSTEM_RANGE(1, 800);

ALTER TABLE apartments ALTER COLUMN id RESTART WITH 801;

-- =========================================================
-- ROLES
-- =========================================================

INSERT INTO roles (
    id,
    name
)
VALUES
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_RESIDENT');

ALTER TABLE roles ALTER COLUMN id RESTART WITH 3;

-- =========================================================
-- USERS
-- =========================================================
-- simulated = FALSE en todos: ninguna convocatoria nueva los preinscribe
-- automáticamente (SimulationEnrollmentService).

-- id 1: residente base (apartamento 101); usado por api-tests/test_api.py.
-- id 2: administrador (sin apartamento).
INSERT INTO users (
    id,
    email,
    password,
    name,
    enabled,
    apartment_id,
    simulated
)
VALUES
    (
        1,
        'residente@test.com',
        '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
        'Usuario Apartamento 101',
        TRUE,
        1,
        FALSE
    ),
    (
        2,
        'admin@test.com',
        '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
        'Administración',
        TRUE,
        NULL,
        FALSE
    );

-- ids 3-22: prueba01..prueba20@test.com, un apartamento distinto cada uno
-- (apartamentos 9-28 → números 109-128).
INSERT INTO users (id, email, password, name, enabled, apartment_id, simulated)
SELECT
    2 + X,
    CONCAT('prueba', LPAD(CAST(X AS VARCHAR), 2, '0'), '@test.com'),
    '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
    CONCAT('Residente Prueba ', LPAD(CAST(X AS VARCHAR), 2, '0')),
    TRUE,
    8 + X,
    FALSE
FROM SYSTEM_RANGE(1, 20);

-- ids 23-27: prueba01b..prueba05b@test.com, segundo residente del MISMO
-- apartamento que prueba01..prueba05 (números 109-113). Sirven para validar
-- RN-04: si uno de los dos ya se postuló, el otro recibe
-- APPLICATION_ALREADY_EXISTS.
INSERT INTO users (id, email, password, name, enabled, apartment_id, simulated)
SELECT
    22 + X,
    CONCAT('prueba', LPAD(CAST(X AS VARCHAR), 2, '0'), 'b@test.com'),
    '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
    CONCAT('Residente Prueba ', LPAD(CAST(X AS VARCHAR), 2, '0'), 'B'),
    TRUE,
    8 + X,
    FALSE
FROM SYSTEM_RANGE(1, 5);

-- Los ids insertados a mano no avanzan el identity:
-- se reinicia para que los próximos inserts no choquen.
ALTER TABLE users ALTER COLUMN id RESTART WITH 28;

-- =========================================================
-- USER ROLES
-- =========================================================

INSERT INTO user_roles (
    user_id,
    role_id
)
VALUES
    (1, 2),
    (2, 1);

-- Residentes de prueba (ids 3-27)
INSERT INTO user_roles (user_id, role_id)
SELECT 2 + X, 2 FROM SYSTEM_RANGE(1, 25);
