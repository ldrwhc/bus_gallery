import requests
requests.packages.urllib3.disable_warnings()
r = requests.get('https://192.144.227.251/api/stats', verify=False, timeout=15)
d = r.json()
imgs = d.get('latestImages', [])
print(f'Latest images: {len(imgs)}')
for i in imgs[:3]:
    print(f'  plate={i.get("plateNumber")} model={i.get("modelName","")[:20]} by={i.get("uploaderName")} time={i.get("createdAt","")[:16]}')
