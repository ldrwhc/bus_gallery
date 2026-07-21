import requests, json
requests.packages.urllib3.disable_warnings()

base = 'https://192.144.227.251/api'

# Check CK models
r = requests.get(f'{base}/models', params={'keyword': 'CK'}, verify=False, timeout=15)
data = r.json()
models = []
if isinstance(data, list):
    models = data
elif isinstance(data, dict):
    models = data.get('records') or data.get('data') or data.get('items') or []

print('=== CK models ===')
for m in (models or [])[:15]:
    brand = m.get('brand')
    brand_name = brand.get('name') if isinstance(brand, dict) else brand
    print(f'  id={m.get("id")} name={m.get("name")} brand_name={brand_name}')
print(f'  Total: {len(models) if models else 0}')

# Check brands
r2 = requests.get(f'{base}/brands', verify=False, timeout=15)
brands = r2.json()
if isinstance(brands, dict):
    brands = brands.get('records') or brands.get('data') or brands
print('\n=== All brands ===')
for b in (brands or []):
    print(f'  id={b.get("id")} name={b.get("name")} chnName={b.get("chnName")}')

# Check plate lookup for CK model
r3 = requests.get(f'{base}/vehicles/plate/%E9%99%95A%20L8500', verify=False, timeout=15)
plate_data = r3.json()
print('\n=== Plate lookup: 陕A L8500 ===')
if isinstance(plate_data, list):
    for v in plate_data:
        c = v.get('company') or {}
        rgn = v.get('region') or {}
        m = v.get('model') or {}
        b = m.get('brand') or {} if isinstance(m, dict) else {}
        print(f'  id={v.get("id")} plate={v.get("plateNumber")}')
        print(f'  model={m.get("name") if isinstance(m,dict) else m} brand={b.get("name") if isinstance(b,dict) else b}')
        print(f'  company={c.get("name") if isinstance(c,dict) else c}')
        print(f'  region={rgn.get("name") if isinstance(rgn,dict) else rgn}')
else:
    print(json.dumps(plate_data, ensure_ascii=False, indent=2)[:500])
