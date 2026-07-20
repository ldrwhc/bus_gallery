package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.mapper.BrandMapper;
import com.busgallery.busgallery.mapper.CompanyMapper;
import com.busgallery.busgallery.mapper.RegionMapper;
import com.busgallery.busgallery.service.BusRouteService;
import com.busgallery.busgallery.service.ImageAccessService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified search controller.
 *
 * UPGRADE(ES): When server memory exceeds 4 GB, replace the FULLTEXT-based
 * search with Elasticsearch:
 *   1. Index: bus_gallery_search with fields from all entities
 *   2. Use IK Chinese tokenizer for CJK text, pinyin for romanized search
 *   3. Use ES aggregations for dynamic facet counts (brand, region, fuel, etc.)
 *   4. Use ES suggest API for real-time autocomplete
 *   5. Keep this controller's API contract unchanged — just swap the implementation
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int PREVIEW_LIMIT = 5;

    private final VehicleService vehicleService;
    private final BusRouteService busRouteService;
    private final ImageService imageService;
    private final ImageAccessService imageAccessService;
    private final BrandMapper brandMapper;
    private final CompanyMapper companyMapper;
    private final RegionMapper regionMapper;

    /**
     * Unified search across vehicles, routes, companies, regions, and brands.
     * Uses MySQL FULLTEXT + ngram for Chinese text matching.
     * UPGRADE(ES): Replace with a single ES multi-index search query.
     */
    @GetMapping
    public SearchResult search(@RequestParam String keyword,
                               @RequestParam(defaultValue = "all") String scope) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return SearchResult.empty();
        }

        boolean all = "all".equals(scope);
        SearchResult result = new SearchResult();
        result.setKeyword(kw);

        // --- Vehicles (uses FULLTEXT on company/region/brand + LIKE on plate_number) ---
        if (all || "vehicles".equals(scope)) {
            List<Vehicle> vehicles = vehicleService.queryPage(PREVIEW_LIMIT, null, null,
                    null, null, kw, null, null);
            long total = vehicleService.count(null, null, null, null, kw);
            result.setVehicles(new SearchCategory(total, vehicles.stream()
                    .map(v -> SearchItem.ofVehicle(v, findFirstImageUrl(v.getId())))
                    .collect(Collectors.toList())));
        }

        // --- Routes (uses FULLTEXT on route_number, route_name, stops) ---
        if (all || "routes".equals(scope)) {
            List<BusRoute> routes = busRouteService.searchByKeyword(kw, PREVIEW_LIMIT);
            long total = busRouteService.count(null, null, null, kw, null);
            result.setRoutes(new SearchCategory(total, routes.stream()
                    .map(SearchItem::ofRoute)
                    .collect(Collectors.toList())));
        }

        // --- Regions (FULLTEXT; searched first so companies can use region matches) ---
        List<Region> matchedRegions = Collections.emptyList();
        if (all || "regions".equals(scope)) {
            matchedRegions = regionMapper.searchByKeyword(kw, PREVIEW_LIMIT);
            result.setRegions(new SearchCategory(matchedRegions.size(), matchedRegions.stream()
                    .map(SearchItem::ofRegion)
                    .collect(Collectors.toList())));
        }

        // --- Companies (FULLTEXT on name + companies in matching regions) ---
        if (all || "companies".equals(scope)) {
            Set<Company> companySet = new LinkedHashSet<>(companyMapper.searchByKeyword(kw, PREVIEW_LIMIT));
            // Also include companies from matched regions
            if (companySet.size() < PREVIEW_LIMIT) {
                for (Region region : matchedRegions) {
                    if (companySet.size() >= PREVIEW_LIMIT) break;
                    List<Company> regionCompanies = companyMapper.selectByRegionId(region.getId());
                    for (Company c : regionCompanies) {
                        if (companySet.size() >= PREVIEW_LIMIT) break;
                        companySet.add(c);
                    }
                }
            }
            List<Company> matched = new ArrayList<>(companySet);
            result.setCompanies(new SearchCategory(matched.size(), matched.stream()
                    .map(SearchItem::ofCompany)
                    .collect(Collectors.toList())));
        }

        // --- Brands (UPGRADE(ES): add as a facet aggregation in the search response) ---
        if (all) {
            List<Brand> brands = brandMapper.searchByKeyword(kw, PREVIEW_LIMIT);
            if (!brands.isEmpty()) {
                result.setBrands(new SearchCategory(brands.size(), brands.stream()
                        .map(SearchItem::ofBrand)
                        .collect(Collectors.toList())));
            }
        }

        return result;
    }

    /**
     * Search suggestions for autocomplete.
     * UPGRADE(ES): Use ES suggest API with completion suggesters for instant results.
     */
    @GetMapping("/suggest")
    public List<SearchSuggestion> suggest(@RequestParam String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return Collections.emptyList();
        }

        List<SearchSuggestion> suggestions = new ArrayList<>();

        // Route suggestions (most useful for bus gallery)
        List<BusRoute> routes = busRouteService.searchByKeyword(kw, 3);
        routes.forEach(r -> {
            String label = r.getRouteNumber();
            if (r.getRegion() != null && StringUtils.hasText(r.getRegion().getName())) {
                label = r.getRouteNumber() + " | " + r.getRegion().getName();
            }
            suggestions.add(new SearchSuggestion(label, "route", r.getId()));
        });

        // Vehicle plate suggestions
        List<Vehicle> vehicles = vehicleService.queryPage(3, null, null,
                null, null, kw, null, null);
        vehicles.forEach(v -> suggestions.add(
                new SearchSuggestion(v.getPlateNumber(), "vehicle", v.getId())));

        // Brand suggestions (UPGRADE(ES): pinyin would give "yutong" -> "宇通")
        List<Brand> brands = brandMapper.searchByKeyword(kw, 2);
        brands.forEach(b -> suggestions.add(
                new SearchSuggestion(
                        b.getChnName() != null ? b.getChnName() : b.getName(),
                        "brand", b.getId())));

        return suggestions;
    }

    /**
     * Resolve the first (thumbnail) image URL for a vehicle.
     * UPGRADE(ES): Store thumbnail_url directly in the ES index — no need for this DB lookup.
     */
    private String findFirstImageUrl(Long vehicleId) {
        if (vehicleId == null) return null;
        try {
            List<Image> images = imageService.listByVehicle(vehicleId);
            if (images != null && !images.isEmpty()) {
                Image first = images.get(0);
                String objectName = imageAccessService.resolveObjectNameRef(
                        first.getThumbnailUrl() != null ? first.getThumbnailUrl() : first.getUrl());
                if (objectName != null && !objectName.isBlank()) {
                    return imageAccessService.signThumbnailObject(objectName);
                }
            }
        } catch (Exception ignore) {
            // Return null on any error — image URL is optional for search results
        }
        return null;
    }

    // ---- DTOs ----

    @Data
    @NoArgsConstructor
    public static class SearchResult {
        private String keyword;
        private SearchCategory vehicles;
        private SearchCategory routes;
        private SearchCategory companies;
        private SearchCategory regions;
        /** UPGRADE(ES): This becomes a facet aggregation — add fuel_type, step_type etc. */
        private SearchCategory brands;

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
        /** UPGRADE(ES): Stored directly in the index, no post-query enrichment needed. */
        private String imageUrl;

        public static SearchItem ofVehicle(Vehicle v, String imageUrl) {
            SearchItem item = new SearchItem();
            item.id = v.getId();
            item.title = v.getPlateNumber();
            item.subtitle = (v.getModel() != null ? v.getModel().getName() : null);
            item.type = "vehicle";
            item.imageUrl = imageUrl;
            return item;
        }

        public static SearchItem ofRoute(BusRoute r) {
            SearchItem item = new SearchItem();
            item.id = r.getId();
            String regionName = (r.getRegion() != null && StringUtils.hasText(r.getRegion().getName()))
                    ? r.getRegion().getName() : null;
            item.title = regionName != null ? r.getRouteNumber() + " | " + regionName : r.getRouteNumber();
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

        public static SearchItem ofBrand(Brand b) {
            SearchItem item = new SearchItem();
            item.id = b.getId();
            item.title = b.getChnName() != null ? b.getChnName() : b.getName();
            item.subtitle = b.getChnName() != null ? b.getName() : null;
            item.type = "brand";
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
