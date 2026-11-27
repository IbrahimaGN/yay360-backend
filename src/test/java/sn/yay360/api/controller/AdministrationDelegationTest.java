package sn.yay360.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import sn.yay360.api.dto.AffecterProfessionnelRequest;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.dto.EtablissementRequest;
import sn.yay360.api.helper.EtablissementHelper;
import sn.yay360.api.helper.UtilisateurHelper;
import sn.yay360.api.service.impl.EtablissementServiceImpl;
import sn.yay360.api.service.impl.UtilisateurServiceImpl;

import static org.mockito.Mockito.*;

/**
 * Les points d'entrée d'administration transmettent l'auteur connecté jusqu'aux helpers, qui décident
 * du périmètre. Les droits eux-mêmes sont vérifiés par SecuriteIntegrationIT.
 */
class AdministrationDelegationTest {

    private static final String AUTEUR = "admin.hpd@yay360.sn";

    private UtilisateurHelper utilisateurHelper;
    private EtablissementHelper etablissementHelper;
    private UtilisateurController utilisateurController;
    private EtablissementController etablissementController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        utilisateurHelper = mock(UtilisateurHelper.class);
        etablissementHelper = mock(EtablissementHelper.class);
        utilisateurController = new UtilisateurController(new UtilisateurServiceImpl(utilisateurHelper));
        etablissementController = new EtablissementController(new EtablissementServiceImpl(etablissementHelper));
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(AUTEUR);
    }

    @Test
    void lesComptesSontGeresAuNomDeLAuteurConnecte() {
        CreerProfessionnelRequest request = new CreerProfessionnelRequest();

        utilisateurController.creerProfessionnel(authentication, request);
        utilisateurController.soignantsDeMonEtablissement(authentication);
        utilisateurController.desactiver(5L, authentication);
        utilisateurController.activer(5L, authentication);

        verify(utilisateurHelper).creerProfessionnel(AUTEUR, request);
        verify(utilisateurHelper).listerSoignantsDeMonEtablissement(AUTEUR);
        verify(utilisateurHelper).desactiverCompte(5L, AUTEUR);
        verify(utilisateurHelper).activerCompte(5L, AUTEUR);
    }

    @Test
    void lesEtablissementsSontGeresAuNomDeLAuteurConnecte() {
        EtablissementRequest coordonnees = new EtablissementRequest();
        AffecterProfessionnelRequest affectation = new AffecterProfessionnelRequest();

        etablissementController.monEtablissement(authentication);
        etablissementController.mettreAJour(authentication, 1L, coordonnees);
        etablissementController.affecterProfessionnel(authentication, 1L, affectation);
        etablissementController.retirerProfessionnel(authentication, 1L, 7L);

        verify(etablissementHelper).monEtablissement(AUTEUR);
        verify(etablissementHelper).mettreAJour(AUTEUR, 1L, coordonnees);
        verify(etablissementHelper).affecterProfessionnel(AUTEUR, 1L, affectation);
        verify(etablissementHelper).retirerProfessionnel(AUTEUR, 1L, 7L);
    }
}
