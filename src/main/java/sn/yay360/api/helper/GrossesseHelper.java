package sn.yay360.api.helper;

import sn.yay360.api.dto.ChoisirEtablissementRequest;
import sn.yay360.api.dto.DeclarerGrossesseRequest;
import sn.yay360.api.dto.DeclarerRisqueRequest;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.dto.PatienteRechercheDto;
import sn.yay360.api.dto.RattacherGrossesseRequest;
import sn.yay360.api.dto.TerminerGrossesseRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.GrossesseMapper;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.TypeNotification;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Contient toute la logique métier liée à la déclaration et au suivi des grossesses (§4.2).
 */
@Component
@RequiredArgsConstructor
public class GrossesseHelper {

    /** Au-delà, la date des dernières règles est incohérente avec une grossesse en cours. */
    static final int SEMAINES_MAX_DECLARATION = 42;

    private final GrossesseRepository grossesseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementSanteRepository etablissementRepository;
    private final sn.yay360.api.repo.RendezVousRepository rendezVousRepository;
    private final GrossesseMapper grossesseMapper;
    private final CalculObstetricalHelper calcul;
    private final DossierFamilialHelper dossierFamilialHelper;
    private final AccesDossierHelper acces;
    private final NotificationHelper notificationHelper;

    // ==================== PATIENTE ====================

    /** Seule la femme enceinte déclare sa grossesse (le conjoint suit le dossier). */
    @PreAuthorize("hasAuthority('FEMME_ENCEINTE')")
    @Transactional
    public GrossesseDto declarer(String emailFemme, DeclarerGrossesseRequest request) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);

        grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS)
                .ifPresent(g -> {
                    throw new OperationNonAutoriseeException("Une grossesse en cours existe déjà pour cette utilisatrice");
                });

        if (request.getDateDernieresRegles().isBefore(LocalDate.now().minusWeeks(SEMAINES_MAX_DECLARATION))) {
            throw new IllegalArgumentException(
                    "La date des dernières règles semble trop ancienne (plus de " + SEMAINES_MAX_DECLARATION + " semaines)");
        }

        Grossesse grossesse = new Grossesse();
        grossesse.setFemmeEnceinte(femme);
        grossesse.setDateDernieresRegles(request.getDateDernieresRegles());
        grossesse.setDatePrevueAccouchement(calcul.calculerDatePrevueAccouchement(request.getDateDernieresRegles()));
        grossesse.setNombreGrossessesPrecedentes(
                request.getNombreGrossessesPrecedentes() != null ? request.getNombreGrossessesPrecedentes() : 0);
        grossesse.setAntecedents(request.getAntecedents());
        grossesse.setStatut(StatutGrossesse.EN_COURS);
        grossesse.setGrossesseARisque(false);
        if (request.getEtablissementId() != null) {
            grossesse.setEtablissementSuivi(trouverEtablissementActif(request.getEtablissementId()));
        }

        return versDtoCalcule(grossesseRepository.save(grossesse));
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional(readOnly = true)
    public GrossesseDto obtenirGrossesseActive(String emailFemme) {
        return versDtoCalcule(trouverActiveParEmail(emailFemme));
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional(readOnly = true)
    public List<GrossesseDto> obtenirHistorique(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return grossesseRepository.findByFemmeEnceinteIdOrderByDateCreationDesc(femme.getId()).stream()
                .map(this::versDtoCalcule)
                .toList();
    }

    /** La patiente choisit (ou change) l'établissement qui assure son suivi. */
    @PreAuthorize("hasAuthority('FEMME_ENCEINTE')")
    @Transactional
    public GrossesseDto choisirEtablissement(String emailFemme, ChoisirEtablissementRequest request) {
        Grossesse grossesse = trouverActiveParEmail(emailFemme);
        grossesse.setEtablissementSuivi(trouverEtablissementActif(request.getEtablissementId()));
        return versDtoCalcule(grossesseRepository.save(grossesse));
    }

    // ==================== PROFESSIONNELS ====================

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    @Transactional(readOnly = true)
    public GrossesseDto obtenirParId(String email, Long id) {
        Grossesse grossesse = trouverParId(id);
        acces.verifierLecture(acces.utilisateur(email), grossesse);
        return versDtoCalcule(grossesse);
    }

    /** Admin : toutes les grossesses en cours (supervision). Soignant : celles qu'il suit. */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<GrossesseDto> listerActives(String email) {
        return grossessesVisibles(acces.utilisateur(email)).stream()
                .map(this::versDtoCalcule)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<GrossesseDto> listerARisque(String email) {
        Utilisateur utilisateur = acces.utilisateur(email);
        List<Grossesse> grossesses = AccesDossierHelper.estAdmin(utilisateur)
                ? grossesseRepository.findByStatutAndGrossesseARisqueTrue(StatutGrossesse.EN_COURS)
                : grossesseRepository.findSuiviesParProfessionnel(utilisateur.getId(), StatutGrossesse.EN_COURS)
                        .stream().filter(g -> Boolean.TRUE.equals(g.getGrossesseARisque())).toList();
        return grossesses.stream().map(this::versDtoCalcule).toList();
    }

    /**
     * Recherche exacte d'une patiente (téléphone ou email) pour la prise en charge d'une
     * patiente qui se présente. Ne renvoie aucune donnée clinique.
     */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional(readOnly = true)
    public List<PatienteRechercheDto> rechercherPatiente(String emailProfessionnel, String telephone, String email) {
        boolean parTelephone = telephone != null && !telephone.isBlank();
        boolean parEmail = email != null && !email.isBlank();
        if (parTelephone == parEmail) {
            throw new IllegalArgumentException("Indiquez soit un téléphone, soit un email (recherche exacte)");
        }
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        List<Utilisateur> femmes = new ArrayList<>();
        if (parTelephone) {
            femmes.addAll(utilisateurRepository.findByTelephoneAndRole(telephone.trim(), RoleUtilisateur.FEMME_ENCEINTE));
        } else {
            utilisateurRepository.findByEmailIgnoreCase(AuthHelper.normaliserEmail(email))
                    .filter(u -> RoleUtilisateur.FEMME_ENCEINTE.equals(u.getRole()))
                    .ifPresent(femmes::add);
        }
        List<PatienteRechercheDto> resultats = new ArrayList<>();
        for (Utilisateur femme : femmes) {
            grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS)
                    .ifPresent(g -> resultats.add(new PatienteRechercheDto(
                            g.getId(), femme.getPrenom(), femme.getNom(),
                            calcul.calculerSemaineActuelle(g.getDateDernieresRegles()),
                            g.getEtablissementSuivi() != null ? g.getEtablissementSuivi().getNom() : null,
                            acces.suit(professionnel, g))));
        }
        return resultats;
    }

    /**
     * Rattache une grossesse à un établissement dont le soignant est membre. Refusé si la
     * grossesse est déjà suivie par un autre établissement (seule la patiente peut en changer).
     */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public GrossesseDto rattacher(String emailProfessionnel, Long id, RattacherGrossesseRequest request) {
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        Grossesse grossesse = trouverParId(id);
        verifierEnCours(grossesse);
        if (!acces.estMembre(professionnel, request.getEtablissementId())) {
            throw new OperationNonAutoriseeException("Vous n'êtes pas membre de cet établissement");
        }
        // Preuve de présence : le téléphone fourni doit être celui de la patiente
        String telephone = grossesse.getFemmeEnceinte().getTelephone();
        if (telephone == null || !telephone.equals(request.getTelephonePatiente().trim())) {
            throw new OperationNonAutoriseeException("Le téléphone indiqué ne correspond pas à cette patiente");
        }
        // Seul un soignant qui la suit déjà peut changer l'établissement d'une grossesse suivie ;
        // sinon, la grossesse doit n'être suivie par aucun soignant actif.
        if (!acces.suit(professionnel, grossesse) && !acces.estSansSuivi(grossesse)) {
            throw new OperationNonAutoriseeException(
                    "Cette grossesse est déjà suivie par une autre équipe. La patiente peut changer d'établissement.");
        }
        grossesse.setEtablissementSuivi(trouverEtablissementActif(request.getEtablissementId()));
        Grossesse enregistree = grossesseRepository.save(grossesse);
        notificationHelper.envoyer(grossesse.getFemmeEnceinte(), grossesse, TypeNotification.INFORMATION,
                "Votre suivi est assuré par " + enregistree.getEtablissementSuivi().getNom(),
                "Votre dossier de grossesse est désormais suivi par l'établissement "
                        + enregistree.getEtablissementSuivi().getNom() + ".");
        return versDtoCalcule(enregistree);
    }

    @PreAuthorize("hasAuthority('MEDECIN')")
    @Transactional
    public GrossesseDto declarerARisque(String emailMedecin, Long id, DeclarerRisqueRequest request) {
        Grossesse grossesse = trouverParId(id);
        acces.verifierSoin(acces.utilisateur(emailMedecin), grossesse);
        verifierEnCours(grossesse);
        grossesse.setGrossesseARisque(true);
        grossesse.setMotifRisque(request.getMotif());
        Grossesse enregistree = grossesseRepository.save(grossesse);
        notificationHelper.envoyer(grossesse.getFemmeEnceinte(), grossesse, TypeNotification.INFORMATION,
                "Suivi renforcé de votre grossesse",
                "Votre médecin a mis en place un suivi renforcé. Respectez bien vos rendez-vous "
                        + "et signalez rapidement tout symptôme inhabituel.");
        return versDtoCalcule(enregistree);
    }

    @PreAuthorize("hasAuthority('MEDECIN')")
    @Transactional
    public GrossesseDto retirerRisque(String emailMedecin, Long id) {
        Grossesse grossesse = trouverParId(id);
        acces.verifierSoin(acces.utilisateur(emailMedecin), grossesse);
        verifierEnCours(grossesse);
        grossesse.setGrossesseARisque(false);
        grossesse.setMotifRisque(null);
        return versDtoCalcule(grossesseRepository.save(grossesse));
    }

    /** Clôture réservée au médecin (cahier des charges §3). */
    @PreAuthorize("hasAuthority('MEDECIN')")
    @Transactional
    public GrossesseDto terminer(String emailMedecin, Long id, TerminerGrossesseRequest request) {
        Grossesse grossesse = trouverParId(id);
        acces.verifierSoin(acces.utilisateur(emailMedecin), grossesse);
        verifierEnCours(grossesse);

        LocalDate dateFin = request.getDateFin() != null ? request.getDateFin() : LocalDate.now();
        if (dateFin.isBefore(grossesse.getDateDernieresRegles())) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date des dernières règles");
        }
        grossesse.setStatut(request.getStatut());
        grossesse.setDateFin(dateFin);
        // Les rendez-vous encore prévus n'ont plus lieu d'être : ni rappel, ni consultation
        rendezVousRepository.findByGrossesseIdOrderByDateHeureDesc(grossesse.getId()).stream()
                .filter(r -> RendezVousHelper.STATUTS_ACTIFS.contains(r.getStatut()))
                .forEach(r -> {
                    r.setStatut(sn.yay360.api.model.StatutRendezVous.ANNULE);
                    r.setNotesAnnulation("Grossesse clôturée le " + dateFin);
                    rendezVousRepository.save(r);
                });
        return versDtoCalcule(grossesseRepository.save(grossesse));
    }

    // --- Méthodes internes utilisées aussi par les autres helpers ---

    List<Grossesse> grossessesVisibles(Utilisateur utilisateur) {
        return AccesDossierHelper.estAdmin(utilisateur)
                ? grossesseRepository.findByStatut(StatutGrossesse.EN_COURS)
                : grossesseRepository.findSuiviesParProfessionnel(utilisateur.getId(), StatutGrossesse.EN_COURS);
    }

    Grossesse trouverActiveParEmail(String email) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(email);
        return grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune grossesse en cours trouvée"));
    }

    Grossesse trouverParId(Long id) {
        return grossesseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grossesse introuvable avec l'id : " + id));
    }

    static void verifierEnCours(Grossesse grossesse) {
        if (!StatutGrossesse.EN_COURS.equals(grossesse.getStatut())) {
            throw new OperationNonAutoriseeException("Cette grossesse est clôturée (statut : " + grossesse.getStatut() + ")");
        }
    }

    EtablissementSante trouverEtablissementActif(Long id) {
        EtablissementSante etablissement = etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + id));
        if (!Boolean.TRUE.equals(etablissement.getActif())) {
            throw new OperationNonAutoriseeException("Cet établissement n'est plus actif");
        }
        return etablissement;
    }

    GrossesseDto versDtoCalcule(Grossesse grossesse) {
        int ageJours = calcul.calculerAgeGestationnelJours(grossesse.getDateDernieresRegles());
        int semaine = calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles());
        String trimestre = calcul.calculerTrimestre(semaine);
        int joursRestants = calcul.calculerJoursRestants(grossesse.getDatePrevueAccouchement());
        return grossesseMapper.versDto(grossesse, ageJours, semaine, trimestre, joursRestants);
    }
}
