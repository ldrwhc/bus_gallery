#!/usr/bin/env python3
"""
Re-scrape all vehicle configs from buspedia.top and update the database.

Usage:
  python rescrape_buspedia_config.py --dry-run    # preview only, no DB writes
  python rescrape_buspedia_config.py              # actually update DB
  python rescrape_buspedia_config.py --plate "京A R1809"  # single vehicle
"""

import sys, os, re, json, zlib, urllib.parse, urllib.request, ssl, time, argparse
from datetime import datetime

# ── MySQL connection (pymysql) ──────────────────────────────────────────
try:
    import pymysql
except ImportError:
    print("[ERROR] pymysql not installed. Run: pip install pymysql")
    sys.exit(1)

DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 13306,
    "user": "root",
    "password": "123456",
    "database": "bus_gallery",
    "charset": "utf8mb4",
}

API_BASE = "https://api.buspedia.top"
SEARCH_DELAY = 1.5   # seconds between search requests
DETAIL_DELAY = 0.5   # seconds between detail requests

# ── Fuel type normalization ─────────────────────────────────────────────
# Map buspedia raw fuel values to our internal FUEL_OPTIONS values
FUEL_NORMALIZE = {
    "电": "纯电",
    "电动": "纯电",
    "纯电动": "纯电",
    "纯电": "纯电",
    "柴油": "柴油",
    "柴油+电": "柴油+电",
    "柴油,电": "柴油+电",
    "压缩天然气": "压缩天然气",
    "压缩天然气+电": "压缩天然气+电",
    "压缩天然气,电": "压缩天然气+电",
    "液化天然气": "液化天然气",
    "液化天然气+电": "液化天然气+电",
    "液化天然气,电": "液化天然气+电",
    "压缩氢气+电": "压缩氢气+电",
    "压缩氢气,电": "压缩氢气+电",
}

def normalize_fuel(raw):
    """Normalize buspedia fuel value to our internal format."""
    if not raw:
        return ""
    # Normalize separators: remove spaces, replace commas with +
    normalized = raw.replace(",", "+").replace("，", "+").replace(" ", "")
    # Direct match in our mapping
    if normalized in FUEL_NORMALIZE:
        return FUEL_NORMALIZE[normalized]
    # Try fuzzy containment
    for key, val in FUEL_NORMALIZE.items():
        if normalized in key or key in normalized:
            return val
    return normalized

# ── Buspedia API helpers ─────────────────────────────────────────────────

def fetch_api(path, timeout=15):
    """Fetch and decompress buspedia API response."""
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    req = urllib.request.Request(f"{API_BASE}{path}")
    req.add_header("User-Agent", "Mozilla/5.0 BusGalleryReScrape/1.0")
    req.add_header("Accept", "application/json")
    req.add_header("Origin", "https://buspedia.top")
    req.add_header("Referer", "https://buspedia.top/")
    try:
        resp = urllib.request.urlopen(req, timeout=timeout)
        raw = resp.read()
        if raw[:2] == b"x\x9c":  # zlib magic
            raw = zlib.decompress(raw)
        return json.loads(raw.decode("utf-8", errors="replace"))
    except Exception as e:
        print(f"  [API ERROR] {path}: {e}")
        return None


def search_buspedia(plate):
    """Search buspedia for a plate number, return the bus slug.

    Note: buspedia search needs `?name=` parameter and plate with spaces preserved.
    Response: {"v": [{"id": "m19pwe", ...}], ...}
    """
    # Keep original format first (with space, e.g. "京A R1809"), then try without
    variants = [plate]  # original format
    clean = plate.replace(" ", "")
    if clean != plate:
        variants.append(clean)

    for query in variants:
        try:
            result = fetch_api(f"/search?name={urllib.parse.quote(query)}")
            if isinstance(result, dict):
                vehicles = result.get("v", [])
                if vehicles and isinstance(vehicles, list) and len(vehicles) > 0:
                    first = vehicles[0]
                    slug = first.get("id") or first.get("uid") or first.get("slug")
                    if slug:
                        return slug
            elif isinstance(result, list) and result:
                return result[0].get("slug") or result[0].get("id")
        except Exception:
            pass
    return None


def scrape_bus_detail(slug):
    """Extract vehicle config data from buspedia bus detail API."""
    data = fetch_api(f"/bus/{slug}")
    if not data:
        return None

    result = {"_slug": slug}
    veh = data.get("veh", {})
    model = data.get("model", {})
    manuf = model.get("manuf", {})

    # Brand
    brand_name = ""
    if isinstance(manuf, list):
        for m in manuf:
            if isinstance(m, dict) and m.get("name"):
                brand_name = m["name"]
                break
    elif isinstance(manuf, dict):
        brand_name = manuf.get("name", "") or ""
    result["brandName"] = brand_name

    # Model code
    result["modelCode"] = model.get("model", "") or ""

    # Engine / Motor
    if model.get("ice"):
        result["engine"] = model["ice"]
    if model.get("motor"):
        result["motor"] = model["motor"]
    if model.get("fuel"):
        result["fuelType"] = model["fuel"].replace(",", "+").replace("，", "+").replace(" ", "")
    if model.get("trans"):
        result["transmission"] = model["trans"]
    if model.get("suspension"):
        result["suspension"] = model["suspension"]
    if model.get("step"):
        result["stepType"] = model["step"]

    # Axle from meta
    meta = model.get("meta", [])
    if isinstance(meta, list):
        for m in meta:
            if isinstance(m, dict) and m.get("item") == "车桥品牌":  # 车桥品牌
                result["axle"] = m.get("value", "")

    # Vehicle registration
    if veh.get("regist"):
        result["plateNumber"] = veh["regist"]
    if veh.get("no"):
        result["fleetNumber"] = str(veh["no"])
    # Dates: handle "2014-06" and year-only "2016"
    def pad_date(raw):
        if not raw or not re.match(r"\d{4}", str(raw)):
            return None
        s = str(raw)
        if len(s) >= 10:
            return s[:10]
        if len(s) == 7:
            return s + "-01"
        return s + "-01-01"

    dm = pad_date(veh.get("date_manuf"))
    if dm:
        result["factoryDate"] = dm
    ds = pad_date(veh.get("date_serve"))
    if ds:
        result["launchDate"] = ds

    # Company
    comp = veh.get("comp", {})
    if isinstance(comp, dict) and comp.get("name"):
        result["company"] = comp["name"]

    return result


# ── Database helpers ─────────────────────────────────────────────────────

def get_db():
    return pymysql.connect(**DB_CONFIG)


def get_all_vehicles(cursor):
    """Get all vehicles with their current config."""
    sql = """
        SELECT v.id, v.plate_number, v.custom_number,
               vc.id AS config_id,
               vc.engine, vc.motor, vc.fuel_type, vc.step_type,
               vc.transmission_system, vc.suspension, vc.axle,
               m.name AS model_name,
               b.name AS brand_name, b.chn_name AS brand_chn_name
        FROM vehicle v
        LEFT JOIN vehicle_config vc ON vc.vehicle_id = v.id
        LEFT JOIN model m ON v.model_id = m.id
        LEFT JOIN brand b ON m.brand_id = b.id
        ORDER BY v.id
    """
    cursor.execute(sql)
    return cursor.fetchall()


def update_vehicle_config(cursor, vehicle_id, config_id, fresh, dry_run):
    """Update vehicle_config with fresh buspedia data. Insert if no config exists."""
    if not fresh:
        return "no_data"

    # Normalize fuel type
    fuel_normalized = normalize_fuel(fresh.get("fuelType", ""))

    if config_id:
        # Update existing config
        sql = """
            UPDATE vehicle_config SET
                engine = %s, motor = %s, fuel_type = %s, step_type = %s,
                transmission_system = %s, suspension = %s, axle = %s
            WHERE id = %s
        """
        params = (
            fresh.get("engine", ""),
            fresh.get("motor", ""),
            fuel_normalized,
            fresh.get("stepType", ""),
            fresh.get("transmission", ""),
            fresh.get("suspension", ""),
            fresh.get("axle", ""),
            config_id,
        )
    else:
        # Insert new config
        sql = """
            INSERT INTO vehicle_config (vehicle_id, engine, motor, fuel_type, step_type,
                transmission_system, suspension, axle)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """
        params = (
            vehicle_id,
            fresh.get("engine", ""),
            fresh.get("motor", ""),
            fuel_normalized,
            fresh.get("stepType", ""),
            fresh.get("transmission", ""),
            fresh.get("suspension", ""),
            fresh.get("axle", ""),
        )

    if dry_run:
        return f"DRY_RUN: {params}"
    cursor.execute(sql, params)
    return "updated" if config_id else "inserted"


def update_vehicle_fields(cursor, vehicle_id, fresh, dry_run):
    """Update fleet number and dates on vehicle table."""
    updates = []
    params = []

    if fresh.get("fleetNumber"):
        updates.append("custom_number = %s")
        params.append(fresh["fleetNumber"])
    if fresh.get("factoryDate"):
        updates.append("factory_date = %s")
        params.append(fresh["factoryDate"])
    if fresh.get("launchDate"):
        updates.append("launch_date = %s")
        params.append(fresh["launchDate"])

    if not updates:
        return "no_changes"

    params.append(vehicle_id)
    sql = f"UPDATE vehicle SET {', '.join(updates)} WHERE id = %s"

    if dry_run:
        return f"DRY_RUN: {params}"
    cursor.execute(sql, params)
    return "updated"


def diff_config(old, new):
    """Return list of changed fields."""
    field_map = [
        ("engine", "engine", "发动机"),
        ("motor", "motor", "电机"),
        ("fuel_type", "fuelType", "燃料"),
        ("step_type", "stepType", "踏步"),
        ("transmission_system", "transmission", "变速箱"),
        ("suspension", "suspension", "悬挂"),
        ("axle", "axle", "车桥"),
    ]
    changes = []
    for db_field, api_field, label in field_map:
        old_val = (old.get(db_field) or "").strip()
        new_val = (new.get(api_field) or "").strip()
        # Normalize fuel for comparison
        if db_field == "fuel_type":
            new_val = normalize_fuel(new_val)
        if old_val != new_val:
            changes.append((label, old_val, new_val))
    return changes


# ── Main ──────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Re-scrape buspedia configs")
    parser.add_argument("--dry-run", action="store_true", help="Preview only, no DB writes")
    parser.add_argument("--plate", type=str, help="Scrape a single plate only")
    parser.add_argument("--limit", type=int, default=0, help="Limit number of vehicles to process")
    args = parser.parse_args()

    dry_run = args.dry_run
    if dry_run:
        print("=" * 60)
        print("  DRY RUN MODE — no database changes will be made")
        print("=" * 60)

    print(f"[{datetime.now().strftime('%H:%M:%S')}] Connecting to database...")
    db = get_db()
    cursor = db.cursor()

    # Get vehicles
    cursor.execute("SELECT COUNT(*) FROM vehicle")
    total_count = cursor.fetchone()[0]
    print(f"  Total vehicles in database: {total_count}")

    vehicles = get_all_vehicles(cursor)

    # Filter by plate if specified
    if args.plate:
        plate_filter = args.plate.replace(" ", "").upper()
        vehicles = [v for v in vehicles if (v[1] or "").replace(" ", "").upper() == plate_filter]
        print(f"  Filtered to plate '{args.plate}': {len(vehicles)} vehicle(s)")

    if args.limit > 0:
        vehicles = vehicles[: args.limit]
        print(f"  Limited to first {args.limit} vehicle(s)")

    print(f"  Processing {len(vehicles)} vehicle(s)")
    print()

    stats = {
        "total": len(vehicles),
        "searched": 0,
        "found": 0,
        "not_found": 0,
        "api_error": 0,
        "config_updated": 0,
        "config_unchanged": 0,
        "details": [],
    }

    for i, v in enumerate(vehicles):
        vehicle_id = v[0]
        plate = (v[1] or "").strip()
        custom_num = (v[2] or "").strip()
        config_id = v[3]
        old_config = {
            "engine": v[4] or "",
            "motor": v[5] or "",
            "fuel_type": v[6] or "",
            "step_type": v[7] or "",
            "transmission_system": v[8] or "",
            "suspension": v[9] or "",
            "axle": v[10] or "",
        }
        model_name = v[11] or ""
        brand_name = v[12] or ""

        prefix = f"[{i+1}/{len(vehicles)}]"
        info_parts = [plate]
        if custom_num:
            info_parts.append(f"#{custom_num}")
        if model_name:
            info_parts.append(model_name)
        label = " ".join(info_parts)

        if not plate:
            print(f"  {prefix} SKIP (no plate) id={vehicle_id}")
            continue

        # Search buspedia
        print(f"  {prefix} Searching: {label}...", end=" ", flush=True)
        stats["searched"] += 1
        time.sleep(SEARCH_DELAY)
        slug = search_buspedia(plate)

        if not slug:
            # Try searching by fleet number if plate search fails
            if custom_num:
                time.sleep(0.3)
                slug = search_buspedia(custom_num)
        if not slug:
            print("NOT FOUND on buspedia")
            stats["not_found"] += 1
            continue

        print(f"found (slug={slug})")

        # Fetch detail
        time.sleep(DETAIL_DELAY)
        fresh = scrape_bus_detail(slug)
        if not fresh:
            print(f"         API error fetching detail")
            stats["api_error"] += 1
            continue

        stats["found"] += 1

        # Compare and update
        changes = diff_config(old_config, fresh)
        date_status = update_vehicle_fields(cursor, vehicle_id, fresh, dry_run)
        if changes:
            status = update_vehicle_config(cursor, vehicle_id, config_id, fresh, dry_run)
            stats["config_updated"] += 1
            print(f"         UPDATED config ({len(changes)} field(s) changed):")
            for label, old_val, new_val in changes:
                old_display = old_val if old_val else "(empty)"
                new_display = new_val if new_val else "(empty)"
                print(f"           {label}: [{old_display}] → [{new_display}]")
            stats["details"].append(
                {
                    "vehicle_id": vehicle_id,
                    "plate": plate,
                    "slug": slug,
                    "status": "updated",
                    "changes": [
                        {"field": c[0], "old": c[1], "new": c[2]} for c in changes
                    ],
                }
            )
        elif date_status and date_status != "no_changes":
            stats["config_updated"] += 1
            print(f"         UPDATED dates only")
        else:
            stats["config_unchanged"] += 1
            print(f"         unchanged (config matches buspedia)")

    # Commit
    if not dry_run and stats["config_updated"] > 0:
        print()
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Committing changes...")
        db.commit()
        print("  Commit done.")
    elif dry_run and stats["config_updated"] > 0:
        print()
        print("  DRY RUN — no changes committed.")

    # Summary
    print()
    print("=" * 60)
    print("  SUMMARY")
    print("=" * 60)
    print(f"  Total vehicles processed:  {stats['total']}")
    print(f"  Searched on buspedia:      {stats['searched']}")
    print(f"  Found on buspedia:         {stats['found']}")
    print(f"  Not found on buspedia:     {stats['not_found']}")
    print(f"  API errors:                {stats['api_error']}")
    print(f"  Configs updated:           {stats['config_updated']}")
    print(f"  Configs unchanged:         {stats['config_unchanged']}")
    print()

    if stats["details"]:
        print("  Changed vehicles:")
        for d in stats["details"]:
            print(f"    #{d['vehicle_id']} {d['plate']} ({d['slug']})")
            for c in d["changes"]:
                print(f"      {c['field']}: {c['old'][:50]} → {c['new'][:50]}")

    cursor.close()
    db.close()


if __name__ == "__main__":
    main()
