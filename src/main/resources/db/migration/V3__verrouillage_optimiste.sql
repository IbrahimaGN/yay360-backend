-- YAY360 — Verrouillage optimiste (@Version) sur les données modifiées par plusieurs soignants.
-- Deux prises en charge simultanées d'une même alerte : la seconde reçoit un 409 au lieu d'écraser la première.

ALTER TABLE alertes_medicales ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE rendez_vous ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
