import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Step 1: Check if ZK brand exists
print('=== Step 1: Check existing ZK brand ===')
r = requests.get(f'{base}/brands', verify=False, timeout=10)
brands = r.json() if isinstance(r.json(), list) else r.json().get('records', [])
zk = [b for b in brands if b.get('name') == 'ZK']
print(f'  ZK brand exists: {len(zk) > 0}')
if zk:
    print(f'  id={zk[0].get("id")} name={zk[0].get("name")} chnName={zk[0].get("chnName")}')

# Step 2: Create ZK brand via POST
print('\n=== Step 2: Create ZK brand ===')
brand_payload = {'name': 'ZK', 'chnName': '宇通'}
r2 = requests.post(f'{base}/brands', json=brand_payload, verify=False, timeout=10)
print(f'  Status: {r2.status_code}')
print(f'  Response: {r2.text[:300]}')

# Step 3: Create model ZK6125HNG2
if r2.status_code == 200:
    brand_data = r2.json()
    brand_id = brand_data.get('id')
    print(f'\n=== Step 3: Create model ZK6125HNG2 under brand {brand_id} ===')
    model_payload = {'name': 'ZK6125HNG2', 'brandId': brand_id}
    r3 = requests.post(f'{base}/models', json=model_payload, verify=False, timeout=10)
    print(f'  Status: {r3.status_code}')
    print(f'  Response: {r3.text[:300]}')

# Step 4: Verify model exists
print('\n=== Step 4: Verify ===')
r4 = requests.get(f'{base}/models', params={'keyword': 'ZK6125HNG2'}, verify=False, timeout=10)
models = r4.json()
if isinstance(models, list):
    found = models
elif isinstance(models, dict):
    found = models.get('records') or models.get('data') or []
for m in (found or []):
    b = m.get('brand') or {}
    print(f'  id={m.get("id")} name={m.get("name")} brand={b.get("name")}({b.get("chnName")})')
if not found:
    print('  Model NOT found')
