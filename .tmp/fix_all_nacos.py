import paramiko, time

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

def run(cmd, timeout=30):
    print(f'>>> {cmd}')
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode(errors='replace')
    err = stderr.read().decode(errors='replace')
    if out: print(out.strip()[:2000])
    if err: print('ERR:', err.strip()[:1000])
    return out

# Check current nacos settings across all services
print('=== Check current NACOS in docker-compose ===')
run("grep -n 'NACOS' /root/code/bus_gallery/docker/docker-compose.yml")
run("grep -n 'GATEWAY_CONTENT_URI\|GATEWAY_BRIDGE_URI' /root/code/bus_gallery/docker/docker-compose.yml")

# Add NACOS_DISCOVERY_ENABLED=false to group service (after TZ line)
print('\n=== Adding NACOS_DISCOVERY_ENABLED=false to group ===')
run("grep -n 'TZ: Asia/Shanghai' /root/code/bus_gallery/docker/docker-compose.yml | head -5")
# Group section TZ is around line 188
run("sed -i '/^      TZ: Asia.Shanghai$/!b;n;/^      TRADE_DB_URL:/i\\      NACOS_DISCOVERY_ENABLED: \"false\"' /root/code/bus_gallery/docker/docker-compose.yml")
# Hmm sed approach is fragile. Let me just check and adjust manually
# Actually, group already has TZ, and TRADE_DB_URL is the next env var

# Simpler approach: add to the file using known line patterns
# Check if we can just add a global override via .env
print('\n=== Current .env.server ===')
run('cat /root/code/bus_gallery/docker/.env.server')

# Add NACOS_DISCOVERY_ENABLED to all services via .env
run('echo "" >> /root/code/bus_gallery/docker/.env.server && echo "NACOS_DISCOVERY_ENABLED=false" >> /root/code/bus_gallery/docker/.env.server')
run('echo "GATEWAY_CONTENT_URI=http://backend:8080" >> /root/code/bus_gallery/docker/.env.server')

print('\n=== Updated .env.server ===')
run('cat /root/code/bus_gallery/docker/.env.server')

# Now restart all containers
print('\n=== Restart all containers ===')
run('cd /root/code/bus_gallery/docker && docker compose up -d 2>&1')

time.sleep(10)
run('docker ps --format "table {{.Names}}\t{{.Status}}"')

# Quick test
print('\n=== Test API ===')
run('curl -s -o /dev/null -w "%{http_code}" http://localhost:8094/api/regions 2>&1')
run('curl -s http://localhost:8094/api/regions 2>&1 | head -c 500')

client.close()
