import paramiko, os

host = '192.144.227.251'
user = 'root'
pwd = 'whc@njupt020704'
local_dir = r'D:\code\bus-gallery\frontend\dist'
remote_tmp = '/tmp/dist-frontend'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(host, username=user, password=pwd, timeout=10)

sftp = ssh.open_sftp()
ssh.exec_command(f'rm -rf {remote_tmp}')

def mkdir_p(sftp, remote_path):
    parts = remote_path.strip('/').split('/')
    current = ''
    for part in parts:
        current += '/' + part
        try:
            sftp.stat(current)
        except:
            sftp.mkdir(current)

for root, dirs, files in os.walk(local_dir):
    for name in files:
        local_path = os.path.join(root, name)
        relative = os.path.relpath(local_path, local_dir).replace('\\', '/')
        remote_path = remote_tmp + '/' + relative
        mkdir_p(sftp, os.path.dirname(remote_path))
        sftp.put(local_path, remote_path)

sftp.close()
ssh.exec_command(f'docker cp {remote_tmp}/. bus-gallery-frontend:/usr/share/nginx/html/')
ssh.exec_command(f'rm -rf {remote_tmp}')
ssh.close()
print('Deploy done')
