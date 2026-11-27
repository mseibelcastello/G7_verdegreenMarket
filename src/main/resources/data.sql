-- =====================================================================
-- DATOS DE PRUEBA
-- Se ejecuta en cada arranque (spring.sql.init.mode=always). Todos los
-- INSERT son IGNORE y los UPDATE son idempotentes, así que se puede
-- correr muchas veces sin duplicar nada.
-- =====================================================================

-- ---------------------------------------------------------------------
-- CLIENTES
--   30111222 Ana   -> muchos pedidos (CU-02 y CU-04, filtros)
--   22333444 Juan  -> un solo pedido
--   40555666 Lucía -> sin pedidos (CU-04 FA-02: lista vacía)
--   99999999       -> NO existe (CU-02 / CU-04 FE-01: 404)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO clientes (dni, nombre, apellido, email, telefono)
VALUES ('30111222', 'Ana', 'Pérez', 'ana@mail.com', '3794000000'),
       ('22333444', 'Juan', 'Pérez', 'juan@mail.com', '3794000000'),
       ('40555666', 'Lucía', 'Gómez', 'lucia@mail.com', '3794111111');

-- ---------------------------------------------------------------------
-- USUARIOS INTERNOS (CU-01: el vendedor que carga la promoción)
--   30111222 -> vendedor (es el DNI del ejemplo de Swagger)
--   99999999 -> NO existe (CU-01: 404 vendedor inexistente)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO usuario_interno (dni, nombre, email, rol)
VALUES ('30111222', 'Carla Vendedora', 'carla@verdegreen.com', 'VENDEDOR'),
       ('28999888', 'Pablo Logística', 'pablo@verdegreen.com', 'LOGISTICA');

-- ---------------------------------------------------------------------
-- PRODUCTOS
--   CU-03 (stock disponible = físico - reservado):
--     1, 2, 6 -> disponibles
--     3       -> stock bajo (25 - 5 = 20 disponibles)
--     4       -> sin stock (físico 0)
--     5       -> sin stock (todo reservado: 30 - 30 = 0)
--   99 -> NO existe (CU-01 / CU-03: 404 producto inexistente)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO productos (codigo, nombre, descripcion, categoria, precio_pack, unidades_pack, stock_fisico, stock_reservado, stock_minimo, estado)
VALUES (1, 'Vaso PET 250cc', 'Pack 10 un.', 'Vasos', 4500, 10, 100, 0, 20, 'DISPONIBLE'),
       (2, 'Vaso PET 500cc', 'Pack 10 un.', 'Vasos', 6800, 10, 80, 0, 20, 'DISPONIBLE'),
       (3, 'Bandeja PET 500g', 'Pack 8 un.', 'Bandejas', 5200, 8, 25, 5, 20, 'STOCK_BAJO'),
       (4, 'Clamshell PET 750cc', 'Pack 8 un.', 'Contenedores', 7400, 8, 0, 0, 10, 'SIN_STOCK'),
       (5, 'Clamshell PET 1000cc', 'Pack 8 un.', 'Contenedores', 8900, 8, 30, 30, 10, 'SIN_STOCK'),
       (6, 'Tapa PET para vaso', 'Pack 20 un.', 'Otros', 3200, 20, 200, 0, 40, 'DISPONIBLE');

-- ---------------------------------------------------------------------
-- PROMOCIONES (CU-01)
-- Promo vigente en noviembre sobre el producto 4: sirve para probar
-- RN-14 (409). Si se carga otra promo de noviembre con el producto 4,
-- tiene que fallar; con los productos 2 y 3 (ejemplo de Swagger) no.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO promociones (id, nombre, tipo, valor, cantidad_minima, fecha_inicio, fecha_fin, estado, usuario_interno_dni)
VALUES (1, 'Promo Clamshell', 'PORCENTAJE', 10, NULL, '2026-11-01', '2026-11-30', 'VIGENTE', '30111222');

INSERT IGNORE INTO promocion_producto (promociones_id, productos_codigo)
VALUES (1, 4);

-- ---------------------------------------------------------------------
-- PEDIDOS (CU-02 y CU-04)
-- Fechas fijas para poder probar los filtros por rango.
-- valor_total = suma de los subtotales de sus líneas (RN-26).
--
--   id  cliente   fecha        estado     entrega       total
--   1   Ana       2026-11-20   PAGADO     RETIRO_LOCAL   9000
--   2   Ana       2026-11-18   CANCELADO  ENVIO          4500
--   3   Ana       2026-10-05   ENTREGADO  ENVIO         34020  (precio viejo + descuento)
--   4   Ana       2026-09-12   ENTREGADO  RETIRO_LOCAL  13500  (descuento)
--   5   Ana       2026-11-25   ACEPTADO   ENVIO         24400  (2 productos)
--   6   Ana       2026-11-26   ENVIADO    ENVIO         12500
--   7   Juan      2026-10-20   PAGADO     RETIRO_LOCAL   6200
-- ---------------------------------------------------------------------
INSERT IGNORE INTO pedidos (id, fecha, estado, modo_entrega, direccion_entrega, valor_total, cliente_dni)
VALUES (1, '2026-11-20 10:00:00', 'PAGADO', 'RETIRO_LOCAL', NULL, 9000, '30111222'),
       (2, '2026-11-18 09:00:00', 'CANCELADO', 'ENVIO', 'Calle 123', 4500, '30111222'),
       (3, '2026-10-05 11:00:00', 'ENTREGADO', 'ENVIO', 'Av. San Martín 450', 34020, '30111222'),
       (4, '2026-09-12 15:00:00', 'ENTREGADO', 'RETIRO_LOCAL', NULL, 13500, '30111222'),
       (5, '2026-11-25 18:00:00', 'ACEPTADO', 'ENVIO', 'Calle 123', 24400, '30111222'),
       (6, '2026-11-26 08:00:00', 'ENVIADO', 'ENVIO', 'Calle 123', 12500, '30111222'),
       (7, '2026-10-20 12:00:00', 'PAGADO', 'RETIRO_LOCAL', NULL, 6200, '22333444');

-- los pedidos 1 y 2 antes se cargaban con NOW(): se les fija la fecha
UPDATE pedidos SET fecha = '2026-11-20 10:00:00' WHERE id = 1;
UPDATE pedidos SET fecha = '2026-11-18 09:00:00' WHERE id = 2;

UPDATE pedidos SET motivo_cancelacion = 'Pedido duplicado' WHERE id = 2;

-- ---------------------------------------------------------------------
-- DETALLE DE PEDIDOS (CU-04)
-- subtotal = cantidad x precio_unitario - descuento_aplicado (en pesos)
--   los precios son los del momento de la compra: no coinciden con el
--   precio actual del producto (RN-21). Ej: pedido 3, línea 3 se pagó a
--   6000 aunque hoy el producto 2 vale 6800
--   pedido 3, línea 4: 2 x 8900 - 1780 = 16020
--   pedido 4, línea 5: 5 x 3000 - 1500 = 13500
-- ---------------------------------------------------------------------
INSERT IGNORE INTO detalle_pedido (id, pedido_id, producto_codigo, cantidad, precio_unitario, descuento_aplicado)
VALUES (1, 1, 1, 2, 4500, 0),
       (2, 2, 1, 1, 4500, 0),
       (3, 3, 2, 3, 6000, 0),
       (4, 3, 3, 2, 8900, 1780),
       (5, 4, 4, 5, 3000, 1500),
       (6, 5, 1, 4, 4500, 0),
       (7, 5, 4, 2, 3200, 0),
       (8, 6, 5, 1, 12500, 0),
       (9, 7, 2, 1, 6200, 0);

-- ---------------------------------------------------------------------
-- HISTORIAL DE ESTADOS (CU-02)
-- Retiro en local no pasa por ENVIADO.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO historial_estado (id, fecha_hora, estado, pedido_id)
VALUES (1, '2026-11-20 10:00:00', 'ACEPTADO', 1),
       (2, '2026-11-20 10:30:00', 'PAGADO', 1),
       (3, '2026-11-18 09:00:00', 'ACEPTADO', 2),
       (4, '2026-11-18 12:00:00', 'CANCELADO', 2),
       (5, '2026-10-05 11:00:00', 'ACEPTADO', 3),
       (6, '2026-10-05 11:20:00', 'PAGADO', 3),
       (7, '2026-10-06 09:00:00', 'ENVIADO', 3),
       (8, '2026-10-08 16:00:00', 'ENTREGADO', 3),
       (9, '2026-09-12 15:00:00', 'ACEPTADO', 4),
       (10, '2026-09-12 15:10:00', 'PAGADO', 4),
       (11, '2026-09-13 10:00:00', 'ENTREGADO', 4),
       (12, '2026-11-25 18:00:00', 'ACEPTADO', 5),
       (13, '2026-11-26 08:00:00', 'ACEPTADO', 6),
       (14, '2026-11-26 08:15:00', 'PAGADO', 6),
       (15, '2026-11-26 14:00:00', 'ENVIADO', 6),
       (16, '2026-10-20 12:00:00', 'ACEPTADO', 7),
       (17, '2026-10-20 12:05:00', 'PAGADO', 7);

-- los cambios de estado 1 a 4 antes se cargaban con NOW(): se les fija la fecha
UPDATE historial_estado SET fecha_hora = '2026-11-20 10:00:00' WHERE id = 1;
UPDATE historial_estado SET fecha_hora = '2026-11-20 10:30:00' WHERE id = 2;
UPDATE historial_estado SET fecha_hora = '2026-11-18 09:00:00' WHERE id = 3;
UPDATE historial_estado SET fecha_hora = '2026-11-18 12:00:00' WHERE id = 4;
