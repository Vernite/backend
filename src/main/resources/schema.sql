DROP PROCEDURE IF EXISTS counter_increment ^;

CREATE PROCEDURE counter_increment (IN counter_id int, OUT result int)
BEGIN
START TRANSACTION;
UPDATE counter_sequence cs SET cs.counter_value = cs.counter_value + 1 WHERE cs.id = counter_id;
SELECT counter_value INTO result from counter_sequence WHERE id = counter_id;
COMMIT;
END ^;