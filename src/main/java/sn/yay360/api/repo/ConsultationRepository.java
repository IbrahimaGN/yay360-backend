package sn.yay360.api.repo;

import sn.yay360.api.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByFemmeEnceinteIdOrderByDateConsultationDesc(Long femmeEnceinteId);

    List<Consultation> findByGrossesseIdOrderByDateConsultationDesc(Long grossesseId);

    List<Consultation> findByProfessionnelIdOrderByDateConsultationDesc(Long professionnelId);

    long countByGrossesseId(Long grossesseId);

    java.util.Optional<Consultation> findFirstByGrossesseIdOrderByDateConsultationDesc(Long grossesseId);

    @org.springframework.data.jpa.repository.Query(
            "select c.dateConsultation from Consultation c where c.grossesse.id = :grossesseId")
    List<java.time.LocalDateTime> datesParGrossesse(
            @org.springframework.data.repository.query.Param("grossesseId") Long grossesseId);

    @org.springframework.data.jpa.repository.Query(
            "select c.signauxCliniques from Consultation c where c.grossesse.id = :grossesseId and c.signauxCliniques is not null")
    List<String> signauxParGrossesse(@org.springframework.data.repository.query.Param("grossesseId") Long grossesseId);
}
