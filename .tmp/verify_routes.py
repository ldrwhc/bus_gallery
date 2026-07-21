import requests
requests.packages.urllib3.disable_warnings()

r = requests.get('https://192.144.227.251/api/stats', verify=False, timeout=15)
d = r.json()
print('=== Stats routes ===')
for item in (d.get('mostImagedRoutes') or [])[:3]:
    print(f'  name={item["name"]}')

r2 = requests.get('https://192.144.227.251/api/search/suggest', params={'keyword':'313'}, verify=False, timeout=15)
sugs = r2.json() if isinstance(r2.json(), list) else []
print('\n=== Search suggest ===')
for s in sugs[:5]:
    if s.get('type') == 'route':
        print(f'  {s["value"]}')

print('\n=== Search results ===')
r3 = requests.get('https://192.144.227.251/api/search', params={'keyword':'313','scope':'routes'}, verify=False, timeout=15)
sr = r3.json()
routes = sr.get('routes', {}).get('items', [])
for rt in routes[:3]:
    print(f'  title={rt["title"]}  subtitle={rt.get("subtitle")}')
