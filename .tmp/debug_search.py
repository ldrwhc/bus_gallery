import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

r = requests.get(f'{base}/vehicles', params={'keyword': 'CK6120LGEV', 'size': 5}, verify=False, timeout=15)
d = r.json()
print('Response type:', type(d).__name__)
if isinstance(d, dict):
    print('Keys:', list(d.keys())[:10])
    print('Total:', d.get('total'))
    records = d.get('records') or d.get('data') or []
    print('Records count:', len(records))
    if records:
        print('First record keys:', list(records[0].keys())[:10])
        print('First record:', json.dumps(records[0], ensure_ascii=False, default=str)[:400])

# Try the search endpoint instead
print()
print('=== Search API ===')
r2 = requests.get(f'{base}/search', params={'keyword': 'CK6120LGEV', 'scope': 'vehicles'}, verify=False, timeout=15)
d2 = r2.json()
if isinstance(d2, dict):
    vehicles = d2.get('vehicles', {})
    if isinstance(vehicles, dict):
        print('Vehicle search total:', vehicles.get('total'))
        items = vehicles.get('items', [])
        print('Items count:', len(items))
        for item in items[:5]:
            print(f'  {item.get("title")} - {item.get("subtitle")}')
