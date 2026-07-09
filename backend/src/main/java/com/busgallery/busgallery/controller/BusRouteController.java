package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.RoleGuard;
import com.busgallery.busgallery.entity.BusRoute;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleRoute;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.mapper.VehicleRouteMapper;
import com.busgallery.busgallery.service.BusRouteService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class BusRouteController {

    private final BusRouteService busRouteService;
    private final VehicleRouteMapper vehicleRouteMapper;
    private final VehicleService vehicleService;
    private final ImageService imageService;

    @GetMapping
    public RoutePageResponse page(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) Long regionId,
                                  @RequestParam(required = false) Long companyId,
                                  @RequestParam(required = false) String routeType,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Boolean isActive) {
        int pageSize = Math.max(1, Math.min(size, 50));
        int offset = Math.max(0, (Math.max(1, page) - 1) * pageSize);
        List<BusRoute> records = busRouteService.queryPage(offset, pageSize, regionId, companyId,
                routeType, StringUtils.hasText(keyword) ? keyword.trim() : null, isActive);
        long total = busRouteService.count(regionId, companyId, routeType,
                StringUtils.hasText(keyword) ? keyword.trim() : null, isActive);
        return new RoutePageResponse(records, total, page, pageSize);
    }

    @GetMapping("/{id}")
    public BusRoute detail(@PathVariable Long id) {
        BusRoute route = busRouteService.findById(id);
        if (route == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        return route;
    }

    @PostMapping
    @RequireLogin
    public BusRoute create(@Valid @RequestBody RouteRequest request) {
        // All logged-in users can create routes; reviewers/station can also update/delete
        BusRoute route = request.toRoute();
        return busRouteService.create(route, request.getRegionId(), request.getCompanyId(), request.getParentRouteId());
    }

    @PutMapping("/{id}")
    @RequireLogin
    public BusRoute update(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        RoleGuard.requireReviewerOrStation();
        BusRoute route = request.toRoute();
        route.setId(id);
        return busRouteService.update(route, request.getRegionId(), request.getCompanyId(), request.getParentRouteId());
    }

    @GetMapping("/{id}/vehicles")
    public List<VehicleController.VehicleSummary> vehicles(@PathVariable Long id) {
        List<VehicleRoute> vrs = vehicleRouteMapper.selectByRouteId(id);
        if (vrs == null || vrs.isEmpty()) {
            return Collections.emptyList();
        }
        return vrs.stream().map(vr -> {
            Vehicle vehicle = vehicleService.findById(vr.getVehicle().getId());
            if (vehicle == null) return null;
            List<Image> images = imageService.listByVehicle(vehicle.getId());
            return VehicleController.assembleDetail(vehicle,
                    vehicleService.findConfigByVehicleId(vehicle.getId()), images);
        }).filter(s -> s != null).map(s -> new VehicleController.VehicleSummary(
                s.getVehicle(), s.getImages())).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    public void delete(@PathVariable Long id) {
        RoleGuard.requireReviewerOrStation();
        busRouteService.delete(id);
    }

    @Data
    public static class RouteRequest {
        private String routeNumber;
        private String routeName;
        private String subType;
        private Long parentRouteId;
        private String startStop;
        private String endStop;
        private String downStartStop;
        private String downEndStop;
        private Boolean isLoop;
        private Long regionId;
        private Long companyId;
        private String routeType;
        private java.math.BigDecimal lineLengthKm;
        private String ticketType;
        private String ticketPrice;
        private String operatingHours;
        private Boolean isActive;
        private java.time.LocalDate firstOperated;
        private java.time.LocalDate lastOperated;
        private String remark;

        public BusRoute toRoute() {
            BusRoute r = new BusRoute();
            r.setRouteNumber(routeNumber);
            r.setRouteName(routeName);
            r.setSubType(subType);
            r.setStartStop(startStop);
            r.setEndStop(endStop);
            r.setDownStartStop(downStartStop);
            r.setDownEndStop(downEndStop);
            r.setIsLoop(Boolean.TRUE.equals(isLoop));
            r.setRouteType(StringUtils.hasText(routeType) ? routeType : "REGULAR");
            r.setLineLengthKm(lineLengthKm);
            r.setTicketType(ticketType);
            r.setTicketPrice(ticketPrice);
            r.setOperatingHours(operatingHours);
            r.setIsActive(isActive == null || isActive);
            r.setFirstOperated(firstOperated);
            r.setLastOperated(lastOperated);
            r.setRemark(remark);
            return r;
        }
    }

    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RoutePageResponse {
        private List<BusRoute> records;
        private long total;
        private int page;
        private int size;
    }
}
