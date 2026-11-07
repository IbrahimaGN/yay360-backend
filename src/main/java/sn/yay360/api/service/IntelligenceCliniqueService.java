package sn.yay360.api.service;

import sn.yay360.api.dto.*;

import java.util.List;

public interface IntelligenceCliniqueService {

    PlanCpnDto monPlanCpn(String email);

    PlanCpnDto planCpnParId(String email, Long grossesseId);

    EvaluationRisqueDto evaluerRisque(String email, Long grossesseId);
}
