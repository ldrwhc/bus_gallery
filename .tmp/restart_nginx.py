import paramiko, time
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=15)

# Full restart
stdin, stdout, stderr = ssh.exec_command('docker restart bus-gallery-frontend 2>&1', timeout=20)
print('restart:', stdout.read().decode(), stderr.read().decode())
time.sleep(5)

# Quick test from localhost
stdin, stdout, stderr = ssh.exec_command('curl -sk -o /dev/null -w "%{http_code}" --connect-timeout 5 -H "Host: bgpic.site" https://127.0.0.1/', timeout=10)
print('self test:', stdout.read().decode())

ssh.close()
