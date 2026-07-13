-- ============================================================================
-- MySQL FULLTEXT Search Migration for Bus Gallery
--
-- Replaces LIKE '%kw%' with FULLTEXT + ngram parser for Chinese text search.
-- Zero extra memory overhead — runs within existing MySQL container.
--
-- UPGRADE PATH: When server memory allows (>4GB), migrate to Elasticsearch:
--   1. Create ES index mirroring these FULLTEXT columns
--   2. Bulk-index existing data from MySQL
--   3. Switch SearchController from MATCH...AGAINST to ES query
--   4. Add IK Chinese tokenizer + pinyin plugin for better CJK search
--   5. Enable ES aggregations for dynamic facet counts
-- ============================================================================

-- Vehicle: plate number, custom number, remarks
ALTER TABLE vehicle ADD FULLTEXT INDEX ft_vehicle_search
    (plate_number, custom_number, remark) WITH PARSER ngram;

-- Company: company name (Chinese text — ngram is critical here)
ALTER TABLE company ADD FULLTEXT INDEX ft_company_search
    (name) WITH PARSER ngram;

-- Brand: abbreviation + Chinese name (e.g. "BYD" + "比亚迪")
-- UPGRADE(ES): pinyin plugin would allow "yutong" -> "宇通"
ALTER TABLE brand ADD FULLTEXT INDEX ft_brand_search
    (name, chn_name) WITH PARSER ngram;

-- Region: province/city names
ALTER TABLE region ADD FULLTEXT INDEX ft_region_search
    (name) WITH PARSER ngram;

-- BusRoute: route number, name, stops (both directions)
ALTER TABLE bus_route ADD FULLTEXT INDEX ft_route_search
    (route_number, route_name, start_stop, end_stop,
     down_start_stop, down_end_stop) WITH PARSER ngram;

-- VehicleConfig: technical specs search — motor, engine, fuel type, etc.
-- UPGRADE(ES): these become faceted aggregations for filtering
ALTER TABLE vehicle_config ADD FULLTEXT INDEX ft_config_search
    (motor, engine, fuel_type, step_type, transmission_system,
     suspension, axle, other_configs) WITH PARSER ngram;

-- Model: model name
ALTER TABLE model ADD FULLTEXT INDEX ft_model_search
    (name) WITH PARSER ngram;

-- Verify indexes were created
SHOW INDEX FROM vehicle WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM company WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM brand WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM region WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM bus_route WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM vehicle_config WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM model WHERE Key_name LIKE 'ft_%';
