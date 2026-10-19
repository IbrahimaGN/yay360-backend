package sn.yay360.api.repo;

import sn.yay360.api.model.InvitationConjoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDateTime;

public interface InvitationConjointRepository extends JpaRepository<InvitationConjoint, Long> {

    Optional<InvitationConjoint> findByTokenAndAccepteeFalse(String token);

    boolean existsByEmailIgnoreCaseAndAccepteeFalseAndDateExpirationAfter(String email, LocalDateTime maintenant);
}
