import urllib.request, zlib, json

# First, find a vehicle in Anshan to see region IDs
url = 'https://api.buspedia.top/search?name=%E8%BE%BDc%200001'
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0',
    'Accept': 'application/json',
    'Origin': 'https://buspedia.top',
    'Referer': 'https://buspedia.top/'
})
raw = urllib.request.urlopen(req).read()
data = zlib.decompress(raw[2:], -15) if raw[0] == 0x78 else raw
j = json.loads(data)

# Check region/city info in the response
if 'r' in j:
    print('Regions found:', json.dumps(j['r'], ensure_ascii=False)[:500])

# Try without "路" suffix — buspedia website shows "1环" not "1环路"
url3 = 'https://api.buspedia.top/search?name=1%E7%8E%AF'
print('Searching "1环" (no 路)...')
req3 = urllib.request.Request(url3, headers={'User-Agent': 'Mozilla/5.0', 'Accept': 'application/json', 'Origin': 'https://buspedia.top', 'Referer': 'https://buspedia.top/'})
raw3 = urllib.request.urlopen(req3).read()
data3 = zlib.decompress(raw3[2:], -15) if raw3[0] == 0x78 else raw3
j3 = json.loads(data3)
lines3 = j3.get('l', [])
print(f'Found {len(lines3)} lines:')
for l in lines3[:5]:
    print(f'  {json.dumps(l, ensure_ascii=False)[:150]}')

# Try with region=106 (Anshan?)
print()
url4 = 'https://api.buspedia.top/search?name=1%E7%8E%AF&region=106'
req4 = urllib.request.Request(url4, headers={'User-Agent': 'Mozilla/5.0', 'Accept': 'application/json', 'Origin': 'https://buspedia.top', 'Referer': 'https://buspedia.top/'})
raw4 = urllib.request.urlopen(req4).read()
data4 = zlib.decompress(raw4[2:], -15) if raw4[0] == 0x78 else raw4
j4 = json.loads(data4)
lines4 = j4.get('l', [])
print(f'With region=106: {len(lines4)} lines')
for l in lines4[:5]:
    print(f'  {json.dumps(l, ensure_ascii=False)[:150]}')
    req2 = urllib.request.Request(url2, headers={
        'User-Agent': 'Mozilla/5.0',
        'Accept': 'application/json',
        'Origin': 'https://buspedia.top',
        'Referer': 'https://buspedia.top/'
    })
    raw2 = urllib.request.urlopen(req2).read()
    data2 = zlib.decompress(raw2[2:], -15) if raw2[0] == 0x78 else raw2
    j2 = json.loads(data2)
    lines = j2.get('l', [])
    if lines:
        print(f'region={rid}: {len(lines)} lines, first={json.dumps(lines[0], ensure_ascii=False)[:100]}')
