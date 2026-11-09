-- YAY360 — Constantes prénatales complémentaires (expression des besoins : entretien avec une sage-femme).
-- Fréquence cardiaque maternelle, protéinurie et glycosurie (bandelette urinaire).
-- Colonnes facultatives : une image antérieure les ignore, le retour arrière reste possible.

ALTER TABLE consultations ADD COLUMN IF NOT EXISTS frequence_cardiaque_maternelle integer;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS proteinurie varchar(12);
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS glycosurie varchar(12);

ALTER TABLE consultations ADD CONSTRAINT ck_consultations_proteinurie
    CHECK (proteinurie IS NULL OR proteinurie IN ('NEGATIVE', 'TRACES', 'UNE_CROIX', 'DEUX_CROIX', 'TROIS_CROIX'));
ALTER TABLE consultations ADD CONSTRAINT ck_consultations_glycosurie
    CHECK (glycosurie IS NULL OR glycosurie IN ('NEGATIVE', 'TRACES', 'UNE_CROIX', 'DEUX_CROIX', 'TROIS_CROIX'));
