import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker logs bus-gallery-backend --tail 60 2>&1')
print(stdout.read().decode())

ssh.close()
