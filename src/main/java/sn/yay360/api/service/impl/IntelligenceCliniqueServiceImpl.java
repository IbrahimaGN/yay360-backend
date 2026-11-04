package sn.yay360.api.service.impl;

import sn.yay360.api.dto.*;
import sn.yay360.api.helper.IntelligenceCliniqueHelper;
import sn.yay360.api.service.IntelligenceCliniqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IntelligenceCliniqueServiceImpl implements IntelligenceCliniqueService {

    private final IntelligenceCliniqueHelper intelligenceCliniqueHelper;

    @Override
    public PlanCpnDto monPlanCpn(String email) {
        return intelligenceCliniqueHelper.monPlanCpn(email);
    }

    @Override
    public PlanCpnDto planCpnParId(String email, Long grossesseId) {
        return intelligenceCliniqueHelper.planCpnParId(email, grossesseId);
    }

    @Override
    public EvaluationRisqueDto evaluerRisque(String email, Long grossesseId) {
        return intelligenceCliniqueHelper.evaluerRisque(email, grossesseId);
    }
}
