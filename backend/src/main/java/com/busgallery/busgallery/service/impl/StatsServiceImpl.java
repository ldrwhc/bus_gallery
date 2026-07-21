package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.dto.response.StatsResponse;
import com.busgallery.busgallery.mapper.StatsMapper;
import com.busgallery.busgallery.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;

    @Override
    public StatsResponse getStats() {
        // Overview
        long totalVehicles = statsMapper.countVehicles();
        long totalImages = statsMapper.countImages();
        long totalModels = statsMapper.countModels();
        long totalBrands = statsMapper.countBrands();
        long totalCompanies = statsMapper.countCompanies();
        long totalRegions = statsMapper.countRegions();
        long totalRoutes = statsMapper.countRoutes();
        long totalUsers = statsMapper.countUsers();
        long totalImageSizeBytes = statsMapper.totalImageSizeBytes();

        StatsResponse.Overview overview = StatsResponse.Overview.builder()
                .totalVehicles(totalVehicles)
                .totalImages(totalImages)
                .totalModels(totalModels)
                .totalBrands(totalBrands)
                .totalCompanies(totalCompanies)
                .totalRegions(totalRegions)
                .totalRoutes(totalRoutes)
                .totalUsers(totalUsers)
                .totalImageSizeBytes(totalImageSizeBytes)
                .totalImageSizeFormatted(formatBytes(totalImageSizeBytes))
                .build();

        return StatsResponse.builder()
                .overview(overview)

                // 图片之最
                .mostImagedModels(mapRankItems(statsMapper.mostImagedModels(), "brandName"))
                .mostImagedVehicles(mapRankItems(statsMapper.mostImagedVehicles(), "modelName"))
                .mostImagedRoutes(mapRankItems(statsMapper.mostImagedRoutes(), "routeName"))
                .mostUploadingUsers(mapRankItems(statsMapper.mostUploadingUsers(), "userName"))

                // 车型与品牌之最
                .mostVehiclesByModel(mapRankItems(statsMapper.mostVehiclesByModel(), "brandName"))
                .mostVehiclesByBrand(mapRankItems(statsMapper.mostVehiclesByBrand(), "chnName"))
                .mostModelVarietyByCompany(mapRankItems(statsMapper.mostModelVarietyByCompany(), null))
                .mostModelVarietyByRoute(mapRankItems(statsMapper.mostModelVarietyByRoute(), "routeName"))

                // 地区之最
                .mostVehiclesByRegion(mapRankItems(statsMapper.mostVehiclesByRegion(), null))
                .mostRoutesByRegion(mapRankItems(statsMapper.mostRoutesByRegion(), null))
                .mostCompaniesByRegion(mapRankItems(statsMapper.mostCompaniesByRegion(), null))

                // 配置统计
                .fuelTypeDistribution(mapKvItems(statsMapper.fuelTypeDistribution()))
                .acDistribution(mapKvItems(statsMapper.acDistribution()))
                .stepTypeDistribution(mapKvItems(statsMapper.stepTypeDistribution()))

                // 时间之最
                .oldestVehicles(mapTimeRankItems(statsMapper.oldestVehicles()))
                .newestVehicles(mapTimeRankItems(statsMapper.newestVehicles()))
                .factoryYearDistribution(mapYearItems(statsMapper.factoryYearDistribution()))

                // 热度之最
                .mostViewedVehicles(mapViewRankItems(statsMapper.mostViewedVehicles()))
                .latestImages(mapImageRankItems(statsMapper.latestImages()))

                .build();
    }

    private List<StatsResponse.RankItem> mapRankItems(List<Map<String, Object>> rows, String subNameKey) {
        return rows.stream().map(row -> {
            Long id = toLong(row.get("id"));
            String name = toString(row.get("name"));
            String subName = subNameKey != null ? toString(row.get(subNameKey)) : null;
            long cnt = toLong(row.get("cnt"));
            return StatsResponse.RankItem.builder()
                    .id(id).name(name).subName(subName).cnt(cnt)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<StatsResponse.KvItem> mapKvItems(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> StatsResponse.KvItem.builder()
                .name(toString(row.get("name")))
                .cnt(toLong(row.get("cnt")))
                .build()).collect(Collectors.toList());
    }

    private List<StatsResponse.TimeRankItem> mapTimeRankItems(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> StatsResponse.TimeRankItem.builder()
                .id(toLong(row.get("id")))
                .plateNumber(toString(row.get("plateNumber")))
                .modelName(toString(row.get("modelName")))
                .dateVal(toString(row.get("dateVal")))
                .factoryDate(toString(row.get("factoryDate")))
                .launchDate(toString(row.get("launchDate")))
                .build()).collect(Collectors.toList());
    }

    private List<StatsResponse.YearItem> mapYearItems(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Object y = row.get("year");
            int year = y instanceof Number ? ((Number) y).intValue() : Integer.parseInt(String.valueOf(y));
            return StatsResponse.YearItem.builder()
                    .year(year)
                    .cnt(toLong(row.get("cnt")))
                    .build();
        }).collect(Collectors.toList());
    }

    private List<StatsResponse.ViewRankItem> mapViewRankItems(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> StatsResponse.ViewRankItem.builder()
                .id(toLong(row.get("id")))
                .plateNumber(toString(row.get("plateNumber")))
                .modelName(toString(row.get("modelName")))
                .viewCount(toLong(row.get("viewCount")))
                .build()).collect(Collectors.toList());
    }

    private List<StatsResponse.ImageRankItem> mapImageRankItems(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> StatsResponse.ImageRankItem.builder()
                .id(toLong(row.get("id")))
                .url(toString(row.get("url")))
                .thumbnailUrl(toString(row.get("thumbnailUrl")))
                .uploaderName(toString(row.get("uploaderName")))
                .createdAt(toString(row.get("createdAt")))
                .plateNumber(toString(row.get("plateNumber")))
                .modelName(toString(row.get("modelName")))
                .build()).collect(Collectors.toList());
    }

    private static String toString(Object val) {
        return val == null ? null : String.valueOf(val);
    }

    private static long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        long kb = bytes / 1024;
        if (kb < 1024) return kb + " KB";
        long mb = kb / 1024;
        if (mb < 1024) return mb + " MB";
        long gb = mb / 1024;
        return String.format("%.2f GB", gb + (mb % 1024) / 1024.0);
    }
}
