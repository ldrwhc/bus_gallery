import urllib.request, ssl, zlib, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Test search
for query in ["%E4%BA%ACAR1809", "%E4%BA%ACA+R1809", "24441"]:
    url = f"https://api.buspedia.top/search?q={query}"
    req = urllib.request.Request(url)
    req.add_header("User-Agent", "Mozilla/5.0")
    req.add_header("Accept", "application/json")
    req.add_header("Origin", "https://buspedia.top")
    req.add_header("Referer", "https://buspedia.top/")
    try:
        resp = urllib.request.urlopen(req, timeout=15)
        raw = resp.read()
        if raw[:2] == b"x\x9c":
            raw = zlib.decompress(raw)
        data = json.loads(raw.decode("utf-8", errors="replace"))
        v = data.get("v", [])
        print(f"Search q='{query}': v_count={len(v)}")
        if v:
            first = v[0]
            print(f"  first: id={first.get('id')}, regist={first.get('regist')}, no={first.get('no')}")
    except Exception as e:
        print(f"Search q='{query}': ERROR {e}")
