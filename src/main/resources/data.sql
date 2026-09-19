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
       );


-- =========================================================
-- CALLS
-- =========================================================

INSERT INTO calls (
    id,
    title,
    description,
    start_date,
    end_date,
    available_slots,
    status
)
VALUES (
           1,
           'Convocatoria Parqueadero Comunal 2026',
           'Asignación de parqueaderos comunales mediante sorteo',
           '2026-08-01',
           '2026-09-30',
           200,
           'OPEN'
       );














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
--     available_slots,
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
--     available_slots,
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
--     available_slots,
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
--            'REGISTERED',
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