import paramiko

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

# Check Docker networks
run('docker network ls')
run('docker inspect bus-gallery-backend --format "{{json .NetworkSettings.Networks}}" 2>&1')
run('docker inspect bus-gallery-gateway --format "{{json .NetworkSettings.Networks}}" 2>&1')

# Test DNS from gateway
run('docker exec bus-gallery-gateway nslookup backend 2>&1 || docker exec bus-gallery-gateway getent hosts backend 2>&1')
run('docker exec bus-gallery-gateway ping -c2 -W2 backend 2>&1')

# Check if gateway can reach anything
run('docker exec bus-gallery-gateway wget -qO- --timeout=3 http://bus-gallery-backend:8080/api/regions 2>&1 | head -c 500')

# Check actual container names vs service names
run('docker inspect bus-gallery-backend --format "{{.Name}} {{.Config.Hostname}}"')
run('docker inspect bus-gallery-gateway --format "{{.Name}} {{.Config.Hostname}}"')

client.close()
