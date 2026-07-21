import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Remove old files from container first, then copy fresh
ssh.exec_command('docker exec bus-gallery-frontend sh -c "rm -rf /usr/share/nginx/html/assets/*"')
print('Cleared old assets')

# Re-copy from local dist
import os
sftp = ssh.open_sftp()
local_dist = r'D:\code\bus-gallery\frontend\dist'

def mkdir_p(sftp, remote_path):
    parts = remote_path.strip('/').split('/')
    current = ''
    for part in parts:
        current += '/' + part
        try:
            sftp.stat(current)
        except:
            sftp.mkdir(current)

# First upload index.html directly
remote_tmp = '/tmp/dist-new'
ssh.exec_command(f'rm -rf {remote_tmp}')
mkdir_p(sftp, remote_tmp)

for root, dirs, files in os.walk(local_dist):
    for name in files:
        local_path = os.path.join(root, name)
        relative = os.path.relpath(local_path, local_dist).replace('\\', '/')
        remote_path = remote_tmp + '/' + relative
        mkdir_p(sftp, os.path.dirname(remote_path))
        sftp.put(local_path, remote_path)

sftp.close()
print('Uploaded fresh dist')

# Copy into container
ssh.exec_command(f'docker cp {remote_tmp}/. bus-gallery-frontend:/usr/share/nginx/html/')
ssh.exec_command(f'rm -rf {remote_tmp}')
print('Copied to container')

# Verify
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend cat /usr/share/nginx/html/index.html')
idx = stdout.read().decode()
for line in idx.split('\n'):
    if 'script' in line or 'link' in line:
        print('Verified:', line.strip())

ssh.close()
