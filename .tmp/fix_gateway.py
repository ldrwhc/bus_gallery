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

# Check current docker-compose for gateway env
print('=== Current gateway config in docker-compose.yml ===')
run("grep -A 30 'gateway:' /root/code/bus_gallery/docker/docker-compose.yml | head -40")

# Add NACOS_DISCOVERY_ENABLED=false and GATEWAY_CONTENT_URI to gateway env
print('\n=== Fixing gateway config ===')
# Check if NACOS_DISCOVERY_ENABLED already exists
run("grep -n 'NACOS_DISCOVERY_ENABLED' /root/code/bus_gallery/docker/docker-compose.yml")
run("grep -n 'GATEWAY_CONTENT_URI' /root/code/bus_gallery/docker/docker-compose.yml")

# Add the env vars using sed
# Find the line with "SERVER_PORT: 8094" in gateway section and add after it
run("sed -i '/SERVER_PORT: 8094/a\\      NACOS_DISCOVERY_ENABLED: \"false\"\\n      GATEWAY_CONTENT_URI: \"http://backend:8080\"' /root/code/bus_gallery/docker/docker-compose.yml")

print('\n=== Verify changes ===')
run("grep -A 5 'SERVER_PORT: 8094' /root/code/bus_gallery/docker/docker-compose.yml | head -10")

print('\n=== Restart gateway ===')
run('cd /root/code/bus_gallery/docker && docker compose up -d gateway 2>&1')

time.sleep(5)
run('docker ps --format "table {{.Names}}\t{{.Status}}"')
print('\n=== Gateway logs ===')
run('docker logs --tail=20 bus-gallery-gateway 2>&1')

client.close()
