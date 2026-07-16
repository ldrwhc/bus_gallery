"""Test all buspedia search API variants."""
import urllib.request, urllib.parse, ssl, zlib, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

Z = b"x\x9c"

def search(param, query):
    url = "https://api.buspedia.top/search?" + param + "=" + urllib.parse.quote(query)
    req = urllib.request.Request(url)
    req.add_header("User-Agent", "Mozilla/5.0")
    req.add_header("Accept", "application/json")
    req.add_header("Origin", "https://buspedia.top")
    req.add_header("Referer", "https://buspedia.top/")
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        raw = resp.read()
        if raw[:2] == Z:
            raw = zlib.decompress(raw)
        data = json.loads(raw.decode("utf-8", errors="replace"))
        v = data.get("v", []) if isinstance(data, dict) else (data if isinstance(data, list) else [])
        return v
    except Exception as e:
        return None

queries = ["福田", "BJ6160", "m19pwe", "京A R1809", "京AR1809"]
params = ["q", "name", "keyword", "search"]

for query in queries:
    found = False
    for param in params:
        v = search(param, query)
        if v and len(v) > 0:
            first = v[0]
            print("FOUND: %s='%s' => v_count=%d, id=%s, regist=%s" % (
                param, query, len(v), first.get("id", ""), first.get("regist", "")))
            found = True
            break
    if not found:
        print("NOT FOUND: '%s' with any param" % query)
