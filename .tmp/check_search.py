import requests, json
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Check: model CK6120LGEV - does it have Xi'an vehicles?
r = requests.get(f'{base}/models/97/vehicles', verify=False, timeout=15)
d = r.json()
print('=== Model 97 (CK6120LGEV) vehicles ===')
items = []
if isinstance(d, list): items = d
elif isinstance(d, dict): items = d.get('records') or d.get('data') or d.get('items') or []
for v in (items or [])[:10]:
    c = v.get('company') or {}
    rgn = v.get('region') or {}
    m = v.get('model') or {}
    b = (m.get('brand') or {}) if isinstance(m, dict) else {}
    print(f'  {v.get("plateNumber")} company={c.get("name")} region={rgn.get("name")} brand={b.get("name")}')
print(f'  Total: {len(items) if items else 0}')

# Check: search for CK6120LGEV with region filter
r2 = requests.get(f'{base}/vehicles', params={'keyword': 'CK6120LGEV', 'size': 20}, verify=False, timeout=15)
d2 = r2.json()
records = []
if isinstance(d2, list): records = d2
elif isinstance(d2, dict): records = d2.get('records') or d2.get('data') or []
print(f'\n=== Vehicle gallery search CK6120LGEV ({len(records) if records else 0} results) ===')
for v in (records or [])[:20]:
    c = v.get('company') or {}
    rgn = v.get('region') or {}
    print(f'  {v.get("plateNumber")} company_id={v.get("company_id")} model_id={v.get("model_id")}')

# Check company 28 (公交五公司)
r3 = requests.get(f'{base}/companies/28', verify=False, timeout=15)
c3 = r3.json()
print(f'\n=== Company 28 ===')
if isinstance(c3, dict):
    rgn = c3.get('region') or {}
    print(f'  name={c3.get("name")} region={rgn.get("name")}')

# Check all companies with region 35 (西安)
r4 = requests.get(f'{base}/companies', params={'regionId': 35}, verify=False, timeout=15)
companies = r4.json()
co_list = []
if isinstance(companies, list): co_list = companies
elif isinstance(companies, dict): co_list = companies.get('records') or companies.get('data') or []
print(f'\n=== Companies in region 35 (Xian) ===')
for c in co_list:
    print(f'  id={c.get("id")} name={c.get("name")}')
