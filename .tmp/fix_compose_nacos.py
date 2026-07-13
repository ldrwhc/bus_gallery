import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

sftp = client.open_sftp()
# Read current file
with sftp.open('/root/code/bus_gallery/docker/docker-compose.yml', 'r') as f:
    content = f.read().decode('utf-8')

# Add NACOS_DISCOVERY_ENABLED to group service
# Group has TZ followed by TRADE_DB_URL
content = content.replace(
    "      TRADE_DB_URL: ${TRADE_DB_URL:-jdbc:mysql://mysql:3306/trade_center",
    "      NACOS_DISCOVERY_ENABLED: \"false\"\n      TRADE_DB_URL: ${TRADE_DB_URL:-jdbc:mysql://mysql:3306/trade_center"
)

# Add NACOS_DISCOVERY_ENABLED to bridge service
# Bridge has SERVER_PORT: 8093
content = content.replace(
    "      SERVER_PORT: 8093\n      BRIDGE_CONTENT_SERVICE_NAME",
    "      SERVER_PORT: 8093\n      NACOS_DISCOVERY_ENABLED: \"false\"\n      BRIDGE_CONTENT_SERVICE_NAME"
)

# Add GATEWAY_CONTENT_URI override for gateway (already added, but ensure it's there)
# Remove duplicate NACOS_DISCOVERY_ENABLED from gateway if sed added extra
# First check if we already have duplicates
import re
# Count occurrences after gateway's SERVER_PORT: 8094
gateway_section = content.split('gateway:')[1].split('frontend:')[0]
nacos_count = gateway_section.count('NACOS_DISCOVERY_ENABLED')
print(f"NACOS_DISCOVERY_ENABLED count in gateway section: {nacos_count}")

if nacos_count > 1:
    # Remove duplicates - keep only first occurrence after SERVER_PORT
    lines = content.split('\n')
    new_lines = []
    found_gateway = False
    found_port = False
    added_nacos = False
    for line in lines:
        if '  gateway:' in line:
            found_gateway = True
        if found_gateway and '  frontend:' in line:
            found_gateway = False
        if found_gateway and 'SERVER_PORT: 8094' in line:
            found_port = True
            new_lines.append(line)
            continue
        if found_gateway and found_port and 'NACOS_DISCOVERY_ENABLED' in line and added_nacos:
            continue  # Skip duplicate
        if found_gateway and found_port and 'NACOS_DISCOVERY_ENABLED' in line:
            added_nacos = True
        if found_gateway and found_port and 'GATEWAY_CONTENT_URI' in line and added_nacos:
            # Keep GATEWAY_CONTENT_URI too
            pass
        new_lines.append(line)
    content = '\n'.join(new_lines)

# Write back
with sftp.open('/root/code/bus_gallery/docker/docker-compose.yml', 'w') as f:
    f.write(content.encode('utf-8'))

sftp.close()

# Verify
stdin, stdout, stderr = client.exec_command('grep -A3 "NACOS_DISCOVERY_ENABLED" /root/code/bus_gallery/docker/docker-compose.yml')
print(stdout.read().decode())

# Restart
stdin, stdout, stderr = client.exec_command('cd /root/code/bus_gallery/docker && docker compose up -d 2>&1')
print(stdout.read().decode())

import time
time.sleep(5)

stdin, stdout, stderr = client.exec_command('docker ps --format "table {{.Names}}\t{{.Status}}"')
print(stdout.read().decode())

# Quick API test
stdin, stdout, stderr = client.exec_command('curl -s -o /dev/null -w "%{http_code}" http://localhost:8094/api/regions 2>&1')
print(f'API status: {stdout.read().decode()}')

client.close()
