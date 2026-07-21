import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend cat /usr/share/nginx/html/index.html')
idx = stdout.read().decode()
server_chunk = ''
for line in idx.split('\n'):
    if 'script' in line and 'src' in line:
        server_chunk = line.strip()
        break

import os
local_dist = r'D:\code\bus-gallery\frontend\dist'
local_idx = open(os.path.join(local_dist, 'index.html')).read()
local_chunk = ''
for line in local_idx.split('\n'):
    if 'script' in line and 'src' in line:
        local_chunk = line.strip()
        break

print('Local: ', local_chunk)
print('Server:', server_chunk)
print('Match:', local_chunk == server_chunk)
ssh.close()
