import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

# Read docker-compose, add GATEWAY_BRIDGE_URI
sftp = client.open_sftp()
with sftp.open('/root/code/bus_gallery/docker/docker-compose.yml', 'r') as f:
    content = f.read().decode('utf-8')

# Add GATEWAY_BRIDGE_URI next to GATEWAY_CONTENT_URI in gateway section
content = content.replace(
    'GATEWAY_CONTENT_URI: "http://backend:8080"',
    'GATEWAY_CONTENT_URI: "http://backend:8080"\n      GATEWAY_BRIDGE_URI: "http://bridge:8093"'
)

with sftp.open('/root/code/bus_gallery/docker/docker-compose.yml', 'w') as f:
    f.write(content.encode('utf-8'))
sftp.close()

# Verify
stdin, stdout, stderr = client.exec_command('grep -A1 "GATEWAY.*URI" /root/code/bus_gallery/docker/docker-compose.yml')
print(stdout.read().decode())

# Restart gateway
stdin, stdout, stderr = client.exec_command('cd /root/code/bus_gallery/docker && docker compose up -d gateway 2>&1')
print(stdout.read().decode())

import time
time.sleep(10)

# Test bridge
stdin, stdout, stderr = client.exec_command('curl -sk -o /dev/null -w "%{http_code}" https://localhost/api/bridge/portal/messages?limit=5 2>&1')
print(f'Bridge API status: {stdout.read().decode()}')

stdin, stdout, stderr = client.exec_command('curl -sk https://localhost/api/bridge/portal/messages?limit=5 2>&1 | head -c 300')
print(stdout.read().decode())

# Also test bridge internally
stdin, stdout, stderr = client.exec_command('docker exec bus-gallery-gateway wget -qO- --timeout=3 http://bridge:8093/api/bridge/portal/messages?limit=3 2>&1 | head -c 300')
print(f'Internal bridge: {stdout.read().decode()}')

client.close()
