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

print('=== Group logs ===')
run('docker logs --tail=50 bus-gallery-group 2>&1')
print('\n=== Bridge logs ===')
run('docker logs --tail=20 bus-gallery-bridge 2>&1')
print('\n=== Container status ===')
run('docker ps -a --format "table {{.Names}}\t{{.Status}}"')

# Wait a bit and check if group recovers
print('\nWaiting 10s...')
time.sleep(10)
print('\n=== After wait ===')
run('docker ps --format "table {{.Names}}\t{{.Status}}"')

client.close()
