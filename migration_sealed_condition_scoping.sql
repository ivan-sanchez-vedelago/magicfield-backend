-- Migración de datos para agregar set/idioma/condición a productos sellados: scoping de
-- card_condition por tipo de producto (NM/LP/... solo para singles, NEW/USD solo para
-- sellados) y las 2 condiciones nuevas.
--
-- Correr DESPUÉS de que el backend arranque una vez con la columna nueva (Hibernate, con
-- ddl-auto: update, ya debería haber agregado card_condition.applicable_type -- este script
-- solo migra DATOS, no crea estructura).
--
-- Correr ANTES de que cualquiera cree/edite un sellado desde el admin con estos campos: si no,
-- el default "Nuevo" no encuentra ninguna condición todavía y el form arranca sin preselección
-- (no rompe, pero confunde). No es necesario que el deploy del admin/frontend sea atómico con
-- este script -- ver plan.

-- ============================================================
-- 1. Backfillear applicable_type en las condiciones existentes (todas de singles hoy)
-- ============================================================
UPDATE card_condition SET applicable_type = 'SIN'
WHERE short_name IN ('NM', 'LP', 'MP', 'HP', 'DMG');

-- ============================================================
-- 2. Semilla de las 2 condiciones nuevas, exclusivas de sellados
-- ============================================================
INSERT INTO card_condition (short_name, long_name, price_multiplier, applicable_type) VALUES
  ('NEW', 'Nuevo', 1.00, 'PSL'),
  ('USD', 'Usado', 1.00, 'PSL');
-- price_multiplier no se usa nunca en el pricing de sellados (siempre manual, nunca
-- convertUsdToArs) -- 1.00 es un placeholder inerte, solo para satisfacer la columna NOT NULL.

-- ============================================================
-- 3. Chequeo manual: no debería quedar ninguna fila sin applicable_type
-- ============================================================
SELECT id, short_name FROM card_condition WHERE applicable_type IS NULL;
