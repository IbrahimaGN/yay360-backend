package sn.yay360.api.controller;

import sn.yay360.api.dto.InvitationConjointRequest;
import sn.yay360.api.dto.InvitationConjointResponse;
import sn.yay360.api.service.DossierFamilialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/famille")
@RequiredArgsConstructor
@Tag(name = "Dossier familial", description = "Partage sécurisé du suivi avec le conjoint")
public class DossierFamilialController {

    private final DossierFamilialService dossierFamilialService;

    @PostMapping("/inviter-conjoint")
    @Operation(summary = "Inviter son conjoint à accéder au dossier maternel")
    public InvitationConjointResponse inviterConjoint(Authentication authentication,
                                                      @Valid @RequestBody InvitationConjointRequest request) {
        return dossierFamilialService.inviterConjoint(authentication.getName(), request);
    }
}
