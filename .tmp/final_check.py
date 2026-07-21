import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

r = requests.get(f'{base}/vehicles', params={'keyword': 'CK6120LGEV', 'size': 30}, verify=False, timeout=15)
d = r.json()
records = d.get('records', [])
plates = []
for rec in records:
    v = rec.get('vehicle', {})
    p = v.get('plateNumber', '')
    m = v.get('model', {}) or {}
    c = v.get('company', {}) or {}
    rgn = v.get('region', {}) or {}
    plates.append((p, m.get('name'), c.get('name'), rgn.get('name')))

print(f'Total: {d.get("total")} vehicles')
print(f'Page results: {len(records)}')
shaaxi = [p for p in plates if '陕A' in p[0]]
print(f'Shaanxi vehicles found: {len(shaaxi)}')
for p in shaaxi:
    print(f'  {p[0]} | model={p[1]} | company={p[2]} | region={p[3]}')
print()
print('All results:')
for p in sorted(plates):
    print(f'  {p[0]:20s} {p[1]:15s} {p[2]:10s} {p[3]:8s}')
