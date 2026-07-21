import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"')
print('Containers:')
print(stdout.read().decode())

stdin2, stdout2, stderr2 = ssh.exec_command('docker logs bus-gallery-backend --tail 30 2>&1')
print('Backend logs:')
print(stdout2.read().decode())

ssh.close()
