
import paramiko, os, stat

host = '192.144.227.251'
user = 'root'
pwd = 'whc@njupt020704'
local_dir = r'D:\code\bus-gallery\frontend\dist'
remote_tmp = '/tmp/dist-frontend'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(host, username=user, password=pwd, timeout=10)

sftp = ssh.open_sftp()
ssh.exec_command('rm -rf ' + remote_tmp)

def mkdir_p(sftp, remote_path):
    if not remote_path or remote_path == '/':
        return
    parts = remote_path.strip('/').split('/')
    current = ''
    for part in parts:
        if not part:
            continue
        current += '/' + part
        try:
            sftp.stat(current)
        except:
            sftp.mkdir(current)

count = 0
for root, dirs, files in os.walk(local_dir):
    for name in files:
        local_path = os.path.join(root, name)
        relative = os.path.relpath(local_path, local_dir)
        relative = relative.replace('\\', '/')
        remote_path = remote_tmp + '/' + relative
        remote_dir = os.path.dirname(remote_path)
        mkdir_p(sftp, remote_dir)
        sftp.put(local_path, remote_path)
        count += 1
        if count % 20 == 0:
            print(f'Uploaded {count} files...')

sftp.close()
print(f'Uploaded {count} files total.')

stdin, stdout, stderr = ssh.exec_command('docker cp /tmp/dist-frontend/. bus-gallery-frontend:/usr/share/nginx/html/')
print('docker cp:', stdout.read().decode(), stderr.read().decode())

ssh.exec_command('rm -rf /tmp/dist-frontend')

stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend ls /usr/share/nginx/html/index.html')
print('Verify:', stdout.read().decode().strip())

ssh.close()
print('Deploy complete!')
