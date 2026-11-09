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

-- =========================================================
-- TORRES NUEVAS (entorno de simulación masiva, CU 1.1.2 / N6)
-- 8 torres nuevas (torres 4-11)
-- =========================================================

INSERT INTO towers (id, name) VALUES
    (4, 'Torre 4'), (5, 'Torre 5'), (6, 'Torre 6'), (7, 'Torre 7'),
    (8, 'Torre 8'), (9, 'Torre 9'), (10, 'Torre 10'), (11, 'Torre 11');

ALTER TABLE towers ALTER COLUMN id RESTART WITH 12;

-- =========================================================
-- APARTMENTS
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

-- =========================================================
-- APARTAMENTOS NUEVOS (simulación masiva, N6): 200 aptos, torres 4-11
-- 7 pisos × 4 aptos = 28 aptos por torre; ids 801-1000, números 901-1100
-- (fuera del rango 101-900 de torre 1 para no colisionar con UNIQUE(number))
-- =========================================================

INSERT INTO apartments (id, number, enabled, tower_id)
SELECT
    800 + X,
    CAST(900 + X AS VARCHAR),
    TRUE,
    4 + FLOOR((X - 1) / 28)
FROM SYSTEM_RANGE(1, 200);

ALTER TABLE apartments ALTER COLUMN id RESTART WITH 1001;

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

-- =========================================================
-- USERS
-- =========================================================

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
           (1,
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
       ),
       -- Residentes de apoyo para el CU 1.2.4 (panel de Administración).
       -- Mismo hash bcrypt que el residente semilla (123456).
       (
           3,
           'residente2@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Usuario Apartamento 102',
           TRUE,
           2,
           FALSE
       ),
       (
           4,
           'residente3@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Usuario Apartamento 103',
           TRUE,
           3,
           FALSE
       ),
       -- 5 usuarios simulados con postulaciones sin aprobar (PENDING_VALIDATION)
       (
           5,
           'sim001@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Residente Simulado 1',
           TRUE,
           4,
           TRUE
       ),
       (
           6,
           'sim002@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Residente Simulado 2',
           TRUE,
           5,
           TRUE
       ),
       (
           7,
           'sim003@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Residente Simulado 3',
           TRUE,
           6,
           TRUE
       ),
       (
           8,
           'sim004@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Residente Simulado 4',
           TRUE,
           7,
           TRUE
       ),
       (
           9,
           'sim005@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Residente Simulado 5',
           TRUE,
           8,
           TRUE
       );

-- =========================================================
-- 200 usuarios simulados nuevos (N2): ids 10-209
-- Emails residente<numero apto>@test.com, aptos 801-1000 (números 901-1100)
-- =========================================================
INSERT INTO users (
    id,
    email,
    password,
    name,
    enabled,
    apartment_id,
    simulated
)
SELECT
    9 + X,
    CONCAT('residente', 900 + X, '@test.com'),
    '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
    CONCAT('Residente ', 900 + X),
    TRUE,
    800 + X,
    TRUE
FROM SYSTEM_RANGE(1, 200);

-- =========================================================
-- USER ROLES
-- =========================================================

INSERT INTO user_roles (
    user_id,
    role_id
)
VALUES
           (1, 2),
           (2, 1),
           (3, 2),
           (4, 2),
           (5, 2),
           (6, 2),
           (7, 2),
           (8, 2),
           (9, 2);

-- Roles de los 200 usuarios simulados nuevos
INSERT INTO user_roles (user_id, role_id)
SELECT 9 + X, 2 FROM SYSTEM_RANGE(1, 200);

-- Los ids insertados a mano no avanzan el identity:
-- se reinicia para que los próximos inserts no choquen.
ALTER TABLE users ALTER COLUMN id RESTART WITH 210;
ALTER TABLE roles ALTER COLUMN id RESTART WITH 3;

-- =========================================================
-- CALLS
-- =========================================================

-- Solo 1 convocatoria por defecto (vigente)
INSERT INTO calls (
    id,
    title,
    description,
    start_date,
    end_date,
    total_slots,
    status,
    closed_manually
)
VALUES
           (1,
           'Convocatoria Parqueadero Comunal 2026',
           'Asignación de parqueaderos comunales mediante sorteo',
           '2026-10-01',
           '2026-12-30',
           200,
           'OPEN',
           FALSE
       );

ALTER TABLE calls ALTER COLUMN id RESTART WITH 2;

-- =========================================================
-- APPLICATIONS
-- =========================================================
-- 2 postulaciones APPROVED (usuarios residentes 2 y 3)
-- 5 postulaciones PENDING_VALIDATION (usuarios simulados sin aprobar)
-- residente@test.com (usuario 1) NO tiene postulaciones
-- RN-01: una sola postulación por (apartamento, convocatoria)

INSERT INTO applications (
    id,
    application_number,
    resident_id,
    apartment_id,
    call_id,
    status,
    created_at,
    version,
    reviewed_by,
    reviewed_at,
    rejection_reason,
    draw_result
)
VALUES
           (1,
           'APP-2026-0001',
           3,
           2,
           1,
           'APPROVED',
           '2026-10-05 09:05:00',
           0,
           'admin@test.com',
           '2026-10-05 10:05:00',
           NULL,
           NULL
       ),
       (
           2,
           'APP-2026-0002',
           4,
           3,
           1,
           'APPROVED',
           '2026-10-05 09:10:00',
           0,
           'admin@test.com',
           '2026-10-05 10:10:00',
           NULL,
           NULL
       ),
       (
           3,
           'REG-2026-0003',
           5,
           4,
           1,
           'PENDING_VALIDATION',
           '2026-10-05 09:15:00',
           0,
           NULL,
           NULL,
           NULL,
           NULL
       ),
       (
           4,
           'REG-2026-0004',
           6,
           5,
           1,
           'PENDING_VALIDATION',
           '2026-10-05 09:20:00',
           0,
           NULL,
           NULL,
           NULL,
           NULL
       ),
       (
           5,
           'REG-2026-0005',
           7,
           6,
           1,
           'PENDING_VALIDATION',
           '2026-10-05 09:25:00',
           0,
           NULL,
           NULL,
           NULL,
           NULL
       ),
       (
           6,
           'REG-2026-0006',
           8,
           7,
           1,
           'PENDING_VALIDATION',
           '2026-10-05 09:30:00',
           0,
           NULL,
           NULL,
           NULL,
           NULL
       ),
       (
           7,
           'REG-2026-0007',
           9,
           8,
           1,
           'PENDING_VALIDATION',
           '2026-10-05 09:35:00',
           0,
           NULL,
           NULL,
           NULL,
           NULL
       );

-- POSTULACIONES APPROVED DE LOS 200 SIMULADOS NUEVOS (call 1) (N2)
-- ids 8-207, created_at antes de 2026-10-05 (N9) para que los 5 PENDING_VALIDATION
-- (created_at 2026-10-05) aparezcan primero paginado.
-- Sin documentos (N2).
-- =========================================================
INSERT INTO applications (
    id,
    application_number,
    resident_id,
    apartment_id,
    call_id,
    status,
    created_at,
    version,
    reviewed_by,
    reviewed_at,
    rejection_reason,
    draw_result
)
SELECT
    7 + X,
    CONCAT('APP-SIM-', LPAD(CAST(X AS VARCHAR), 3, '0')),
    9 + X,
    800 + X,
    1,
    'APPROVED',
    '2026-10-01 09:00:00',
    0,
    'admin@test.com',
    '2026-10-01 09:00:00',
    NULL,
    NULL
FROM SYSTEM_RANGE(1, 200);

ALTER TABLE applications ALTER COLUMN id RESTART WITH 208;

-- =========================================================
-- DOCUMENTS (solo para las 2 APPROVED, con documentos completos)
-- =========================================================

INSERT INTO documents (
    id,
    application_id,
    original_name,
    content_type,
    size,
    s3_key,
    bucket,
    created_at,
    uploaded_at,
    status,
    document_type
)
VALUES
           (random_uuid(),
           1,
           'licencia_transito.pdf',
           'application/pdf',
           102400,
           'applications/1/documents/licencia_transito.pdf',
           'aes-puj-documents',
           '2026-10-05 09:35:00',
           '2026-10-05 09:35:00',
           'UPLOADED',
           'LICENCIA_TRANSITO'
       ),
       (
           random_uuid(),
           1,
           'soat_vigente.pdf',
           'application/pdf',
           204800,
           'applications/1/documents/soat_vigente.pdf',
           'aes-puj-documents',
           '2026-10-05 09:36:00',
           '2026-10-05 09:36:00',
           'UPLOADED',
           'SOAT_VIGENTE'
       ),
       (
           random_uuid(),
           1,
           'licencia_conduccion.pdf',
           'application/pdf',
           153600,
           'applications/1/documents/licencia_conduccion.pdf',
           'aes-puj-documents',
           '2026-10-05 09:37:00',
           '2026-10-05 09:37:00',
           'UPLOADED',
           'LICENCIA_CONDUCCION'
       ),
       (
           random_uuid(),
           2,
           'licencia_transito.pdf',
           'application/pdf',
           102400,
           'applications/2/documents/licencia_transito.pdf',
           'aes-puj-documents',
           '2026-10-05 09:40:00',
           '2026-10-05 09:40:00',
           'UPLOADED',
           'LICENCIA_TRANSITO'
       ),
       (
           random_uuid(),
           2,
           'soat_vigente.pdf',
           'application/pdf',
           204800,
           'applications/2/documents/soat_vigente.pdf',
           'aes-puj-documents',
           '2026-10-05 09:41:00',
           '2026-10-05 09:41:00',
           'UPLOADED',
           'SOAT_VIGENTE'
       ),
       (
           random_uuid(),
           2,
           'licencia_conduccion.pdf',
           'application/pdf',
           153600,
           'applications/2/documents/licencia_conduccion.pdf',
           'aes-puj-documents',
           '2026-10-05 09:42:00',
           '2026-10-05 09:42:00',
           'UPLOADED',
           'LICENCIA_CONDUCCION'
       );

-- =========================================================
-- COMENTARIOS FINALES
-- =========================================================
-- Resumen del estado inicial:
-- - 1 convocatoria (call 1): vigente hasta 2026-12-30, 200 cupos
-- - 209 usuarios (1 residente, 1 admin, 2 residentes apoyo, 5 simulados, 200 simulados nuevos)
-- - residente@test.com (usuario 1): SIN postulaciones
-- - 202 postulaciones APPROVED (2 residentes + 200 simulados)
-- - 5 postulaciones PENDING_VALIDATION (usuarios simulados 5-9)
-- - Total: 207 postulaciones en la convocatoria 1
