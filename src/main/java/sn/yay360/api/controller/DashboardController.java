package sn.yay360.api.controller;

import sn.yay360.api.dto.DashboardDto;
import sn.yay360.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tableau de bord", description = "Statistiques globales pour les administrateurs (§5.12)")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Générer le tableau de bord statistique complet (Admin)")
    public DashboardDto obtenirTableauDeBord() {
        return dashboardService.genererTableauDeBord();
    }
}
