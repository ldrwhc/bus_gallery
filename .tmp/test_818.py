import urllib.request, zlib, json

# Search without region first
url = 'https://api.buspedia.top/search?name=818%E8%B7%AF'
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0', 'Accept': 'application/json',
    'Origin': 'https://buspedia.top', 'Referer': 'https://buspedia.top/'
})
raw = urllib.request.urlopen(req).read()
data = zlib.decompress(raw[2:], -15) if raw[0] == 0x78 else raw
j = json.loads(data)
lines = j.get('l', [])
print(f'Search results: {len(lines)}')
for l in lines:
    print('---')
    print('Search:', json.dumps(l, ensure_ascii=False))
    slug = l['id']
    url2 = f'https://api.buspedia.top/bus/{slug}'
    req2 = urllib.request.Request(url2, headers={
        'User-Agent': 'Mozilla/5.0', 'Accept': 'application/json',
        'Origin': 'https://buspedia.top', 'Referer': 'https://buspedia.top/'
    })
    raw2 = urllib.request.urlopen(req2).read()
    data2 = zlib.decompress(raw2[2:], -15) if raw2[0] == 0x78 else raw2
    j2 = json.loads(data2)
    # Print all top-level keys
    print(f'Detail keys: {list(j2.keys())}')
    rt = j2.get('rt', {})
    if rt:
        print(f'rt keys: {list(rt.keys())}')
        for k, v in rt.items():
            if isinstance(v, list):
                print(f'  rt.{k}: [{len(v)} items]')
                if v: print(f'    first: {json.dumps(v[0], ensure_ascii=False)[:200]}')
            elif isinstance(v, dict):
                print(f'  rt.{k}: {json.dumps(v, ensure_ascii=False)[:300]}')
            else:
                print(f'  rt.{k}: {v}')
    print()
