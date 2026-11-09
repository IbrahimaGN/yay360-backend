package sn.yay360.api.helper;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regroupe tous les calculs obstétricaux automatiques du système intelligent :
 * - Date prévue d'accouchement (DPA = date des dernières règles + 280 jours / règle de Naegele) : une ESTIMATION
 * - Période d'accouchement à terme : de 37 SA + 0 j à 41 SA + 6 j (ACOG, avis n° 579 ; CIM-10 : 37 à moins de 42 SA)
 * - Âge gestationnel en jours
 * - Semaine de grossesse actuelle
 * - Trimestre correspondant
 * - Jours restants avant l'accouchement
 *
 * Centralisé ici pour être réutilisé par GrossesseHelper, CalendrierHelper, SuiviHelper.
 */
@Component
public class CalculObstetricalHelper {

    public static final int DUREE_GROSSESSE_SEMAINES = 40;
    private static final int DUREE_GROSSESSE_JOURS = 280; // 40 semaines, règle de Naegele

    public int dureeGrossesseSemaines() {
        return DUREE_GROSSESSE_SEMAINES;
    }

    public LocalDate calculerDatePrevueAccouchement(LocalDate dateDernieresRegles) {
        return dateDernieresRegles.plusDays(DUREE_GROSSESSE_JOURS);
    }

    /** 37 SA + 0 j : début de la période à terme (avant : accouchement prématuré). */
    static final int DEBUT_TERME_JOURS = 37 * 7;
    /** 41 SA + 6 j : fin de la période à terme (à partir de 42 SA : grossesse prolongée). */
    static final int FIN_TERME_JOURS = 41 * 7 + 6;

    /** Premier jour de la période d'accouchement à terme, ou null sans date des dernières règles. */
    public static LocalDate debutPeriodeTerme(LocalDate dateDernieresRegles) {
        return dateDernieresRegles == null ? null : dateDernieresRegles.plusDays(DEBUT_TERME_JOURS);
    }

    /** Dernier jour de la période d'accouchement à terme, ou null sans date des dernières règles. */
    public static LocalDate finPeriodeTerme(LocalDate dateDernieresRegles) {
        return dateDernieresRegles == null ? null : dateDernieresRegles.plusDays(FIN_TERME_JOURS);
    }

    public int calculerAgeGestationnelJours(LocalDate dateDernieresRegles) {
        int jours = (int) ChronoUnit.DAYS.between(dateDernieresRegles, LocalDate.now());
        return Math.max(jours, 0);
    }

    public int calculerSemaineActuelle(LocalDate dateDernieresRegles) {
        int ageJours = calculerAgeGestationnelJours(dateDernieresRegles);
        int semaine = (ageJours / 7) + 1;
        return Math.min(semaine, DUREE_GROSSESSE_SEMAINES);
    }

    public String calculerTrimestre(int semaine) {
        if (semaine <= 13) {
            return "T1";
        } else if (semaine <= 27) {
            return "T2";
        } else {
            return "T3";
        }
    }

    public int calculerJoursRestants(LocalDate datePrevueAccouchement) {
        int jours = (int) ChronoUnit.DAYS.between(LocalDate.now(), datePrevueAccouchement);
        return Math.max(jours, 0);
    }
}
