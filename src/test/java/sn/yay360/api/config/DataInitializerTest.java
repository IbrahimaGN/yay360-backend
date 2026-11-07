package sn.yay360.api.config;

import sn.yay360.api.helper.CalculObstetricalHelper;
import sn.yay360.api.model.EtapeGrossesse;
import sn.yay360.api.repo.ConseilGrossesseRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.EtapeGrossesseRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    private EtapeGrossesseRepository etapeGrossesseRepository;
    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        etapeGrossesseRepository = mock(EtapeGrossesseRepository.class);
        initializer = new DataInitializer(
                mock(UtilisateurRepository.class),
                etapeGrossesseRepository,
                mock(ConseilGrossesseRepository.class),
                mock(EtablissementSanteRepository.class),
                mock(PasswordEncoder.class),
                new CalculObstetricalHelper(),
                mock(JeuDeDonneesDemo.class)
        );
    }

    @Test
    void seedEtapesGrossesseInitialiseQuaranteSemaines() {
        ArgumentCaptor<EtapeGrossesse> captor = ArgumentCaptor.forClass(EtapeGrossesse.class);

        initializer.seedEtapesGrossesse();

        verify(etapeGrossesseRepository, times(40)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(EtapeGrossesse::getSemaine)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 40).boxed().toList());
    }
}
