import requests, json
requests.packages.urllib3.disable_warnings()
r = requests.get('https://192.144.227.251/api/stats', verify=False, timeout=15)
d = r.json()

for k, v in d.items():
    if k == 'overview':
        print(f'{k}: {len(v)} fields')
    elif isinstance(v, list):
        print(f'{k}: {len(v)} items')
    else:
        print(f'{k}: {type(v).__name__}')

print()
print('--- mostImagedModels (top 3) ---')
for item in d['mostImagedModels'][:3]:
    print(f'  {item["name"]} ({item.get("subName","")}): {item["cnt"]} images')

print()
print('--- mostVehiclesByBrand (top 3) ---')
for item in d['mostVehiclesByBrand'][:3]:
    print(f'  {item["name"]} ({item.get("subName","")}): {item["cnt"]} vehicles')

print()
print('--- fuelTypeDistribution ---')
for item in d['fuelTypeDistribution']:
    print(f'  {item["name"]}: {item["cnt"]}')

print()
print('--- oldestVehicles ---')
for item in d['oldestVehicles']:
    print(f'  {item["plateNumber"]} ({item["modelName"]}): {item["dateVal"]}')

print()
print('--- latestImages (top 3) ---')
for item in d['latestImages'][:3]:
    print(f'  #{item["id"]} by {item["uploaderName"]} at {item["createdAt"]}')
