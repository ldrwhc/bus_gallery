"""Diagnose vehicle date quality via API."""
import requests
requests.packages.urllib3.disable_warnings()
base = 'https://192.144.227.251/api'

# Get all vehicles (paginated)
all_vehicles = []
page = 1
while True:
    r = requests.get(f'{base}/vehicles', params={'size': 50, 'page': page}, verify=False, timeout=15)
    d = r.json()
    records = d.get('records') or d.get('data') or []
    if not records: break
    for rec in records:
        v = rec.get('vehicle', {})
        all_vehicles.append(v)
    total = d.get('total', 0)
    if len(all_vehicles) >= total: break
    page += 1

total = len(all_vehicles)
print(f'Total vehicles: {total}')

# Count problems
missing_both = 0
missing_factory = 0
missing_launch = 0
bogus_factory = 0  # factory > launch by > 3 years
bogus_launch = 0   # launch > factory by > 5 years
samples_bogus_factory = []
samples_missing_factory = []

for v in all_vehicles:
    fd = v.get('factoryDate')
    ld = v.get('launchDate')
    has_fd = fd is not None
    has_ld = ld is not None

    if not has_fd and not has_ld:
        missing_both += 1
    elif not has_fd:
        missing_factory += 1
        if len(samples_missing_factory) < 5:
            samples_missing_factory.append(f"{v['plateNumber']} {v.get('model',{}).get('name','')} launch={ld}")
    elif not has_ld:
        missing_launch += 1

    if has_fd and has_ld:
        from datetime import date
        try:
            f_d = date.fromisoformat(fd)
            l_d = date.fromisoformat(ld)
            if f_d.year >= 2026 and l_d.year <= 2020:
                bogus_factory += 1
                if len(samples_bogus_factory) < 5:
                    samples_bogus_factory.append(f"{v['plateNumber']} {v.get('model',{}).get('name','')} factory={fd} launch={ld}")
        except: pass

print(f'\nMissing BOTH dates: {missing_both}')
print(f'Missing factory_date (has launch): {missing_factory}')
print(f'Missing launch_date (has factory): {missing_launch}')
print(f'Bogus factory_date (2026 factory, pre-2020 launch): {bogus_factory}')

if samples_bogus_factory:
    print(f'\nSample bogus factory_date:')
    for s in samples_bogus_factory:
        print(f'  {s}')
if samples_missing_factory:
    print(f'\nSample missing factory_date:')
    for s in samples_missing_factory:
        print(f'  {s}')
