import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Try from frontend container (has nginx which proxies /api to backend)
stdin, stdout, stderr = ssh.exec_command("docker exec bus-gallery-frontend wget -qO- http://bus-gallery-gateway:8094/api/users/2/footprint 2>&1 | head -c 2000")
out = stdout.read().decode()
err = stderr.read().decode()
print('Gateway:', out[:500] if out else 'EMPTY')
if err: print('Err:', err[:200])

# Check what hostnames resolve
stdin2, stdout2, stderr2 = ssh.exec_command("docker exec bus-gallery-frontend wget -qO- http://bus-gallery-backend:8080/api/users/2/footprint 2>&1 | head -c 2000")
out2 = stdout2.read().decode()
print('Backend direct:', out2[:500] if out2 else 'EMPTY')

ssh.close()
