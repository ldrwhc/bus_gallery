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

# Upload updated docker-compose.yml
sftp = client.open_sftp()
sftp.put(r'D:\code\bus-gallery\docker\docker-compose.yml', '/root/code/bus_gallery/docker/docker-compose.yml')
sftp.close()
print('docker-compose.yml uploaded')

# Also upload updated bridge application.yml
sftp = client.open_sftp()
sftp.put(r'D:\code\bus-gallery\bridge\src\main\resources\application.yml', '/root/code/bus_gallery/bridge/src/main/resources/application.yml')
sftp.close()
print('bridge application.yml uploaded')

# Verify the fix
print('\n=== Bridge env check ===')
run("grep -A1 'BRIDGE.*URL' /root/code/bus_gallery/docker/docker-compose.yml")

# Rebuild bridge + restart
print('\n=== Rebuild bridge ===')
stdin, stdout, stderr = client.exec_command('cd /root/code/bus_gallery/docker && docker compose build --no-cache bridge 2>&1', timeout=600)
# Use channel for streaming
import sys
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
rc = channel.recv_exit_status()
print(f'\nBuild exit: {rc}')

if rc == 0:
    print('\n=== Restart bridge ===')
    run('cd /root/code/bus_gallery/docker && docker compose up -d bridge 2>&1')
    time.sleep(10)
    run('docker ps --format "table {{.Names}}\t{{.Status}}"')

    # Test
    print('\n=== Test Bridge API ===')
    run('curl -sk -o /dev/null -w "%{http_code}" https://localhost/api/bridge/portal/messages?limit=3')
    run('curl -sk https://localhost/api/bridge/portal/messages?limit=3 2>&1 | head -c 200')

client.close()
