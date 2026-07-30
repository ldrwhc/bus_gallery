#!/usr/bin/env python3
"""
Run ON THE SERVER to scrape AC models from buspedia.top and update local MySQL.
Copy to server and run: python3 /tmp/scrape_ac.py [--dry-run] [--limit N]
"""
import sys, re, json, zlib, urllib.parse, urllib.request, ssl, time, argparse, subprocess

API_BASE = 'https://api.buspedia.top'

def mysql_exec(sql, timeout=30):
    """Execute SQL via docker exec, returns stdout lines."""
    cmd = ['docker', 'exec', 'bus-gallery-mysql', 'mysql', '-uroot', '-p123456',
           '--default-character-set=utf8mb4', '-N', '-B', 'bus_gallery', '-e', sql]
    try:
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        if result.returncode != 0:
            err = result.stderr.decode('utf-8', errors='replace')
            if 'Warning' not in err and err.strip():
                print(f"  SQL Error: {err}", file=sys.stderr)
        raw = result.stdout
        return raw.decode('utf-8', errors='replace')
    except subprocess.TimeoutExpired:
        print(f"  SQL timeout", file=sys.stderr)
        return ''

def mysql_exec_no_output(sql, timeout=30):
    """Execute SQL, return True on success."""
    cmd = ['docker', 'exec', 'bus-gallery-mysql', 'mysql', '-uroot', '-p123456',
           '--default-character-set=utf8mb4', 'bus_gallery', '-e', sql]
    try:
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        err = result.stderr.decode('utf-8', errors='replace')
        if result.returncode != 0 and 'Warning' not in err and err.strip():
            print(f"  SQL Error: {err}", file=sys.stderr)
            return False
        return True
    except subprocess.TimeoutExpired:
        return False

def fetch_api(path, timeout=15):
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    req = urllib.request.Request(f'{API_BASE}{path}')
    req.add_header('User-Agent', 'Mozilla/5.0')
    req.add_header('Accept', 'application/json')
    req.add_header('Origin', 'https://buspedia.top')
    req.add_header('Referer', 'https://buspedia.top/')
    resp = urllib.request.urlopen(req, timeout=timeout)
    raw = resp.read()
    if raw[:2] == b'x\x9c':
        raw = zlib.decompress(raw)
    return json.loads(raw.decode('utf-8', errors='replace'))

def search_buspedia(plate):
    try:
        result = fetch_api(f'/search?name={urllib.parse.quote(plate)}')
        if isinstance(result, dict):
            vehicles = result.get('v', [])
            if vehicles and isinstance(vehicles, list) and len(vehicles) > 0:
                first = vehicles[0]
                return first.get('id') or first.get('uid') or first.get('slug')
    except Exception as e:
        pass
    return None

def scrape_vehicle_ac(slug):
    try:
        data = fetch_api(f'/bus/{slug}')
        veh = data.get('veh', {})
        ac_val = veh.get('ac')
        if ac_val is None:
            return None, None
        ac_str = str(ac_val).strip()
        if ac_str in ('1', 'true', 'True'):
            return True, None
        elif ac_str in ('0', 'false', 'False', ''):
            return False, None
        else:
            return True, ac_str
    except:
        return None, None

def normalize_plate(plate):
    return re.sub(r'\s+', '', str(plate or '')).strip().upper()

def add_space_to_plate(plate):
    plate = normalize_plate(plate)
    if len(plate) >= 3:
        return plate[:2] + ' ' + plate[2:]
    return plate

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    parser.add_argument('--limit', type=int, default=0)
    parser.add_argument('--offset', type=int, default=0)
    parser.add_argument('--delay', type=float, default=0.5, help='Delay between API calls (seconds)')
    args = parser.parse_args()

    # Get vehicle list
    query = "SELECT id, plate_number, air_conditioned, air_conditioner_model FROM vehicle ORDER BY id"
    if args.limit > 0:
        query += f" LIMIT {args.limit}"
    if args.offset > 0:
        query += f" OFFSET {args.offset}"

    print("Fetching vehicle list...")
    raw = mysql_exec(query)
    lines = [l.strip() for l in raw.split('\n') if l.strip()]

    vehicles = []
    for line in lines:
        parts = line.split('\t')
        if len(parts) >= 2:
            try:
                vid = int(parts[0])
                plate = parts[1]
                existing_ac = int(parts[2]) if parts[2] and parts[2] != 'NULL' else None
                existing_model = parts[3] if len(parts) >= 4 and parts[3] and parts[3] != 'NULL' else None
                vehicles.append({'id': vid, 'plate': plate, 'ac': existing_ac, 'model': existing_model})
            except ValueError:
                pass

    print(f"Found {len(vehicles)} vehicles")

    updated = skipped = no_ac = not_found = 0
    start = time.time()

    for i, v in enumerate(vehicles):
        vid, plate = v['id'], v['plate']
        if not plate:
            skipped += 1
            continue

        elapsed = time.time() - start
        rate = (i + 1) / max(elapsed, 1)
        remaining = (len(vehicles) - i - 1) / max(rate, 0.01)

        search_plate = add_space_to_plate(plate)
        slug = search_buspedia(search_plate)
        if not slug:
            slug = search_buspedia(normalize_plate(plate))

        if not slug:
            print(f"[{i+1}/{len(vehicles)}] {plate} NOT_FOUND")
            not_found += 1
            continue

        has_ac, ac_model = scrape_vehicle_ac(slug)

        if has_ac is None:
            if (i+1) % 20 == 0:
                print(f"[{i+1}/{len(vehicles)}] {plate} no_ac_field  | u:{updated} s:{skipped} nf:{not_found} na:{no_ac} eta:{remaining:.0f}s")
            no_ac += 1
            time.sleep(args.delay)
            continue

        if has_ac is False:
            if v['ac'] == 0:
                skipped += 1
            else:
                print(f"[{i+1}/{len(vehicles)}] {plate} -> no_ac (was {v['ac']})")
                if not args.dry_run:
                    mysql_exec_no_output(f"UPDATE vehicle SET air_conditioned=0, air_conditioner_model=NULL WHERE id={vid}")
                updated += 1
        else:
            if ac_model:
                if v['model'] == ac_model and v['ac'] == 1:
                    skipped += 1
                else:
                    print(f"[{i+1}/{len(vehicles)}] {plate} -> {ac_model}")
                    if not args.dry_run:
                        escaped = ac_model.replace('\\', '\\\\').replace("'", "\\'")
                        mysql_exec_no_output(f"UPDATE vehicle SET air_conditioned=1, air_conditioner_model='{escaped}' WHERE id={vid}")
                    updated += 1
            else:
                if v['ac'] == 1 and v['model'] is None:
                    skipped += 1
                else:
                    print(f"[{i+1}/{len(vehicles)}] {plate} -> ac=yes (no model)")
                    if not args.dry_run:
                        mysql_exec_no_output(f"UPDATE vehicle SET air_conditioned=1, air_conditioner_model=NULL WHERE id={vid}")
                    updated += 1

        if (i+1) % 20 == 0:
            print(f"[{i+1}/{len(vehicles)}]  | u:{updated} s:{skipped} nf:{not_found} na:{no_ac} eta:{remaining:.0f}s")

        time.sleep(args.delay)

    elapsed_total = time.time() - start
    print(f"\nDone! {elapsed_total:.1f}s  Updated:{updated} Skipped:{skipped} NotFound:{not_found} NoACField:{no_ac}")
    if args.dry_run:
        print("** DRY RUN **")

if __name__ == '__main__':
    main()
