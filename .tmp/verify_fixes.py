import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Fix 2: Search CK6120LGEV should now find 陕A L8500
print('=== Fix 2: Search CK6120LGEV ===')
r = requests.get(f'{base}/vehicles', params={'keyword': 'CK6120LGEV', 'size': 20}, verify=False, timeout=15)
d = r.json()
records = d.get('records') or d.get('data') or []
total = d.get('total', len(records))
plates = set()
for v in (records or []):
    p = v.get('plateNumber', '')
    if p: plates.add(p)
print(f'  Total results: {total}')
print(f'  Unique plates: {len(plates)}')
found_xian = any('陕A' in p for p in plates)
print(f'  Has Shaanxi plate: {found_xian}')
if plates:
    for p in sorted(plates)[:10]:
        print(f'    {p}')

# Check the plate lookup for CK6120LGEV model
print()
print('=== Plate lookup for CK model ===')
r2 = requests.get(f'{base}/vehicles/plate/%E9%99%95A%20L8500', verify=False, timeout=15)
pd = r2.json()
variants = pd.get('variants', [])
for var in variants:
    v = var.get('vehicle', {})
    m = v.get('model', {}) or {}
    c = v.get('company', {}) or {}
    rgn = v.get('region', {}) or {}
    print(f'  plate={v.get("plateNumber")}')
    print(f'  model={m.get("name")} brand={m.get("brandName")} ({m.get("brandChnName")})')
    print(f'  company={c.get("name")} region={rgn.get("name")}')
