import paramiko, time, sys

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

# Upload only CompanyCatalog.vue
sftp = client.open_sftp()
sftp.put(r'D:\code\bus-gallery\frontend\src\views\CompanyCatalog.vue', '/root/code/bus_gallery/frontend/src/views/CompanyCatalog.vue')
sftp.close()
print('CompanyCatalog.vue uploaded')

# Rebuild frontend only
print('Building frontend...')
channel = client.get_transport().open_session()
channel.exec_command('cd /root/code/bus_gallery/docker && docker compose build --no-cache frontend 2>&1')
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
while channel.recv_ready():
    try: sys.stdout.buffer.write(channel.recv(4096))
    except: pass
rc = channel.recv_exit_status()
print(f'\nBuild exit: {rc}')

if rc == 0:
    # Restart frontend
    stdin, stdout, stderr = client.exec_command('cd /root/code/bus_gallery/docker && docker compose up -d frontend 2>&1', timeout=60)
    print(stdout.read().decode())
    time.sleep(8)

    # Test
    stdin, stdout, stderr = client.exec_command('curl -sk -o /dev/null -w "%{http_code}" https://localhost/')
    print(f'Frontend: {stdout.read().decode().strip()}')
    stdin, stdout, stderr = client.exec_command('docker ps --format "table {{.Names}}\t{{.Status}}"')
    print(stdout.read().decode())

client.close()
