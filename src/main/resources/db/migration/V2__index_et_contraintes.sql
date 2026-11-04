-- YAY360 — Index de performance et contraintes d'intégrité métier.

-- Une seule grossesse EN_COURS par femme enceinte, garantie par la base de données :
-- protège contre le double clic ou deux déclarations simultanées (femme et conjoint).
CREATE UNIQUE INDEX IF NOT EXISTS ux_grossesse_en_cours_par_femme
    ON grossesses (femme_enceinte_id)
    WHERE statut = 'EN_COURS';

-- Index sur les colonnes de recherche et de jointure les plus utilisées (§5.1 « modèle indexé »).
CREATE INDEX IF NOT EXISTS ix_grossesses_statut ON grossesses (statut);
CREATE INDEX IF NOT EXISTS ix_grossesses_etablissement_suivi ON grossesses (etablissement_suivi_id);

CREATE INDEX IF NOT EXISTS ix_rendez_vous_grossesse ON rendez_vous (grossesse_id);
CREATE INDEX IF NOT EXISTS ix_rendez_vous_femme_date ON rendez_vous (femme_enceinte_id, date_heure);
CREATE INDEX IF NOT EXISTS ix_rendez_vous_professionnel_date ON rendez_vous (professionnel_id, date_heure);
CREATE INDEX IF NOT EXISTS ix_rendez_vous_statut_date ON rendez_vous (statut, date_heure);

CREATE INDEX IF NOT EXISTS ix_consultations_grossesse_date ON consultations (grossesse_id, date_consultation);
CREATE INDEX IF NOT EXISTS ix_consultations_professionnel ON consultations (professionnel_id);

CREATE INDEX IF NOT EXISTS ix_alertes_grossesse_date ON alertes_medicales (grossesse_id, date_declaration);
CREATE INDEX IF NOT EXISTS ix_alertes_statut ON alertes_medicales (statut);
CREATE INDEX IF NOT EXISTS ix_alertes_professionnel ON alertes_medicales (professionnel_id);

CREATE INDEX IF NOT EXISTS ix_notifications_destinataire_lue ON notifications (destinataire_id, lue);

CREATE INDEX IF NOT EXISTS ix_utilisateurs_role ON utilisateurs (role);
CREATE INDEX IF NOT EXISTS ix_utilisateurs_telephone ON utilisateurs (telephone);
CREATE UNIQUE INDEX IF NOT EXISTS ux_utilisateurs_email_minuscule ON utilisateurs (lower(email));
