import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker restart bus-gallery-gateway 2>&1')
print('Restart gateway:', stdout.read().decode())

# Wait a bit
import time
time.sleep(5)

stdin2, stdout2, stderr2 = ssh.exec_command('docker ps --format "table {{.Names}}\t{{.Status}}"')
print(stdout2.read().decode())

ssh.close()
