import paramiko, time, sys

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

# Upload fixed docker-compose.yml
sftp = client.open_sftp()
sftp.put(r'D:\code\bus-gallery\docker\docker-compose.yml', '/root/code/bus_gallery/docker/docker-compose.yml')
sftp.close()
print('docker-compose.yml uploaded')

# Verify no duplicate
stdin, stdout, stderr = client.exec_command("grep -n 'NACOS_DISCOVERY_ENABLED' /root/code/bus_gallery/docker/docker-compose.yml")
print(stdout.read().decode())

# Rebuild bridge
print('Building bridge...')
channel = client.get_transport().open_session()
channel.exec_command('cd /root/code/bus_gallery/docker && docker compose build --no-cache bridge 2>&1')
end = time.time() + 600
while time.time() < end:
    if channel.recv_ready():
        data = channel.recv(4096)
        try: sys.stdout.buffer.write(data)
        except: pass
    if channel.recv_stderr_ready():
        data = channel.recv_stderr(4096)
        try: sys.stdout.buffer.write(data)
        except: pass
    if channel.exit_status_ready():
        break
    time.sleep(0.3)
# Drain
while channel.recv_ready():
    try: sys.stdout.buffer.write(channel.recv(4096))
    except: pass

rc = channel.recv_exit_status()
print(f'\nBuild exit: {rc}')

if rc == 0:
    print('\n=== Restart bridge + gateway ===')
    stdin, stdout, stderr = client.exec_command('cd /root/code/bus_gallery/docker && docker compose up -d 2>&1', timeout=60)
    print(stdout.read().decode())

    time.sleep(15)

    # Test
    print('\n=== Test APIs ===')
    for url, desc in [
        ('https://localhost/api/regions', 'Regions'),
        ('https://localhost/api/vehicles?size=1', 'Vehicles'),
        ('https://localhost/api/bridge/portal/messages?limit=3', 'Bridge Messages'),
    ]:
        stdin, stdout, stderr = client.exec_command(f'curl -sk -o /dev/null -w "%{{http_code}}" {url}')
        code = stdout.read().decode().strip()
        print(f'{desc}: {code}')

    print('\n=== Bridge logs ===')
    stdin, stdout, stderr = client.exec_command('docker logs --tail=15 bus-gallery-bridge 2>&1', timeout=10)
    print(stdout.read().decode())

client.close()
