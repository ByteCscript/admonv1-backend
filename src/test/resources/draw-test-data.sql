-- =========================================================
-- TOWERS
-- =========================================================

INSERT INTO towers (
    id,
    name
)
VALUES (
           1,
           'Torre 1'
       );


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
    apartment_id
)
VALUES (
           1,
           'residente@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Usuario Apartamento 101',
           TRUE,
           1
       ),
       (
           2,
           'admin@test.com',
           '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
           'Administración',
           TRUE,
           NULL
       );


-- =========================================================
-- USER ROLES
-- =========================================================

INSERT INTO user_roles (
    user_id,
    role_id
)
VALUES (
           1,
           2
       ),
       (
           2,
           1
       );

-- Los ids insertados a mano no avanzan el identity:
-- se reinicia para que los próximos inserts no choquen.
ALTER TABLE users ALTER COLUMN id RESTART WITH 3;
ALTER TABLE roles ALTER COLUMN id RESTART WITH 3;


-- =========================================================
-- CALLS
-- =========================================================

INSERT INTO calls (
    id,
    title,
    description,
    start_date,
    end_date,
    total_slots,
    status
)
VALUES (
           1,
           'Convocatoria Parqueadero Comunal 2026',
           'Asignación de parqueaderos comunales mediante sorteo',
           '2026-10-01',
           '2026-10-30',
           200,
           'OPEN'
       ),
       (
           2,
           'Convocatoria Parqueadero Comunal 2026 - 2',
           'Asignación de parqueaderos comunales mediante sorteo',
           '2026-10-01',
           '2026-11-30',
           180,
           'OPEN'
       ),
       (
           3,
           'Convocatoria Parqueadero Comunal 2027 - 1',
           'Asignación de parqueaderos comunales mediante sorteo',
           '2026-10-01',
           '2027-11-30',
           190,
           'OPEN'
       );

-- =========================================================
-- C.U-08 · DATOS DE PRUEBA DEL SORTEO DE CUPOS
-- =========================================================
-- Mientras el C.U-07 (aceptar solicitud) no esté terminado, ninguna
-- solicitud llega a APPROVED desde la aplicación. Estos datos permiten
-- probar el sorteo de punta a punta:
--   * Convocatoria 4, cerrada por fecha, con 3 cupos.
--   * 21 solicitudes APPROVED (relación 1 cupo por 7, SUP-02): participan.
--   * 2 CANCELLED y 1 REJECTED: no participan. No hay solicitudes pendientes
--     de validación, porque bloquearían el sorteo (RN-37).
-- Residentes residente1@test.com ... residente24@test.com (clave 123456),
-- en los apartamentos 102 a 125. Retirar cuando el C.U-07 esté listo.

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
VALUES (
           4,
           'Convocatoria Parqueadero Comunal - Prueba de sorteo',
           'Convocatoria cerrada con solicitudes aprobadas para probar el sorteo de cupos (C.U-08)',
           '2026-09-01',
           '2026-09-30',
           3,
           'CLOSED',
           FALSE
       );

INSERT INTO users (
    id,
    email,
    password,
    name,
    enabled,
    apartment_id
)
SELECT
    X + 2,
    'residente' || X || '@test.com',
    '$2y$10$FOnAuAc6RbshJevn.hT.HuiRqqtdWzqt4CxsEfVUFM48/CVdQRtuS',
    'Residente Apartamento ' || (101 + X),
    TRUE,
    X + 1
FROM SYSTEM_RANGE(1, 24);

INSERT INTO user_roles (
    user_id,
    role_id
)
SELECT
    X + 2,
    2
FROM SYSTEM_RANGE(1, 24);

INSERT INTO applications (
    id,
    application_number,
    resident_id,
    apartment_id,
    call_id,
    status,
    created_at,
    version
)
SELECT
    X,
    'POST-SORTEO-' || LPAD(CAST(X AS VARCHAR), 3, '0'),
    X + 2,
    X + 1,
    4,
    CASE
        WHEN X <= 21 THEN 'APPROVED'
        WHEN X <= 23 THEN 'CANCELLED'
        ELSE 'REJECTED'
    END,
    DATEADD('MINUTE', X, TIMESTAMP '2026-09-10 08:00:00'),
    0
FROM SYSTEM_RANGE(1, 24);

ALTER TABLE users ALTER COLUMN id RESTART WITH 27;
ALTER TABLE applications ALTER COLUMN id RESTART WITH 25;

-- Los ids 1-4 se insertaron a mano: se reinicia el identity de calls
-- para que una convocatoria creada por la API no choque con ellos.
ALTER TABLE calls ALTER COLUMN id RESTART WITH 5;
















-- -- =========================================================
-- -- APARTMENTS
-- -- =========================================================
--
-- INSERT INTO apartments (id, number, enabled)
-- SELECT
--     X,
--     CAST(100 + X AS VARCHAR),
--     TRUE
-- FROM SYSTEM_RANGE(1, 800);
--
--
-- -- =========================================================
-- -- RESIDENTS
-- -- =========================================================
--
-- INSERT INTO residents (
--     id,
--     name,
--     document,
--     apartment_id
-- )
-- SELECT
--     X,
--     CONCAT('Resident ', X),
--     CONCAT('100000', LPAD(CAST(X AS VARCHAR), 4, '0')),
--     X
-- FROM SYSTEM_RANGE(1, 800);
--
--
-- -- =========================================================
-- -- CALLS
-- -- =========================================================
--
-- INSERT INTO calls (
--     id,
--     title,
--     description,
--     start_date,
--     end_date,
--     total_slots,
--     status
-- )
-- VALUES (
--            1,
--            'Convocatoria Parqueadero Comunal 2026',
--            'Asignación de parqueaderos comunales mediante sorteo',
--            '2026-08-01',
--            '2026-09-30',
--            200,
--            'OPEN'
--        );
--

-- =========================================================
-- NO INSERTAMOS APPLICATIONS
-- =========================================================


-- -- APARTMENTS
-- INSERT INTO apartments (id, number, enabled)
-- VALUES (1, '101', true);
--
-- INSERT INTO apartments (id, number, enabled)
-- VALUES (2, '202', true);
--
--
-- -- RESIDENTS
-- INSERT INTO residents (id, name, document, apartment_id)
-- VALUES (1, 'Cristian Gomez', '100000001', 1);
--
-- INSERT INTO residents (id, name, document, apartment_id)
-- VALUES (2, 'Juan Perez', '100000002', 2);
--
--
-- -- CALLS
-- INSERT INTO calls (
--     id,
--     title,
--     description,
--     start_date,
--     end_date,
--     total_slots,
--     status
-- )
-- VALUES (
--            1,
--            'Convocatoria Parqueadero Comunal 2026',
--            'Asignación de parqueaderos comunales mediante sorteo',
--            '2026-08-01',
--            '2026-09-30',
--            15,
--            'OPEN'
--        );
--
-- INSERT INTO calls (
--     id,
--     title,
--     description,
--     start_date,
--     end_date,
--     total_slots,
--     status
-- )
-- VALUES (
--            2,
--            'Convocatoria Parqueadero Torre B 2026',
--            'Asignación de parqueaderos de la Torre B',
--            '2026-08-01',
--            '2026-09-15',
--            10,
--            'OPEN'
--        );
--
--
-- -- APPLICATIONS
-- INSERT INTO applications (
--     id,
--     application_number,
--     resident_id,
--     apartment_id,
--     call_id,
--     status,
--     created_at
-- )
-- VALUES (
--            1,
--            'POST-2026-00001',
--            1,
--            1,
--            1,
--            'PENDING_VALIDATION',
--            '2026-08-28 09:00:00'
--        );
--
-- INSERT INTO applications (
--     id,
--     application_number,
--     resident_id,
--     apartment_id,
--     call_id,
--     status,
--     created_at
-- )
-- VALUES (
--            2,
--            'POST-2026-00002',
--            2,
--            2,
--            2,
--            'PENDING_VALIDATION',
--            '2026-08-28 09:15:00'
--        );