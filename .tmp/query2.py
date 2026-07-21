import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Get the model CK6120LGEV detail
r = requests.get(f'{base}/models/97', verify=False, timeout=15)
m = r.json()
print('=== Model CK6120LGEV (id=97) ===')
if isinstance(m, dict):
    brand = m.get('brand') or {}
    print(f'  name={m.get("name")}')
    print(f'  brand_id={brand.get("id")} brand_name={brand.get("name")} brand_chn={brand.get("chnName")}')
else:
    print(json.dumps(m, ensure_ascii=False, indent=2)[:500])

# Check all models starting with CK
r2 = requests.get(f'{base}/models', verify=False, timeout=15)
data2 = r2.json()
all_models = []
if isinstance(data2, list): all_models = data2
elif isinstance(data2, dict): all_models = data2.get('records') or data2.get('data') or []
ck_models = [m for m in all_models if m.get('name','').upper().startswith('CK')]
print(f'\n=== CK-prefix models (found {len(ck_models)}) ===')
for m in ck_models:
    brand = m.get('brand') or {}
    print(f'  id={m.get("id")} name={m.get("name")} brand_id={brand.get("id")} brand_name={brand.get("name")}')

# Get full plate lookup for 陕A L8500
r3 = requests.get(f'{base}/vehicles/plate/%E9%99%95A%20L8500', verify=False, timeout=15)
pd = r3.json()
print('\n=== Full vehicle data for 陕A L8500 ===')
variants = pd.get('variants', [])
for var in variants:
    v = var.get('vehicle', {})
    m = v.get('model', {}) or {}
    b = (m.get('brand') or {}) if isinstance(m, dict) else {}
    c = v.get('company', {}) or {}
    rgn = v.get('region', {}) or {}
    print(f'  id={v.get("id")}')
    print(f'  plate={v.get("plateNumber")} customNumber={v.get("customNumber")}')
    print(f'  model={m.get("name")} (id={m.get("id")})')
    print(f'  brand={b.get("name")} chnName={b.get("chnName")}')
    print(f'  company={c.get("name")} (id={c.get("id")})')
    print(f'  region={rgn.get("name")} (id={rgn.get("id")})')
