-- YAY360 — Issue de la grossesse saisie à la clôture (accouchement, fausse couche, GEU, mort fœtale, IMG).
-- Aucune valeur « avortement provoqué » : délit au Sénégal (Code pénal, art. 305), ne pas exposer la patiente.
-- Colonne facultative : les grossesses closes avant cette migration gardent NULL (issue non renseignée).

ALTER TABLE grossesses ADD COLUMN IF NOT EXISTS issue varchar(30);

ALTER TABLE grossesses ADD CONSTRAINT ck_grossesses_issue CHECK (issue IS NULL OR issue IN (
    'NAISSANCE_VIVANTE', 'MORT_NE', 'FAUSSE_COUCHE', 'GROSSESSE_EXTRA_UTERINE',
    'MORT_FOETALE_IN_UTERO', 'INTERRUPTION_THERAPEUTIQUE'));
