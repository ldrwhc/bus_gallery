import paramiko, time

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

def run(cmd, timeout=30):
    print(f'>>> {cmd}')
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode(errors='replace')
    err = stderr.read().decode(errors='replace')
    if out: print(out.strip()[:3000])
    if err: print('ERR:', err.strip()[:1000])
    return out

# 1. Check what's wrong - gateway env vars
print('=== Gateway env vars currently ===')
run("docker inspect bus-gallery-gateway --format '{{range .Config.Env}}{{println .}}{{end}}' | grep -E 'GATEWAY.*URI|NACOS'")

print('\n=== Bridge env vars currently ===')
run("docker inspect bus-gallery-bridge --format '{{range .Config.Env}}{{println .}}{{end}}' | grep -E 'BRIDGE.*URL|NACOS'")

# 2. Check if docker-compose on server has the right config
print('\n=== Server docker-compose gateway section ===')
run("sed -n '/^  gateway:/,/^  frontend:/p' /root/code/bus_gallery/docker/docker-compose.yml | grep -E 'GATEWAY.*URI|NACOS|SERVER_PORT'")

print('\n=== Server docker-compose bridge section ===')
run("sed -n '/^  bridge:/,/^  gateway:/p' /root/code/bus_gallery/docker/docker-compose.yml | grep -E 'BRIDGE.*URL|NACOS|SERVER_PORT'")

# 3. Fix directly on server by editing docker-compose.yml
print('\n=== Reading full compose for editing ===')
sftp = client.open_sftp()
with sftp.open('/root/code/bus_gallery/docker/docker-compose.yml', 'r') as f:
    compose = f.read().decode('utf-8')
sftp.close()

# Check what's missing from gateway section
if 'GATEWAY_CONTENT_URI' not in compose or 'GATEWAY_BRIDGE_URI' not in compose:
    print('Gateway URIs MISSING - fixing...')
    # Add after SERVER_PORT: 8094 in gateway section
    old = '      SERVER_PORT: 8094\n      REDIS_HOST: redis\n      REDIS_PORT: 6379\n      REDIS_PASSWORD: ${REDIS_PASSWORD:-12345678}\n      REDIS_DATABASE: ${REDIS_DATABASE:-0}\n      GATEWAY_AUTH_ENABLED'
    new = '      SERVER_PORT: 8094\n      NACOS_DISCOVERY_ENABLED: "false"\n      GATEWAY_CONTENT_URI: "http://backend:8080"\n      GATEWAY_BRIDGE_URI: "http://bridge:8093"\n      REDIS_HOST: redis\n      REDIS_PORT: 6379\n      REDIS_PASSWORD: ${REDIS_PASSWORD:-12345678}\n      REDIS_DATABASE: ${REDIS_DATABASE:-0}\n      GATEWAY_AUTH_ENABLED'
    if old in compose:
        compose = compose.replace(old, new)
        print('Gateway config fixed')
    else:
        print('Pattern not found, checking...')
        # Find gateway SERVER_PORT line
        idx = compose.find('SERVER_PORT: 8094')
        print(f'SERVER_PORT: 8094 found at index {idx}')

# Check bridge section for BRIDGE_CONTENT_SERVICE_URL
if 'BRIDGE_CONTENT_SERVICE_URL:' not in compose:
    print('Bridge service URLs MISSING - fixing...')
    old_bridge = '      BRIDGE_CONTENT_SERVICE_NAME: ${BRIDGE_CONTENT_SERVICE_NAME:-bus-gallery}\n      BRIDGE_TRADE_SERVICE_NAME: ${BRIDGE_TRADE_SERVICE_NAME:-bus-gallery-group-buy}\n      BRIDGE_CONTENT_SERVICE_BASE_URL'
    new_bridge = '      BRIDGE_CONTENT_SERVICE_NAME: ${BRIDGE_CONTENT_SERVICE_NAME:-bus-gallery}\n      BRIDGE_TRADE_SERVICE_NAME: ${BRIDGE_TRADE_SERVICE_NAME:-bus-gallery-group-buy}\n      BRIDGE_CONTENT_SERVICE_URL: "http://backend:8080"\n      BRIDGE_TRADE_SERVICE_URL: "http://group:8092"\n      BRIDGE_CONTENT_SERVICE_BASE_URL'
    if old_bridge in compose:
        compose = compose.replace(old_bridge, new_bridge)
        print('Bridge config fixed')
    else:
        print('Bridge pattern not found')

# Write back
sftp = client.open_sftp()
with sftp.open('/root/code/bus_gallery/docker/docker-compose.yml', 'w') as f:
    f.write(compose.encode('utf-8'))
sftp.close()
print('Compose file updated on server')

# 4. ONE restart
print('\n=== Final restart ===')
run('cd /root/code/bus_gallery/docker && docker compose up -d 2>&1')

time.sleep(15)

# 5. Verify
print('\n=== Container Status ===')
run('docker ps --format "table {{.Names}}\t{{.Status}}"')

print('\n=== API Tests ===')
for url, desc in [
    ('https://localhost/api/regions', 'Regions'),
    ('https://localhost/api/vehicles?size=1', 'Vehicles'),
    ('https://localhost/api/bridge/portal/messages?limit=3', 'Bridge'),
]:
    stdin, stdout, stderr = client.exec_command(f'curl -sk -o /dev/null -w "%{{http_code}}" {url}', timeout=15)
    code = stdout.read().decode().strip()
    ok = 'OK' if code in ('200','401','404') else 'FAIL'
    print(f'{desc}: {code} ({ok})')

print('\n=== Bridge recent logs (check for loadbalancer errors) ===')
run('docker logs --tail=10 bus-gallery-bridge 2>&1 | grep -E "WARN|ERROR|LoadBalancer|No servers" || echo "No loadbalancer errors"')

client.close()
