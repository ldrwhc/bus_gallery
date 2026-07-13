import paramiko, time

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

def run(cmd, timeout=30):
    print(f'>>> {cmd}')
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode(errors='replace')
    err = stderr.read().decode(errors='replace')
    if out: print(out.strip()[:4000])
    if err: print('ERR:', err.strip()[:1000])
    return out

run('docker ps --format "table {{.Names}}\t{{.Status}}"')
print('\n=== Gateway logs ===')
run('docker logs --tail=30 bus-gallery-gateway 2>&1')
print('\n=== Backend logs ===')
run('docker logs --tail=30 bus-gallery-backend 2>&1')
print('\n=== Frontend nginx error ===')
run('docker exec bus-gallery-frontend cat /var/log/nginx/error.log 2>&1 | tail -20')
client.close()
