import urllib.request, ssl, zlib, json
ctx = ssl.create_default_context(); ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE
Z = b"x\x9c"

for q in ["%E4%BA%ACA+R1809", "%E4%BA%ACAR1809"]:
    for p in ["name", "q"]:
        url = "https://api.buspedia.top/search?" + p + "=" + q
        req = urllib.request.Request(url)
        req.add_header("User-Agent", "Mozilla/5.0")
        req.add_header("Accept", "application/json")
        req.add_header("Origin", "https://buspedia.top")
        req.add_header("Referer", "https://buspedia.top/")
        try:
            resp = urllib.request.urlopen(req, timeout=10)
            raw = resp.read()
            if raw[:2] == Z: raw = zlib.decompress(raw)
            data = json.loads(raw.decode("utf-8", errors="replace"))
            v = data.get("v", [])
            print("%s=%-30s v=%d" % (p, q, len(v)), end="")
            if v: print(" id=%s" % v[0].get("id",""))
            else: print()
        except Exception as e:
            print("%s=%-30s ERROR: %s" % (p, q, e))
