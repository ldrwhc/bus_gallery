import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Step 1: Create CK brand (name=CK, chnName=比亚迪)
print('=== Create CK brand ===')
payload = {'brand': {'name': 'CK', 'chnName': '比亚迪'}}
r = requests.post(f'{base}/brands', json=payload, verify=False, timeout=10)
if r.status_code == 200:
    ck_brand = r.json()
    ck_id = ck_brand['id']
    print(f'  CK brand created: id={ck_id}')
else:
    # Already exists? Find it
    r2 = requests.get(f'{base}/brands', verify=False, timeout=10)
    brands = r2.json() if isinstance(r2.json(), list) else r2.json().get('records', [])
    ck = [b for b in brands if b.get('name') == 'CK']
    if ck:
        ck_id = ck[0]['id']
        print(f'  CK brand already exists: id={ck_id}')
    else:
        print(f'  Failed to create CK brand: {r.status_code} {r.text[:200]}')
        exit(1)

# Step 2: Find CK models (currently under BYD)
print('\n=== CK models to fix ===')
r3 = requests.get(f'{base}/models', verify=False, timeout=10)
all_models = r3.json() if isinstance(r3.json(), list) else r3.json().get('records', [])
ck_models = [m for m in all_models if m.get('name','').upper().startswith('CK')]
for m in ck_models:
    b = m.get('brand') or {}
    print(f'  id={m["id"]} name={m["name"]} current_brand={b.get("name")}({b.get("chnName")})')

# Step 3: Update CK models to CK brand
print(f'\n=== Moving {len(ck_models)} models to CK brand (id={ck_id}) ===')
for m in ck_models:
    update = {'name': m['name'], 'brand': {'id': ck_id}}
    r4 = requests.put(f'{base}/models/{m["id"]}', json=update, verify=False, timeout=10)
    status = 'OK' if r4.status_code == 200 else f'FAIL({r4.status_code})'
    updated = r4.json()
    new_brand = (updated.get('brand') or {}) if r4.status_code == 200 else {}
    print(f'  id={m["id"]} {m["name"]} → brand={new_brand.get("name")}({new_brand.get("chnName")}) [{status}]')

# Step 4: Verify
print('\n=== Verify ===')
r5 = requests.get(f'{base}/catalog/brands', verify=False, timeout=10)
catalog = r5.json()
for b in catalog:
    if b.get('name') in ('CK', 'BYD', 'ZK'):
        models = b.get('models') or []
        print(f'  Brand {b["name"]}({b.get("chnName")}): {len(models)} models')
        for mdl in models[:5]:
            print(f'    - {mdl["name"]}')
