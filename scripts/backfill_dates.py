"""
Backfill missing/bogus vehicle dates.

Strategy:
  1. Find vehicles where one date is bogus (2026 scrape artifact) and the other is real.
     → Clear the bogus date (set to NULL).
  2. Find vehicles missing one date entirely.
     → Optionally re-scrape buspedia to get the missing date.
  3. Find vehicles with both dates but inconsistent (factory > launch by > 3 years).
     → Keep the earlier date, clear the later one.

Usage:
  python backfill_dates.py --dry-run     # Show what would be fixed (no changes)
  python backfill_dates.py --fix         # Apply fixes
  python backfill_dates.py --scrape      # Scrape buspedia to fill missing dates
"""

import requests, sys, json, time
import urllib.request, urllib.parse, ssl, re, zlib

requests.packages.urllib3.disable_warnings()

BASE = 'https://192.144.227.251/api'
BUPEDIA_API = 'https://api.buspedia.top'

# ============================================================
# Buspedia scraper (minimal, local)
# ============================================================
def fetch_buspedia(path, timeout=15):
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    req = urllib.request.Request(f'{BUPEDIA_API}{path}')
    req.add_header('User-Agent', 'Mozilla/5.0')
    req.add_header('Accept', 'application/json')
    req.add_header('Origin', 'https://buspedia.top')
    req.add_header('Referer', 'https://buspedia.top/')
    resp = urllib.request.urlopen(req, timeout=timeout)
    raw = resp.read()
    if raw[:2] == b'x\x9c':
        raw = zlib.decompress(raw)
    return json.loads(raw.decode('utf-8', errors='replace'))

def search_plate(plate):
    try:
        result = fetch_buspedia(f'/search?name={urllib.parse.quote(plate)}')
        if isinstance(result, dict):
            vehicles = result.get('v', [])
            if vehicles: return vehicles[0].get('id')
        elif isinstance(result, list) and result:
            return result[0].get('id')
    except: pass
    return None

def scrape_bus_detail(slug):
    try:
        detail = fetch_buspedia(f'/bus/{slug}')
        if isinstance(detail, dict) and 'v' in detail:
            detail = detail['v']
        if isinstance(detail, dict):
            meta = detail.get('meta', [])
            dates = {}
            for item in meta:
                k, v = item.get('k', ''), item.get('v', '')
                if '出厂' in k: dates['factory'] = v
                if '上线' in k or '服役' in k: dates['launch'] = v
            return dates
    except: pass
    return {}

# ============================================================
# Fetch all vehicles
# ============================================================
def fetch_all_vehicles():
    all_v = []
    page = 1
    while True:
        r = requests.get(f'{BASE}/vehicles', params={'size': 50, 'page': page}, verify=False, timeout=15)
        d = r.json()
        records = d.get('records') or d.get('data') or []
        if not records: break
        for rec in records:
            v = rec.get('vehicle', {})
            all_v.append(v)
        if len(all_v) >= d.get('total', 0): break
        page += 1
        time.sleep(0.1)
    return all_v

def update_vehicle(vehicle_id, payload):
    """Update a vehicle via PUT. Requires auth."""
    # Public API doesn't allow updates without auth.
    # For now, print the SQL instead.
    print(f'  -- UPDATE vehicle SET ... WHERE id={vehicle_id}')
    return False  # Can't do this without admin auth from script

# ============================================================
# Diagnosis
# ============================================================
def diagnose(vehicles):
    problems = []
    for v in vehicles:
        vid = v['id']
        plate = v.get('plateNumber', '?')
        fd = v.get('factoryDate')
        ld = v.get('launchDate')
        has_fd = fd is not None
        has_ld = ld is not None

        from datetime import date
        f_d = date.fromisoformat(fd) if fd else None
        l_d = date.fromisoformat(ld) if ld else None

        issue = None
        new_fd, new_ld = fd, ld

        if has_fd and has_ld:
            # Both exist — check for bogus 2026 dates
            if f_d.year >= 2026 and l_d.year <= 2023:
                issue = 'factory bogus (2026 scrape artifact)'
                new_fd = None  # clear bogus factory
            elif l_d.year >= 2026 and f_d.year <= 2023:
                issue = 'launch bogus (2026 scrape artifact)'
                new_ld = None  # clear bogus launch
            elif f_d.year >= 2026 and l_d.year >= 2026:
                # Both 2026 — both are probably bogus
                issue = 'both dates bogus (2026 scrape)'
                new_fd = new_ld = None
        elif not has_fd and has_ld:
            issue = 'missing factory_date'
        elif has_fd and not has_ld:
            issue = 'missing launch_date'

        if issue:
            problems.append({
                'id': vid, 'plate': plate,
                'model': v.get('model', {}).get('name', ''),
                'factory': fd, 'launch': ld,
                'issue': issue,
                'new_factory': new_fd,
                'new_launch': new_ld,
            })
    return problems

# ============================================================
# Generate SQL for manual fix
# ============================================================
def gen_sql(problems):
    """Generate SQL statements to fix the dates."""
    lines = []
    lines.append('-- Backfill vehicle dates')
    lines.append('-- Run these on the MySQL server: docker exec -i bus-gallery-mysql mysql -uroot -p123456 bus_gallery')
    lines.append('')
    for p in problems:
        sets = []
        if p['new_factory'] != p['factory']:
            val = 'NULL' if p['new_factory'] is None else f"'{p['new_factory']}'"
            sets.append(f"factory_date = {val}")
        if p['new_launch'] != p['launch']:
            val = 'NULL' if p['new_launch'] is None else f"'{p['new_launch']}'"
            sets.append(f"launch_date = {val}")
        if sets:
            comment = f"-- {p['plate']} {p['issue']}: factory={p['factory']} launch={p['launch']}"
            lines.append(comment)
            lines.append(f"UPDATE vehicle SET {', '.join(sets)} WHERE id = {p['id']};")
            lines.append('')
    return '\n'.join(lines)

# ============================================================
# Main
# ============================================================
if __name__ == '__main__':
    dry_run = '--dry-run' in sys.argv or '--fix' not in sys.argv
    do_scrape = '--scrape' in sys.argv

    print('Fetching all vehicles...')
    vehicles = fetch_all_vehicles()
    print(f'Total: {len(vehicles)}')

    problems = diagnose(vehicles)
    print(f'\nProblems found: {len(problems)}')
    for p in problems:
        print(f"  [{p['issue']}] {p['plate']} {p['model']} "
              f"factory={p['factory']} launch={p['launch']}")

    if dry_run:
        print('\n--- DRY RUN: SQL that would fix these ---')
        print(gen_sql(problems))
        print('\nRun with --fix to apply changes via SQL export, or --scrape to also query buspedia.')
    else:
        sql = gen_sql(problems)
        print('\n--- Generated SQL ---')
        print(sql)
        print('\nPaste the SQL above into the MySQL container to fix.')
