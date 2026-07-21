import urllib.request, zlib, json
url = 'https://api.buspedia.top/search?name=273%E8%B7%AF'
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0',
    'Accept': 'application/json',
    'Origin': 'https://buspedia.top',
    'Referer': 'https://buspedia.top/'
})
raw = urllib.request.urlopen(req).read()
print('Raw first bytes:', raw[:5].hex())
if raw[0] == 0x78:
    data = zlib.decompress(raw[2:], -15)  # strip zlib header, raw deflate
    print(f'Decompressed {len(raw)} -> {len(data)} bytes')
else:
    data = raw
j = json.loads(data)
lines = j.get('l', [])
print(f'Found {len(lines)} lines:')
for l in lines:
    print(json.dumps(l, ensure_ascii=False))
