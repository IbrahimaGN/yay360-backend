-- YAY360 — Traçabilité de la résolution d'une alerte.
-- Le soignant qui résout une alerte n'est pas forcément celui qui l'a prise en charge
-- (ex. : prise en charge par la sage-femme, résolution par le médecin).
-- Les alertes résolues avant cette migration gardent une valeur NULL : leur auteur n'a pas été enregistré
-- et n'est pas déduit, pour ne pas attribuer une décision clinique à la mauvaise personne.

ALTER TABLE alertes_medicales ADD COLUMN IF NOT EXISTS resolu_par_id bigint;

ALTER TABLE alertes_medicales
    ADD CONSTRAINT fk_alertes_medicales_resolu_par FOREIGN KEY (resolu_par_id) REFERENCES utilisateurs (id);
