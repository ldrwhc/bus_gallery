import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

r = requests.get(f'{base}/stats', verify=False, timeout=15)
d = r.json()

print('=== mostImagedVehicles (first 2) ===')
for item in (d.get('mostImagedVehicles') or [])[:2]:
    print(json.dumps(item, ensure_ascii=False, default=str))

print('\n=== mostImagedRoutes (first 2) ===')
for item in (d.get('mostImagedRoutes') or [])[:2]:
    print(json.dumps(item, ensure_ascii=False, default=str))

print('\n=== mostUploadingUsers (first 2) ===')
for item in (d.get('mostUploadingUsers') or [])[:2]:
    print(json.dumps(item, ensure_ascii=False, default=str))
