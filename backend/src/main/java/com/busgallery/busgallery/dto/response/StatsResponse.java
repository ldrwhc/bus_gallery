package com.busgallery.busgallery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {

    private Overview overview;

    // 图片之最
    private List<RankItem> mostImagedModels;
    private List<RankItem> mostImagedVehicles;
    private List<RankItem> mostImagedRoutes;
    private List<RankItem> mostUploadingUsers;

    // 车型与品牌之最
    private List<RankItem> mostVehiclesByModel;
    private List<RankItem> mostVehiclesByBrand;
    private List<RankItem> mostModelVarietyByCompany;
    private List<RankItem> mostModelVarietyByRoute;

    // 地区之最
    private List<RankItem> mostVehiclesByRegion;
    private List<RankItem> mostRoutesByRegion;
    private List<RankItem> mostCompaniesByRegion;

    // 配置统计
    private List<KvItem> fuelTypeDistribution;
    private List<KvItem> acDistribution;
    private List<KvItem> stepTypeDistribution;

    // 时间之最
    private List<TimeRankItem> oldestVehicles;
    private List<TimeRankItem> newestVehicles;
    private List<YearItem> factoryYearDistribution;

    // 热度之最
    private List<ViewRankItem> mostViewedVehicles;
    private List<ImageRankItem> latestImages;

    // ===== Inner DTOs =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private long totalVehicles;
        private long totalImages;
        private long totalModels;
        private long totalBrands;
        private long totalCompanies;
        private long totalRegions;
        private long totalRoutes;
        private long totalUsers;
        private long totalImageSizeBytes;
        private String totalImageSizeFormatted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankItem {
        private Long id;
        private String name;
        private String subName;     // brand name, plate number, etc.
        private String extra;       // additional info
        private long cnt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KvItem {
        private String name;
        private long cnt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRankItem {
        private Long id;
        private String plateNumber;
        private String modelName;
        private String dateVal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearItem {
        private int year;
        private long cnt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewRankItem {
        private Long id;
        private String plateNumber;
        private String modelName;
        private long viewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageRankItem {
        private Long id;
        private String url;
        private String thumbnailUrl;
        private String uploaderName;
        private String createdAt;
    }
}
