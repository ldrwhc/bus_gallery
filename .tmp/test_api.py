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

# Test via the actual external URL (goes through nginx -> gateway -> backend)
print('=== Test API via nginx ===')
run('curl -sk -o /dev/null -w "%{http_code}" https://localhost/api/regions')
print()
run('curl -sk https://localhost/api/regions 2>&1 | head -c 1000')

# Check nginx config
print('\n=== Nginx config ===')
run('docker exec bus-gallery-frontend cat /etc/nginx/conf.d/default.conf 2>&1 | head -40')

# Check gateway and backend connectivity
print('\n=== Gateway to backend test ===')
run('docker exec bus-gallery-gateway wget -qO- http://backend:8080/api/regions --timeout=5 2>&1 | head -c 500')

# Check all container health
print('\n=== Container health ===')
run('docker ps --format "table {{.Names}}\t{{.Status}}"')

client.close()
