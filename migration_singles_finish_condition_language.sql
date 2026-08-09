-- Migración de datos para el rediseño de singles: finish/condición/idioma normalizados
-- (las tres como tablas propias con short_name/long_name), constraint único de dedup.
--
-- Correr DESPUÉS de que el backend arranque una vez con las entidades nuevas (Hibernate,
-- con ddl-auto: update, ya debería haber creado las tablas card_condition/card_language/
-- card_finish y las columnas finish_id/condition_id/language_id en products -- este script
-- solo migra DATOS, no crea estructura).
--
-- Ejecutar en este orden. Probar primero contra una copia de la base, no directo en producción.

-- ============================================================
-- 1. Semilla de condiciones (orden = orden de calidad, de mejor a peor)
-- ============================================================
INSERT INTO card_condition (short_name, long_name, price_multiplier) VALUES
  ('NM', 'Near Mint', 1.00),
  ('LP', 'Lightly Played', 0.90),
  ('MP', 'Moderately Played', 0.75),
  ('HP', 'Heavily Played', 0.60),
  ('DMG', 'Damaged', 0.40);

-- ============================================================
-- 2. Semilla de idiomas (mismo mapa que ya usaba el importador de CSV)
-- ============================================================
INSERT INTO card_language (short_name, long_name) VALUES
  ('en', 'English'),
  ('es', 'Spanish'),
  ('fr', 'French'),
  ('de', 'German'),
  ('it', 'Italian'),
  ('pt', 'Portuguese'),
  ('ja', 'Japanese'),
  ('ko', 'Korean'),
  ('ru', 'Russian'),
  ('zhs', 'Simplified Chinese'),
  ('zht', 'Traditional Chinese');

-- ============================================================
-- 3. Semilla de finishes (short_name = lo que ya usa el código, en mayúsculas)
-- ============================================================
INSERT INTO card_finish (short_name, long_name) VALUES
  ('NONFOIL', 'Normal'),
  ('FOIL', 'Foil'),
  ('ETCHED', 'Etched'),
  ('GLOSSY', 'Glossy');

-- ============================================================
-- 4. Backfill de finish_id desde is_foil (determinístico, sin ambigüedad:
--    no existían etched/glossy antes de este cambio)
-- ============================================================
UPDATE products SET finish_id = (SELECT id FROM card_finish WHERE short_name = 'FOIL')
WHERE is_foil = true AND category_id = (SELECT id FROM categories WHERE short_name = 'SIN');

UPDATE products SET finish_id = (SELECT id FROM card_finish WHERE short_name = 'NONFOIL')
WHERE (is_foil = false OR is_foil IS NULL)
  AND category_id = (SELECT id FROM categories WHERE short_name = 'SIN');

-- ============================================================
-- 5. Backfill de condition_id (cubre las variantes de texto más comunes)
-- ============================================================
UPDATE products p SET condition_id = cc.id
FROM card_condition cc
WHERE p.category_id = (SELECT id FROM categories WHERE short_name = 'SIN')
  AND (
    (UPPER(TRIM(p.condition)) IN ('NM', 'NEAR MINT') AND cc.short_name = 'NM') OR
    (UPPER(TRIM(p.condition)) IN ('LP', 'LIGHTLY PLAYED') AND cc.short_name = 'LP') OR
    (UPPER(TRIM(p.condition)) IN ('MP', 'MODERATELY PLAYED') AND cc.short_name = 'MP') OR
    (UPPER(TRIM(p.condition)) IN ('HP', 'HEAVILY PLAYED') AND cc.short_name = 'HP') OR
    (UPPER(TRIM(p.condition)) IN ('DMG', 'DAMAGED') AND cc.short_name = 'DMG')
  );

-- Revisar a mano qué escribieron en las filas que no matchearon ninguna variante conocida:
SELECT id, name, condition FROM products
WHERE category_id = (SELECT id FROM categories WHERE short_name = 'SIN') AND condition_id IS NULL;

-- Si aparecen filas arriba, asignalas a mano, por ejemplo:
-- UPDATE products SET condition_id = (SELECT id FROM card_condition WHERE short_name = 'NM')
-- WHERE id = '<uuid-de-la-fila>';

-- ============================================================
-- 6. Backfill de language_id (por código de 2-3 letras o nombre completo en inglés)
-- ============================================================
UPDATE products p SET language_id = cl.id
FROM card_language cl
WHERE p.category_id = (SELECT id FROM categories WHERE short_name = 'SIN')
  AND (UPPER(p.language) = UPPER(cl.short_name) OR UPPER(p.language) = UPPER(cl.long_name));

-- Revisar a mano qué escribieron en las filas que no matchearon ningún idioma conocido:
SELECT id, name, language FROM products
WHERE category_id = (SELECT id FROM categories WHERE short_name = 'SIN') AND language_id IS NULL;

-- ============================================================
-- 7. Antes de poner el constraint único: detectar duplicados existentes
-- ============================================================
SELECT scryfall_id, finish_id, condition_id, language_id, COUNT(*), SUM(stock) AS stock_total
FROM products
WHERE category_id = (SELECT id FROM categories WHERE short_name = 'SIN')
GROUP BY scryfall_id, finish_id, condition_id, language_id
HAVING COUNT(*) > 1;

-- ============================================================
-- 8. Fusionar duplicados (solo si el paso 7 devolvió filas): suma el stock en la fila
--    más vieja (menor id) de cada grupo duplicado y borra el resto.
-- ============================================================
WITH dupes AS (
  SELECT id, scryfall_id, finish_id, condition_id, language_id, stock,
         ROW_NUMBER() OVER (PARTITION BY scryfall_id, finish_id, condition_id, language_id ORDER BY id) AS rn,
         FIRST_VALUE(id) OVER (PARTITION BY scryfall_id, finish_id, condition_id, language_id ORDER BY id) AS keep_id
  FROM products
  WHERE category_id = (SELECT id FROM categories WHERE short_name = 'SIN')
)
UPDATE products SET stock = products.stock + d.stock
FROM dupes d WHERE d.rn > 1 AND products.id = d.keep_id;

DELETE FROM products WHERE id IN (
  SELECT id FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY scryfall_id, finish_id, condition_id, language_id ORDER BY id) AS rn
    FROM products WHERE category_id = (SELECT id FROM categories WHERE short_name = 'SIN')
  ) x WHERE x.rn > 1
);

-- Volver a correr el SELECT del paso 7: tiene que devolver 0 filas antes de seguir.

-- ============================================================
-- 9. Constraint único (recién acá, con los datos ya limpios)
-- ============================================================
ALTER TABLE products ADD CONSTRAINT uq_product_single_variant
  UNIQUE (scryfall_id, finish_id, condition_id, language_id);

-- ============================================================
-- 10. Limpieza final -- NO correr en el mismo deploy que agrega las columnas nuevas.
--     Ejecutar en un deploy posterior, una vez confirmado que todo funciona bien en
--     producción con finish_id/condition_id/language_id (deja ventana de rollback).
--     Si en algún momento se deployó una versión intermedia donde `finish` era una
--     columna de texto (antes de pasar a tabla propia), agregá también su DROP acá.
-- ============================================================
-- ALTER TABLE products DROP COLUMN is_foil;
-- ALTER TABLE products DROP COLUMN condition;
-- ALTER TABLE products DROP COLUMN language;
-- ALTER TABLE products DROP COLUMN finish; -- solo si existió como columna de texto
