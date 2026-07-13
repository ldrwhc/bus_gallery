#!/usr/bin/env python
"""Deploy frontend dist to production server."""
import paramiko, os, sys

HOST = '192.144.227.251'
USER = 'root'
PWD = 'whc@njupt020704'
LOCAL_DIR = r'D:\code\bus-gallery\frontend\dist'
REMOTE_TMP = '/tmp/dist-frontend'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PWD, timeout=10)

sftp = ssh.open_sftp()
ssh.exec_command('rm -rf ' + REMOTE_TMP)

def mkdir_p(sftp, remote_path):
    parts = remote_path.strip('/').split('/')
    current = ''
    for part in parts:
        current += '/' + part
        try:
            sftp.stat(current)
        except:
            sftp.mkdir(current)

count = 0
for root, dirs, files in os.walk(LOCAL_DIR):
    for name in files:
        local_path = os.path.join(root, name)
        relative = os.path.relpath(local_path, LOCAL_DIR).replace('\\', '/')
        remote_path = REMOTE_TMP + '/' + relative
        mkdir_p(sftp, os.path.dirname(remote_path))
        sftp.put(local_path, remote_path)
        count += 1

sftp.close()
print(f'Uploaded {count} files')

ssh.exec_command('docker cp ' + REMOTE_TMP + '/. bus-gallery-frontend:/usr/share/nginx/html/')
print('docker cp done')

ssh.exec_command('rm -rf ' + REMOTE_TMP)
ssh.close()
print('Deploy complete!')
