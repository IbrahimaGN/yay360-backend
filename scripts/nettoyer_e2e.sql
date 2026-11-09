-- Supprime les alertes créées par scenario_e2e.py et les notifications émises pendant leur traitement.
BEGIN;
WITH t AS (
  SELECT grossesse_id, min(date_declaration) AS debut, max(coalesce(date_resolution, date_declaration)) + interval '2 seconds' AS fin
  FROM alertes_medicales WHERE description = 'Scénario de test e2e (données fictives)' GROUP BY grossesse_id)
DELETE FROM notifications n USING t
 WHERE n.grossesse_id = t.grossesse_id AND n.date_envoi BETWEEN t.debut AND t.fin;
DELETE FROM alertes_medicales WHERE description = 'Scénario de test e2e (données fictives)';
COMMIT;
