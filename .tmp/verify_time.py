import requests
requests.packages.urllib3.disable_warnings()
r = requests.get('https://192.144.227.251/api/stats', verify=False, timeout=15)
d = r.json()
print(f'Oldest vehicles: {len(d.get("oldestVehicles") or [])} items')
for item in (d.get('oldestVehicles') or [])[:3]:
    print(f'  {item["plateNumber"]} {item["modelName"]} dateVal={item["dateVal"]}')
print(f'Newest vehicles: {len(d.get("newestVehicles") or [])} items')
for item in (d.get('newestVehicles') or [])[:3]:
    print(f'  {item["plateNumber"]} {item["modelName"]} dateVal={item["dateVal"]}')
