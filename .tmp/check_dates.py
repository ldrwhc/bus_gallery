import requests
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Check newest vehicles for factory vs launch dates
r = requests.get(f'{base}/stats', verify=False, timeout=15)
d = r.json()
print('=== Newest vehicles (both dates) ===')
for item in (d.get('newestVehicles') or [])[:10]:
    print(f'  {item["plateNumber"]} {item["modelName"]} factory={item.get("factoryDate")} launch={item.get("launchDate")} display={item["dateVal"]}')

print()
print('=== Oldest vehicles (both dates) ===')
for item in (d.get('oldestVehicles') or [])[:5]:
    print(f'  {item["plateNumber"]} {item["modelName"]} factory={item.get("factoryDate")} launch={item.get("launchDate")} display={item["dateVal"]}')

# Check how many vehicles are missing each date
from collections import Counter
factory_missing = sum(1 for v in (d.get('newestVehicles') or []) if not v.get('factoryDate'))
launch_missing = sum(1 for v in (d.get('newestVehicles') or []) if not v.get('launchDate'))
print(f'\nOut of {len(d.get("newestVehicles") or [])} newest:')
print(f'  missing factory_date: {factory_missing}')
print(f'  missing launch_date: {launch_missing}')

# Quick overall stats
print('\n=== Quick count: vehicles with only one date ===')
ov = d['overview']
total = ov['totalVehicles']
print(f'  Total vehicles: {total}')
