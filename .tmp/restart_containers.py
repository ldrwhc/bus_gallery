import paramiko, time

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

def run(cmd, timeout=120):
    print(f'>>> {cmd}')
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode(errors='replace')
    err = stderr.read().decode(errors='replace')
    if out: print(out.strip()[:4000])
    if err: print('ERR:', err.strip()[:1000])
    return out

# Check if frontend build container still running
run('docker ps -a --format "{{.Names}} {{.Status}}" | head -20')

# Try restarting backend container (it's already built)
print('\n=== Restart backend ===')
run('docker stop bus-gallery-backend && docker rm bus-gallery-backend 2>/dev/null; cd /root/code/bus_gallery/docker && docker compose up -d backend 2>&1')

time.sleep(3)

# Check status
run('docker ps --format "table {{.Names}}\t{{.Status}}"')
run('docker logs --tail=20 bus-gallery-backend 2>&1')

client.close()
