package sn.yay360.api.mapper;

import sn.yay360.api.dto.EtapeGrossesseDto;
import sn.yay360.api.model.EtapeGrossesse;
import org.springframework.stereotype.Component;

@Component
public class EtapeGrossesseMapper {

    public EtapeGrossesseDto versDto(EtapeGrossesse etape) {
        if (etape == null) {
            return null;
        }
        return new EtapeGrossesseDto(
                etape.getId(),
                etape.getSemaine(),
                etape.getTrimestre(),
                etape.getDeveloppementBebe(),
                etape.getChangementsMere(),
                etape.getObjectifsSante(),
                etape.getConseilsAlimentation(),
                etape.getConseilsHydratation(),
                etape.getExercicesPhysiques(),
                etape.getConseilsSommeil(),
                etape.getPrecautions(),
                etape.getExamensRecommandes(),
                etape.getSignesAlerte()
        );
    }
}
