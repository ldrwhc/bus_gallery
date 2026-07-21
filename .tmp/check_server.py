import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend cat /usr/share/nginx/html/index.html')
index = stdout.read().decode()
print('=== index.html ===')
print(index)

stdin2, stdout2, stderr2 = ssh.exec_command('docker exec bus-gallery-frontend ls /usr/share/nginx/html/assets/')
assets = stdout2.read().decode()
print('=== assets (all) ===')
for f in sorted(assets.strip().split('\n')):
    print(f)

ssh.close()
