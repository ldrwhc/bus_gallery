import urllib.request, zlib, json
# Test with region parameter
url = 'https://api.buspedia.top/search?name=1%E7%8E%AF%E8%B7%AF&region=106'
print('Testing with region 106...')
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
for i, l in enumerate(lines):
    print(f'{i}: {json.dumps(l, ensure_ascii=False)}')
    if i >= 3: break

# Also search without region to compare
url2 = 'https://api.buspedia.top/search?name=1%E7%8E%AF%E8%B7%AF'
req2 = urllib.request.Request(url2, headers={
    'User-Agent': 'Mozilla/5.0',
    'Accept': 'application/json',
    'Origin': 'https://buspedia.top',
    'Referer': 'https://buspedia.top/'
})
raw2 = urllib.request.urlopen(req2).read()
data2 = zlib.decompress(raw2[2:], -15) if raw2[0] == 0x78 else raw2
j2 = json.loads(data2)
lines2 = j2.get('l', [])
print(f'\nWithout region: {len(lines2)} lines')
for i, l in enumerate(lines2):
    print(f'{i}: {json.dumps(l, ensure_ascii=False)}')
    if i >= 3: break
