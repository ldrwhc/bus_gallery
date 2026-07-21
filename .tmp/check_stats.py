import requests, re
requests.packages.urllib3.disable_warnings()

r = requests.get('https://192.144.227.251/', verify=False, timeout=10)
match = re.search(r'Stats-[\w-]+\.js', r.text)
if match:
    chunk = match.group(0)
    r2 = requests.get(f'https://192.144.227.251/assets/{chunk}', verify=False, timeout=10)
    print(f'Chunk: {chunk} | Status: {r2.status_code} | Size: {len(r2.text)}')
    checks = ['_tipLines', 'ov-card__tip', 'rank-medal', '#f5f7fb', 'min-width:2px']
    for c in checks:
        print(f'  {c}: {c in r2.text}')
else:
    print('Stats chunk not found in HTML')
