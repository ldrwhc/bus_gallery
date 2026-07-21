import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# BrandController POST expects { "brand": { "name": "...", "chnName": "..." } }
print('=== Create ZK brand ===')
payload = {'brand': {'name': 'ZK', 'chnName': '宇通'}}
r = requests.post(f'{base}/brands', json=payload, verify=False, timeout=10)
print(f'  Status: {r.status_code}')
print(f'  Response: {r.text[:400]}')

if r.status_code == 200:
    brand_id = r.json().get('id')
    print(f'\n=== Create model ZK6125HNG2 ===')
    # Use the model controller which expects similar nested format
    model_payload = {'name': 'ZK6125HNG2', 'brand': {'id': brand_id}}
    r2 = requests.post(f'{base}/models', json=model_payload, verify=False, timeout=10)
    print(f'  Status: {r2.status_code}')
    print(f'  Response: {r2.text[:400]}')

# Verify
print('\n=== Verify ===')
r3 = requests.get(f'{base}/brands', verify=False, timeout=10)
brands = r3.json() if isinstance(r3.json(), list) else r3.json().get('records', [])
zk = [b for b in brands if b.get('name') == 'ZK']
if zk:
    print(f'  ZK brand: id={zk[0].get("id")} name={zk[0].get("name")} chnName={zk[0].get("chnName")}')

r4 = requests.get(f'{base}/models', verify=False, timeout=10)
models = r4.json() if isinstance(r4.json(), list) else r4.json().get('records', [])
zk_models = [m for m in models if m.get('name') == 'ZK6125HNG2']
if zk_models:
    b = zk_models[0].get('brand') or {}
    print(f'  ZK6125HNG2: id={zk_models[0].get("id")} brand={b.get("name")}({b.get("chnName")})')
else:
    print('  ZK6125HNG2 NOT found in models list')
