#!/bin/bash
# Kill docker-proxy for 443 and use socat instead

# 1. Kill docker-proxy for 443
fuser -k 443/tcp 2>/dev/null
sleep 1

# 2. Start socat forwarding
nohup socat TCP-LISTEN:443,fork,reuseaddr TCP:172.19.0.7:443 > /dev/null 2>&1 &
echo "socat started PID $!"

sleep 1

# 3. Verify
ss -tlnp | grep 443

# 4. Test from host
curl -sk --connect-timeout 3 -o /dev/null -w "%{http_code}\n" https://127.0.0.1/ -H "Host: bgpic.site"
