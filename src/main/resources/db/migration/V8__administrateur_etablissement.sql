-- YAY360 — Administrateur d'établissement.
-- Un compte ADMIN_ETABLISSEMENT gère les soignants d'un seul établissement : la colonne le rattache
-- à cet établissement, et la contrainte garantit qu'elle n'est remplie que pour ce rôle.
-- Migration additive : aucune donnée existante n'est modifiée.

ALTER TABLE utilisateurs ADD COLUMN IF NOT EXISTS etablissement_administre_id bigint
    REFERENCES etablissements_sante (id);

CREATE INDEX IF NOT EXISTS ix_utilisateurs_etablissement_administre
    ON utilisateurs (etablissement_administre_id);

ALTER TABLE utilisateurs ADD CONSTRAINT ck_utilisateurs_etablissement_administre
    CHECK ((role = 'ADMIN_ETABLISSEMENT') = (etablissement_administre_id IS NOT NULL));
