package sn.yay360.api.service.impl;

import sn.yay360.api.dto.DashboardDto;
import sn.yay360.api.helper.DashboardHelper;
import sn.yay360.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardHelper dashboardHelper;

    @Override
    public DashboardDto genererTableauDeBord() {
        return dashboardHelper.genererTableauDeBord();
    }
}
