package sn.yay360.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.yay360.api.model.CodeVerification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CodeVerificationRepository extends JpaRepository<CodeVerification, Long> {

    Optional<CodeVerification> findFirstByTelephoneOrderByDateEnvoiDesc(String telephone);

    Optional<CodeVerification> findFirstByTelephoneAndUtiliseFalseOrderByDateEnvoiDesc(String telephone);

    List<CodeVerification> findByTelephoneAndUtiliseFalse(String telephone);

    long countByTelephoneAndDateEnvoiAfter(String telephone, LocalDateTime depuis);

    Optional<CodeVerification> findByJetonPinHache(String jetonPinHache);
}
