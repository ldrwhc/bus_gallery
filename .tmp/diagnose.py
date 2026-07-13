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

# 1. Check container status
print('=== Container Status ===')
run('docker ps -a --format "table {{.Names}}\t{{.Status}}"')

# 2. Check gateway env vars
print('\n=== Gateway ENV (NACOS + GATEWAY vars) ===')
run("docker inspect bus-gallery-gateway --format '{{range .Config.Env}}{{println .}}{{end}}' | grep -E 'NACOS|GATEWAY.*URI|SERVER_PORT'")

# 3. Check bridge env vars
print('\n=== Bridge ENV (NACOS) ===')
run("docker inspect bus-gallery-bridge --format '{{range .Config.Env}}{{println .}}{{end}}' | grep -E 'NACOS|SERVER_PORT'")

# 4. Check gateway logs for bridge-related errors
print('\n=== Gateway recent logs ===')
run('docker logs --tail=30 bus-gallery-gateway 2>&1 | grep -E "bridge|error|ERROR|503|Downstream" || docker logs --tail=30 bus-gallery-gateway 2>&1 | tail -15')

# 5. Check bridge logs
print('\n=== Bridge recent logs ===')
run('docker logs --tail=20 bus-gallery-bridge 2>&1')

# 6. Test bridge from gateway
print('\n=== Gateway -> Bridge connectivity ===')
run('docker exec bus-gallery-gateway wget -qO- --timeout=5 http://bridge:8093/actuator/health 2>&1 || docker exec bus-gallery-gateway wget -qO- --timeout=5 http://bus-gallery-bridge:8093/actuator/health 2>&1')

# 7. Test bridge directly
print('\n=== Direct bridge test ===')
run('docker exec bus-gallery-gateway ping -c2 -W2 bridge 2>&1')
run('docker exec bus-gallery-gateway ping -c2 -W2 bus-gallery-bridge 2>&1')

# 8. Check if bridge port is open
print('\n=== Bridge port check ===')
run('docker exec bus-gallery-bridge netstat -tlnp 2>&1 || docker exec bus-gallery-bridge ss -tlnp 2>&1')

# 9. Test the actual bridge API
print('\n=== Bridge API direct ===')
run('curl -sk https://localhost/api/bridge/portal/messages?limit=3 2>&1 | head -c 500')

client.close()
