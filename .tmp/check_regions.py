import urllib.request, zlib, json

# Search for a common route to see all response keys including regions
url = 'https://api.buspedia.top/search?name=1%E7%8E%AF'
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0', 'Accept': 'application/json',
    'Origin': 'https://buspedia.top', 'Referer': 'https://buspedia.top/'
})
raw = urllib.request.urlopen(req).read()
data = zlib.decompress(raw[2:], -15) if raw[0] == 0x78 else raw
j = json.loads(data)

# Look for region/city data
for key in j:
    val = j[key]
    if isinstance(val, list) and val:
        print(f'Key "{key}": {len(val)} items, first: {json.dumps(val[0], ensure_ascii=False)[:200]}')
    elif isinstance(val, dict):
        print(f'Key "{key}": dict with keys {list(val.keys())[:10]}')

# Also try getting detail of an Anshan route to see region mapping
print('\n--- Anshan 1环 detail ---')
url2 = 'https://api.buspedia.top/bus/pf80al'
req2 = urllib.request.Request(url2, headers={
    'User-Agent': 'Mozilla/5.0', 'Accept': 'application/json',
    'Origin': 'https://buspedia.top', 'Referer': 'https://buspedia.top/'
})
raw2 = urllib.request.urlopen(req2).read()
data2 = zlib.decompress(raw2[2:], -15) if raw2[0] == 0x78 else raw2
j2 = json.loads(data2)
for key in j2:
    val = j2[key]
    if isinstance(val, dict):
        print(f'Key "{key}": {json.dumps(val, ensure_ascii=False)[:300]}')
    elif isinstance(val, list) and val:
        print(f'Key "{key}": [{len(val)} items]')
