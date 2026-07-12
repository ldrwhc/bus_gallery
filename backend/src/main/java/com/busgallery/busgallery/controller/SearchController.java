package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.BusRoute;
import com.busgallery.busgallery.service.BusRouteService;
import com.busgallery.busgallery.service.CompanyService;
import com.busgallery.busgallery.service.RegionService;
import com.busgallery.busgallery.service.VehicleService;
import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int PREVIEW_LIMIT = 5;

    private final VehicleService vehicleService;
    private final BusRouteService busRouteService;
    private final CompanyService companyService;
    private final RegionService regionService;

    @GetMapping
    public SearchResult search(@RequestParam String keyword,
                               @RequestParam(defaultValue = "all") String scope) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return SearchResult.empty();
        }

        boolean searchVehicles = scope.equals("all") || scope.equals("vehicles");
        boolean searchRoutes = scope.equals("all") || scope.equals("routes");
        boolean searchCompanies = scope.equals("all") || scope.equals("companies");
        boolean searchRegions = scope.equals("all") || scope.equals("regions");

        SearchResult result = new SearchResult();
        result.setKeyword(kw);

        if (searchVehicles) {
            List<Vehicle> vehicles = vehicleService.queryPage(PREVIEW_LIMIT, null, null,
                    null, null, kw, null, null);
            long total = vehicleService.count(null, null, null, null, kw);
            result.setVehicles(new SearchCategory(
                    total,
                    vehicles.stream().map(v -> SearchItem.ofVehicle(v)).collect(Collectors.toList())
            ));
        }

        if (searchRoutes) {
            List<BusRoute> routes = busRouteService.searchByKeyword(kw, PREVIEW_LIMIT);
            long total = busRouteService.count(null, null, null, kw, null);
            result.setRoutes(new SearchCategory(
                    total,
                    routes.stream().map(r -> SearchItem.ofRoute(r)).collect(Collectors.toList())
            ));
        }

        if (searchCompanies) {
            List<Company> matched = companyService.findAll().stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(kw.toLowerCase()))
                    .limit(PREVIEW_LIMIT)
                    .collect(Collectors.toList());
            long companyTotal = companyService.findAll().stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(kw.toLowerCase()))
                    .count();
            result.setCompanies(new SearchCategory(
                    companyTotal,
                    matched.stream().map(c -> SearchItem.ofCompany(c)).collect(Collectors.toList())
            ));
        }

        if (searchRegions) {
            List<Region> matched = regionService.findAll().stream()
                    .filter(r -> r.getName() != null && r.getName().toLowerCase().contains(kw.toLowerCase()))
                    .limit(PREVIEW_LIMIT)
                    .collect(Collectors.toList());
            long regionTotal = regionService.findAll().stream()
                    .filter(r -> r.getName() != null && r.getName().toLowerCase().contains(kw.toLowerCase()))
                    .count();
            result.setRegions(new SearchCategory(
                    regionTotal,
                    matched.stream().map(r -> SearchItem.ofRegion(r)).collect(Collectors.toList())
            ));
        }

        return result;
    }

    @GetMapping("/suggest")
    public List<SearchSuggestion> suggest(@RequestParam String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return Collections.emptyList();
        }
        List<BusRoute> routes = busRouteService.searchByKeyword(kw, 5);
        return routes.stream()
                .map(r -> new SearchSuggestion(r.getRouteNumber(), "route", r.getId()))
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    public static class SearchResult {
        private String keyword;
        private SearchCategory vehicles;
        private SearchCategory routes;
        private SearchCategory companies;
        private SearchCategory regions;

        public static SearchResult empty() {
            return new SearchResult();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchCategory {
        private long total;
        private List<SearchItem> items;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchItem {
        private Long id;
        private String title;
        private String subtitle;
        private String type;
        private String imageUrl;

        public static SearchItem ofVehicle(Vehicle v) {
            SearchItem item = new SearchItem();
            item.id = v.getId();
            item.title = v.getPlateNumber();
            item.subtitle = (v.getModel() != null ? v.getModel().getName() : null);
            item.type = "vehicle";
            return item;
        }

        public static SearchItem ofRoute(BusRoute r) {
            SearchItem item = new SearchItem();
            item.id = r.getId();
            item.title = r.getRouteNumber();
            item.subtitle = (r.getStartStop() != null ? r.getStartStop() + " ↔ " + r.getEndStop() : null);
            item.type = "route";
            return item;
        }

        public static SearchItem ofCompany(Company c) {
            SearchItem item = new SearchItem();
            item.id = c.getId();
            item.title = c.getName();
            item.subtitle = c.getRegion() != null ? c.getRegion().getName() : null;
            item.type = "company";
            return item;
        }

        public static SearchItem ofRegion(Region r) {
            SearchItem item = new SearchItem();
            item.id = r.getId();
            item.title = r.getName();
            item.subtitle = null;
            item.type = "region";
            return item;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchSuggestion {
        private String value;
        private String type;
        private Long id;
    }
}
