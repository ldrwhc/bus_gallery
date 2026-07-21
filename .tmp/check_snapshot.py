import requests, json
requests.packages.urllib3.disable_warnings()

base = 'https://192.144.227.251/api'

# Check snapshot endpoint for 陕A L8500
r = requests.get(f'{base}/snapshots/plate/%E9%99%95A%20L8500', verify=False, timeout=15)
print('=== Snapshot: 陕A L8500 ===')
d = r.json()
print(json.dumps(d, ensure_ascii=False, indent=2)[:1500])

# Also check: search for CK6120LGEV
print('\n=== Search: CK6120LGEV ===')
r2 = requests.get(f'{base}/search', params={'keyword': 'CK6120LGEV', 'scope': 'all'}, verify=False, timeout=15)
d2 = r2.json()
print(json.dumps(d2, ensure_ascii=False, indent=2)[:1500])

# Check search for 陕A
print('\n=== Search: 陕A L8500 ===')
r3 = requests.get(f'{base}/search', params={'keyword': '陕A L8500', 'scope': 'all'}, verify=False, timeout=15)
d3 = r3.json()
print(json.dumps(d3, ensure_ascii=False, indent=2)[:1500])
