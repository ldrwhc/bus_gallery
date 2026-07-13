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

# Test backend:8080 from gateway
print('=== Test backend:8080 from gateway ===')
run('docker exec bus-gallery-gateway wget -qO- --timeout=5 http://backend:8080/api/regions 2>&1 | head -c 300')

# Test from external
print('\n=== Test from external ===')
run('curl -sk https://localhost/api/regions 2>&1 | head -c 500')

# Check gateway route config
print('\n=== Gateway routes ===')
run('docker exec bus-gallery-gateway wget -qO- --timeout=3 http://localhost:8094/actuator/gateway/routes 2>&1 | head -c 500')

# Check gateway logs for recent errors
print('\n=== Gateway recent logs ===')
run('docker logs --tail=10 bus-gallery-gateway 2>&1')

client.close()
