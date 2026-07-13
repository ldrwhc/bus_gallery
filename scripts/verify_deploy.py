"""Verify backend and frontend deployment."""
import paramiko, json, time

host = '192.144.227.251'
user = 'root'
pwd = 'whc@njupt020704'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(host, username=user, password=pwd, timeout=10)

# Wait for backend startup
time.sleep(15)

# Check backend
stdin, stdout, stderr = ssh.exec_command('docker logs bus-gallery-backend --tail 2')
print(stdout.read().decode())

# Test API
stdin, stdout, stderr = ssh.exec_command('curl -sk https://localhost/api/routes?size=200')
data = json.loads(stdout.read())
print(f'Total: {data["total"]}, Records: {len(data["records"])}')
found_3 = any(r['id'] == 3 for r in data['records'])
print(f'Route 3 in listing: {found_3}')

ssh.close()
