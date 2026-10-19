package sn.yay360.api.repo;

import sn.yay360.api.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByFemmeEnceinteIdOrderByDateConsultationDesc(Long femmeEnceinteId);

    List<Consultation> findByGrossesseIdOrderByDateConsultationDesc(Long grossesseId);

    List<Consultation> findByProfessionnelIdOrderByDateConsultationDesc(Long professionnelId);

    long countByGrossesseId(Long grossesseId);
}
