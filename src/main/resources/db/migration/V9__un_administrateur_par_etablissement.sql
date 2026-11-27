-- YAY360 — Un seul administrateur par établissement.
-- Index unique partiel : deux comptes ne peuvent pas administrer le même établissement.
-- Migration additive : aucune donnée n'est modifiée (elle échoue si un établissement a déjà deux administrateurs).

CREATE UNIQUE INDEX IF NOT EXISTS ux_utilisateurs_etablissement_administre
    ON utilisateurs (etablissement_administre_id)
    WHERE etablissement_administre_id IS NOT NULL;
