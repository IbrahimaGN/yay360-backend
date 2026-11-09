-- Remet le compte de démonstration utilisé par parcours_pin.py dans son état initial (sans PIN).
-- Usage : docker exec -i yay360-db psql -U postgres -d yay360 -v tel=+221770010013 < nettoyer_parcours_pin.sql
BEGIN;
DELETE FROM jetons_rafraichissement WHERE utilisateur_id IN (SELECT id FROM utilisateurs WHERE telephone = :'tel');
DELETE FROM codes_verification WHERE telephone = :'tel';
UPDATE utilisateurs SET pin = NULL, pin_echecs = 0, pin_verrouillages = 0, pin_verrouille_jusqua = NULL
 WHERE telephone = :'tel';
COMMIT;
