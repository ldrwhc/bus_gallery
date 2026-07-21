import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Direct query for model 97 vehicles
r = requests.get(f'{base}/models/97/vehicles', verify=False, timeout=15)
d = r.json()
print('=== Model 97 (CK6120LGEV) vehicles ===')
print(json.dumps(d, ensure_ascii=False, default=str)[:800])

# Check if there's a company/region filter affecting results
print()
print('=== Gallery search: CK6120LGEV unfiltered ===')
r2 = requests.get(f'{base}/vehicles', params={'keyword': 'CK6120LGEV', 'size': 30}, verify=False, timeout=15)
d2 = r2.json()
print(f'Total: {d2.get("total")}')
records = d2.get('records', [])
model_ids = set()
for rec in records:
    v = rec.get('vehicle', {})
    m = v.get('model', {}) or {}
    mid = m.get('id')
    if mid: model_ids.add(mid)
print(f'Unique model IDs found: {model_ids}')
