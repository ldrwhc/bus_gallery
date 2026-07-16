#!/usr/bin/env python3
"""
BusPedia scraper — uses the official API (api.buspedia.top) to fetch structured vehicle data.
Usage: python buspedia_scraper.py <plate_number>
       python buspedia_scraper.py --slug <bus_slug>
Output: JSON with extracted fields matching our database schema.
"""
import sys, re, json, zlib, urllib.parse, urllib.request, ssl


API_BASE = 'https://api.buspedia.top'


def fetch_api(path, timeout=15):
    """Fetch and decompress buspedia API response."""
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
    if raw[:2] == b'x\x9c':  # zlib magic
        raw = zlib.decompress(raw)
    return json.loads(raw.decode('utf-8', errors='replace'))


def search_buspedia(plate):
    """Search buspedia for a plate number, return the bus slug.

    Note: buspedia search requires `?name=` parameter (NOT `?q=`) and
    the plate must include spaces (e.g. "京A R1809" not "京AR1809").
    Response format: {"v": [{"id": "m19pwe", "regist": "...", ...}], ...}
    """
    try:
        # Keep spaces in plate — buspedia search needs them
        result = fetch_api(f'/search?name={urllib.parse.quote(plate)}')
        # Response is {"v": [...], "vh": [...], ...}
        if isinstance(result, dict):
            vehicles = result.get('v', [])
            if vehicles and isinstance(vehicles, list) and len(vehicles) > 0:
                first = vehicles[0]
                return first.get('id') or first.get('uid') or first.get('slug')
        elif isinstance(result, list) and result:
            return result[0].get('slug') or result[0].get('id')
    except:
        pass
    return None


def scrape_bus_detail(slug_or_url):
    """Extract vehicle data from buspedia bus detail API."""
    # Parse slug from URL if needed
    slug = slug_or_url
    if '/' in slug:
        m = re.search(r'/bus/([^/\s?]+)', slug)
        if m:
            slug = m.group(1)
    slug = slug.strip()

    data = fetch_api(f'/bus/{slug}')
    result = {'_slug': slug}

    veh = data.get('veh', {})
    model = data.get('model', {})
    manuf = model.get('manuf', {})

    # Brand — manuf is a list of manufacturer objects
    brand_name = ''
    if isinstance(manuf, list):
        for m in manuf:
            if isinstance(m, dict) and m.get('name'):
                brand_name = m['name']
                break
    elif isinstance(manuf, dict):
        brand_name = manuf.get('name', '') or ''
    result['brandName'] = brand_name

    # Model
    result['modelCode'] = model.get('model', '') or ''

    # Engine / Motor
    if model.get('ice'):
        result['engine'] = model['ice']
    if model.get('motor'):
        result['motor'] = model['motor']
    if model.get('fuel'):
        result['fuelType'] = model['fuel'].replace(',', '+').replace('，', '+').replace(' ', '')
    if model.get('trans'):
        result['transmission'] = model['trans']
    if model.get('suspension'):
        result['suspension'] = model['suspension']
    if model.get('step'):
        result['stepType'] = model['step']

    # Axle and other fields from meta array (e.g. {"item": "车桥品牌", "value": "..."})
    meta = model.get('meta', [])
    if isinstance(meta, list):
        for m in meta:
            if isinstance(m, dict) and m.get('item') == '车桥品牌':
                result['axle'] = m.get('value', '')

    # Vehicle registration
    if veh.get('regist'):
        result['plateNumber'] = veh['regist']
    if veh.get('vin'):
        result['vin'] = veh['vin']
    # Dates: handle both "2014-06" and year-only "2016"
    def pad_date(raw):
        if not raw or not re.match(r'\d{4}', str(raw)):
            return None
        s = str(raw)
        if len(s) >= 10:
            return s[:10]
        if len(s) == 7:
            return s + '-01'
        return s + '-01-01'

    dm = pad_date(veh.get('date_manuf'))
    if dm:
        result['factoryDate'] = dm
    ds = pad_date(veh.get('date_serve'))
    if ds:
        result['launchDate'] = ds

    # Company
    comp = veh.get('comp', {})
    if isinstance(comp, dict) and comp.get('name'):
        result['company'] = comp['name']

    # Region
    region = veh.get('region', {})
    if isinstance(region, dict) and region.get('name'):
        result['regionName'] = region['name']

    # Fleet number — directly on veh object
    if veh.get('no'):
        result['fleetNumber'] = str(veh['no'])

    # Photos
    photos = data.get('photos', {})
    if photos:
        first_year = sorted(photos.keys())[0] if photos else None
        if first_year:
            year_photos = photos[first_year]
            if isinstance(year_photos, list) and year_photos:
                p = year_photos[0]
                if isinstance(p, dict) and p.get('path'):
                    result['photoPath'] = p['path']

    return result


def main():
    if len(sys.argv) < 2:
        print(json.dumps({'error': 'Usage: buspedia_scraper.py <plate> or --slug <slug>'}, ensure_ascii=False))
        sys.exit(1)

    if sys.argv[1] == '--slug' and len(sys.argv) >= 3:
        slug = sys.argv[2]
        result = scrape_bus_detail(slug)
    else:
        plate = ' '.join(sys.argv[1:])
        slug = search_buspedia(plate)
        if not slug:
            search_url = f'https://buspedia.top/search?name={urllib.parse.quote(plate)}'
            print(json.dumps({
                'error': 'NOT_FOUND',
                'searchUrl': search_url,
                'message': f'Could not find bus for plate "{plate}". Please search manually and use --slug.'
            }, ensure_ascii=False))
            sys.exit(0)
        result = scrape_bus_detail(slug)

    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
