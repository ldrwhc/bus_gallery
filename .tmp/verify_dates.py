import json, zlib, urllib.request, urllib.parse, ssl, time

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch(path):
    req = urllib.request.Request(f'https://api.buspedia.top{path}')
    req.add_header('User-Agent', 'Mozilla/5.0 BusGallery/1.0')
    req.add_header('Accept', 'application/json')
    req.add_header('Origin', 'https://buspedia.top')
    req.add_header('Referer', 'https://buspedia.top/')
    resp = urllib.request.urlopen(req, timeout=15)
    raw = resp.read()
    if raw[:2] == b'x\x9c':
        raw = zlib.decompress(raw)
    return json.loads(raw.decode('utf-8', errors='replace'))

all_plates = [
    ("辽B K9755", "CK6120LGEV2"),
    ("辽B K9780", "CK6120LGEV2"),
    ("辽B L4957", "CK6120LGEV2"),
    ("辽B L5227", "CK6120LGEV2"),
    ("辽B L5458", "CK6120LGEV2"),
    ("辽B L3781", "CK6120LGEV2"),
    ("辽B L4370", "CK6120LGEV2"),
    ("冀H Z0166", "CK6120LGEV2"),
    ("冀H Z6392", "CK6120LGEV2"),
    ("冀H Z7909", "CK6120LGEV2"),
    ("冀H Z1811", "CK6120LGEV2"),
    ("苏A 19980D", "CK6120LGEV2"),
    ("苏A 15700D", "CK6120LGEV2"),
    ("辽B M5295", "CK6121LGEV"),
    ("辽B M3050", "CK6121LGEV"),
    ("辽B M5203", "CK6121LGEV"),
    ("辽B M3101", "CK6121LGEV"),
    ("辽B M5015", "CK6121LGEV"),
    ("辽B M5009", "CK6121LGEV"),
    ("辽B M5061", "CK6121LGEV"),
    ("辽B M5096", "CK6121LGEV"),
    ("辽B M5215", "CK6121LGEV"),
    ("辽B M5100", "CK6121LGEV"),
    ("浙A 4M143", "CK6121LGEV"),
    ("浙A 4H502", "CK6121LGEV"),
    ("浙A 9J521", "CK6121LGEV"),
    ("浙A 4H100", "CK6121LGEV"),
    ("浙A 4H397", "CK6121LGEV"),
    ("浙A 4H393", "CK6121LGEV"),
    ("浙A 4H438", "CK6121LGEV"),
]

print(f"{'plate':<14} {'model':<14} {'buspedia_factory':<18} {'buspedia_launch':<18} {'db_factory':<18} {'db_launch':<18} {'match':<6}")
print("-" * 130)

# DB dates from previous query (hardcoded for comparison)
db_dates = {
    "辽B K9755": ("2014-01-01", "2014-11-01"),
    "辽B K9780": ("2014-01-01", "2014-11-01"),
    "辽B L4957": ("2014-06-01", "2015-09-01"),
    "辽B L5227": ("2014-06-01", "2015-10-01"),
    "辽B L5458": ("2014-06-01", "2015-11-01"),
    "辽B L3781": ("2014-06-01", "2015-07-01"),
    "辽B L4370": ("2014-06-01", "2015-11-01"),
    "冀H Z0166": ("2014-12-01", "2015-01-01"),
    "冀H Z6392": ("2014-06-01", "2015-11-01"),
    "冀H Z7909": ("2014-06-01", "2015-11-01"),
    "冀H Z1811": ("2014-06-01", "2015-11-01"),
    "苏A 19980D": ("2014-06-01", "2015-11-01"),
    "苏A 15700D": ("2014-06-01", "2015-11-01"),
    "辽B M5295": ("2016-01-01", "2017-09-01"),
    "辽B M3050": ("2016-01-01", "2017-09-01"),
    "辽B M5203": ("2016-01-01", "2017-09-01"),
    "辽B M3101": ("2016-01-01", "2017-09-01"),
    "辽B M5015": ("2016-01-01", "2017-09-01"),
    "辽B M5009": ("2016-01-01", "2017-09-01"),
    "辽B M5061": ("2016-01-01", "2017-09-01"),
    "辽B M5096": ("2016-01-01", "2017-09-01"),
    "辽B M5215": ("2016-01-01", "2017-09-01"),
    "辽B M5100": ("2016-01-01", "2017-09-01"),
    "浙A 4M143": ("2016-01-01", "2017-09-01"),
    "浙A 4H502": ("2016-01-01", "2017-09-01"),
    "浙A 9J521": ("2016-01-01", "2017-09-01"),
    "浙A 4H100": ("2016-01-01", "2017-09-01"),
    "浙A 4H397": ("2016-01-01", "2017-09-01"),
    "浙A 4H393": ("2016-01-01", "2017-09-01"),
    "浙A 4H438": ("2016-01-01", "2017-09-01"),
}

mismatches = []

for plate, model in all_plates:
    time.sleep(1.0)  # rate limit
    try:
        result = fetch(f'/search?name={urllib.parse.quote(plate)}')
        vehicles = result.get('v', [])
        if not vehicles:
            print(f"{plate:<14} {'NOT FOUND':<14}")
            continue

        slug = vehicles[0].get('id')
        detail = fetch(f'/bus/{slug}')
        veh = detail.get('veh', {})

        def pad_date(raw):
            if not raw: return None
            s = str(raw)
            if len(s) >= 10: return s[:10]
            if len(s) == 7: return s + '-01'
            return s + '-01-01'

        bp_factory = pad_date(veh.get('date_manuf')) or ''
        bp_launch = pad_date(veh.get('date_serve')) or ''

        db_f, db_l = db_dates.get(plate, ('?', '?'))

        match = (bp_factory == db_f and bp_launch == db_l)
        if not match:
            mismatches.append((plate, model, bp_factory, bp_launch, db_f, db_l))

        status = 'OK' if match else 'MISMATCH'
        print(f"{plate:<14} {model:<14} {bp_factory:<18} {bp_launch:<18} {db_f:<18} {db_l:<18} {status:<6}")

    except Exception as e:
        print(f"{plate:<14} {'ERROR':<14} {str(e)[:60]}")

print()
print(f"Mismatches: {len(mismatches)}")
for m in mismatches:
    print(f"  {m[0]} ({m[1]}): buspedia=[{m[2]}, {m[3]}] vs db=[{m[4]}, {m[5]}]")
