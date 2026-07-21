import requests
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Direct plate lookup for the two suspected bogus vehicles
for plate in ['%E9%B2%81B%20R1652', '%E9%B2%81B%20R9217']:
    r = requests.get(f'{base}/vehicles/plate/{plate}', verify=False, timeout=15)
    d = r.json()
    variants = d.get('variants', [])
    for var in variants:
        v = var.get('vehicle', {})
        m = v.get('model', {}) or {}
        print(f'{v["plateNumber"]}: factory={v.get("factoryDate")} launch={v.get("launchDate")} model={m.get("name")}')

# Query vehicles with factory_date in 2026
r2 = requests.get(f'{base}/vehicles', params={'size': 100}, verify=False, timeout=15)
d2 = r2.json()
records = d2.get('records', [])
count_2026 = 0
for rec in records:
    v = rec.get('vehicle', {})
    fd = v.get('factoryDate', '')
    ld = v.get('launchDate', '')
    if fd and str(fd).startswith('2026'):
        count_2026 += 1
        print(f'  BOGUS: {v["plateNumber"]} fd={fd} ld={ld}')
print(f'\nVehicles with factory_date=2026 in first page: {count_2026} / {len(records)}')
