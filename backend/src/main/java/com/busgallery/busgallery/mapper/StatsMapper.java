package com.busgallery.busgallery.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    // ===== Overview =====

    @Select("SELECT COUNT(*) FROM vehicle")
    long countVehicles();

    @Select("SELECT COUNT(*) FROM image")
    long countImages();

    @Select("SELECT COUNT(*) FROM model")
    long countModels();

    @Select("SELECT COUNT(*) FROM brand")
    long countBrands();

    @Select("SELECT COUNT(*) FROM company")
    long countCompanies();

    @Select("SELECT COUNT(*) FROM region")
    long countRegions();

    @Select("SELECT COUNT(*) FROM bus_route")
    long countRoutes();

    @Select("SELECT COUNT(*) FROM app_user")
    long countUsers();

    @Select("SELECT COALESCE(SUM(size_bytes), 0) FROM image")
    long totalImageSizeBytes();

    // ===== 图片之最 =====

    @Select("SELECT m.id AS id, m.name AS name, b.name AS brandName, " +
            "COUNT(DISTINCT vi.image_id) AS cnt " +
            "FROM vehicle_image vi " +
            "JOIN vehicle v ON vi.vehicle_id = v.id " +
            "JOIN model m ON v.model_id = m.id " +
            "LEFT JOIN brand b ON m.brand_id = b.id " +
            "GROUP BY m.id, m.name, b.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostImagedModels();

    @Select("SELECT v.id AS id, v.plate_number AS name, " +
            "m.name AS modelName, COUNT(vi.image_id) AS cnt " +
            "FROM vehicle_image vi " +
            "JOIN vehicle v ON vi.vehicle_id = v.id " +
            "JOIN model m ON v.model_id = m.id " +
            "GROUP BY v.id, v.plate_number, m.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostImagedVehicles();

    @Select("SELECT br.id AS id, " +
            "CONCAT(br.route_number, ' | ', COALESCE(r.name, '')) AS name, " +
            "br.route_name AS routeName, COUNT(i.id) AS cnt " +
            "FROM image i " +
            "JOIN bus_route br ON i.route_id = br.id " +
            "LEFT JOIN region r ON br.region_id = r.id " +
            "WHERE i.route_id IS NOT NULL " +
            "GROUP BY br.id, br.route_number, br.route_name, r.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostImagedRoutes();

    @Select("SELECT i.uploader_id AS userId, " +
            "COALESCE(i.uploader_display_name, i.uploader_username) AS name, " +
            "COUNT(i.id) AS cnt " +
            "FROM image i " +
            "WHERE i.uploader_id IS NOT NULL " +
            "GROUP BY i.uploader_id, i.uploader_username, i.uploader_display_name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostUploadingUsers();

    // ===== 车型与品牌之最 =====

    @Select("SELECT m.id AS id, m.name AS name, b.name AS brandName, " +
            "COUNT(v.id) AS cnt " +
            "FROM vehicle v " +
            "JOIN model m ON v.model_id = m.id " +
            "LEFT JOIN brand b ON m.brand_id = b.id " +
            "GROUP BY m.id, m.name, b.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostVehiclesByModel();

    @Select("SELECT b.id AS id, b.name AS name, b.chn_name AS chnName, " +
            "COUNT(v.id) AS cnt " +
            "FROM vehicle v " +
            "JOIN model m ON v.model_id = m.id " +
            "JOIN brand b ON m.brand_id = b.id " +
            "GROUP BY b.id, b.name, b.chn_name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostVehiclesByBrand();

    @Select("SELECT c.id AS id, c.name AS name, " +
            "COUNT(DISTINCT v.model_id) AS cnt " +
            "FROM vehicle v " +
            "JOIN company c ON v.company_id = c.id " +
            "GROUP BY c.id, c.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostModelVarietyByCompany();

    @Select("SELECT br.id AS id, " +
            "CONCAT(br.route_number, ' | ', COALESCE(r.name, '')) AS name, " +
            "br.route_name AS routeName, " +
            "COUNT(DISTINCT v.model_id) AS cnt " +
            "FROM vehicle_route vr " +
            "JOIN vehicle v ON vr.vehicle_id = v.id " +
            "JOIN bus_route br ON vr.route_id = br.id " +
            "LEFT JOIN region r ON br.region_id = r.id " +
            "GROUP BY br.id, br.route_number, br.route_name, r.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostModelVarietyByRoute();

    // ===== 地区之最 =====

    @Select("SELECT r.id AS id, r.name AS name, COUNT(v.id) AS cnt " +
            "FROM vehicle v " +
            "JOIN region r ON v.region_id = r.id " +
            "GROUP BY r.id, r.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostVehiclesByRegion();

    @Select("SELECT r.id AS id, r.name AS name, COUNT(br.id) AS cnt " +
            "FROM bus_route br " +
            "JOIN region r ON br.region_id = r.id " +
            "GROUP BY r.id, r.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostRoutesByRegion();

    @Select("SELECT r.id AS id, r.name AS name, COUNT(c.id) AS cnt " +
            "FROM company c " +
            "JOIN region r ON c.region_id = r.id " +
            "GROUP BY r.id, r.name " +
            "ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> mostCompaniesByRegion();

    // ===== 配置统计 =====

    @Select("SELECT vc.fuel_type AS name, COUNT(*) AS cnt " +
            "FROM vehicle_config vc " +
            "WHERE vc.fuel_type IS NOT NULL AND vc.fuel_type != '' " +
            "GROUP BY vc.fuel_type " +
            "ORDER BY cnt DESC")
    List<Map<String, Object>> fuelTypeDistribution();

    @Select("SELECT CASE WHEN air_conditioned = 1 THEN '空调车' ELSE '非空调车' END AS name, " +
            "COUNT(*) AS cnt " +
            "FROM vehicle " +
            "GROUP BY air_conditioned")
    List<Map<String, Object>> acDistribution();

    @Select("SELECT vc.step_type AS name, COUNT(*) AS cnt " +
            "FROM vehicle_config vc " +
            "WHERE vc.step_type IS NOT NULL AND vc.step_type != '' " +
            "GROUP BY vc.step_type " +
            "ORDER BY cnt DESC")
    List<Map<String, Object>> stepTypeDistribution();

    // ===== 时间之最 =====

    @Select("SELECT v.id AS id, v.plate_number AS plateNumber, " +
            "m.name AS modelName, " +
            "DATE_FORMAT(LEAST(COALESCE(v.factory_date, v.launch_date), " +
            "  COALESCE(v.launch_date, v.factory_date)), '%Y-%m') AS dateVal, " +
            "DATE_FORMAT(v.factory_date, '%Y-%m') AS factoryDate, " +
            "DATE_FORMAT(v.launch_date, '%Y-%m') AS launchDate " +
            "FROM vehicle v " +
            "JOIN model m ON v.model_id = m.id " +
            "WHERE v.factory_date IS NOT NULL OR v.launch_date IS NOT NULL " +
            "ORDER BY LEAST(COALESCE(v.factory_date, v.launch_date), " +
            "  COALESCE(v.launch_date, v.factory_date)) ASC LIMIT 10")
    List<Map<String, Object>> oldestVehicles();

    @Select("SELECT v.id AS id, v.plate_number AS plateNumber, " +
            "m.name AS modelName, " +
            "DATE_FORMAT(LEAST(COALESCE(v.factory_date, v.launch_date), " +
            "  COALESCE(v.launch_date, v.factory_date)), '%Y-%m') AS dateVal, " +
            "DATE_FORMAT(v.factory_date, '%Y-%m') AS factoryDate, " +
            "DATE_FORMAT(v.launch_date, '%Y-%m') AS launchDate " +
            "FROM vehicle v " +
            "JOIN model m ON v.model_id = m.id " +
            "WHERE v.factory_date IS NOT NULL OR v.launch_date IS NOT NULL " +
            "ORDER BY LEAST(COALESCE(v.factory_date, v.launch_date), " +
            "  COALESCE(v.launch_date, v.factory_date)) DESC LIMIT 10")
    List<Map<String, Object>> newestVehicles();

    @Select("SELECT YEAR(v.factory_date) AS year, COUNT(*) AS cnt " +
            "FROM vehicle v " +
            "WHERE v.factory_date IS NOT NULL " +
            "GROUP BY YEAR(v.factory_date) " +
            "ORDER BY year DESC")
    List<Map<String, Object>> factoryYearDistribution();

    // ===== 热度之最 =====

    @Select("SELECT v.id AS id, v.plate_number AS plateNumber, " +
            "m.name AS modelName, v.view_count AS viewCount " +
            "FROM vehicle v " +
            "JOIN model m ON v.model_id = m.id " +
            "ORDER BY v.view_count DESC, v.id DESC LIMIT 10")
    List<Map<String, Object>> mostViewedVehicles();

    @Select("SELECT i.id AS id, i.url AS url, i.thumbnail_url AS thumbnailUrl, " +
            "COALESCE(i.uploader_display_name, i.uploader_username) AS uploaderName, " +
            "i.created_at AS createdAt, " +
            "COALESCE(v.plate_number, '无车牌') AS plateNumber, " +
            "COALESCE(m.name, '') AS modelName " +
            "FROM image i " +
            "LEFT JOIN vehicle_image vi ON vi.image_id = i.id " +
            "LEFT JOIN vehicle v ON vi.vehicle_id = v.id " +
            "LEFT JOIN model m ON v.model_id = m.id " +
            "ORDER BY i.created_at DESC, i.id DESC LIMIT 10")
    List<Map<String, Object>> latestImages();
}
