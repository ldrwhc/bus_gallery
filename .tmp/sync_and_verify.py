import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

# Upload local docker-compose.yml
sftp = client.open_sftp()
sftp.put(r'D:\code\bus-gallery\docker\docker-compose.yml', '/root/code/bus_gallery/docker/docker-compose.yml')
sftp.close()
print('docker-compose.yml uploaded')

# Full restart
stdin, stdout, stderr = client.exec_command('cd /root/code/bus_gallery/docker && docker compose up -d 2>&1')
print(stdout.read().decode())

import time
time.sleep(10)

# Quick tests
def test(url, desc):
    stdin, stdout, stderr = client.exec_command(f'curl -sk -o /dev/null -w "%{{http_code}}" {url} 2>&1')
    code = stdout.read().decode().strip()
    print(f'{desc}: {code} ({"OK" if code in ("200","401","404") else "FAIL"})')

test('https://localhost/api/regions', 'Regions API')
test('https://localhost/api/vehicles?size=1', 'Vehicles API')
test('https://localhost/api/bridge/portal/messages?limit=3', 'Bridge API')
test('https://localhost/', 'Frontend')

stdin, stdout, stderr = client.exec_command('docker ps --format "table {{.Names}}\t{{.Status}}"')
print(f'\n{stdout.read().decode()}')

client.close()
