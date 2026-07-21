import paramiko, os

HOST = '192.144.227.251'
USER = 'root'
PWD = 'whc@njupt020704'
JAR = r'D:\code\bus-gallery\backend\target\bus-gallery-backend-0.0.1-SNAPSHOT.jar'
REMOTE = '/tmp/bus-gallery-backend.jar'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PWD, timeout=15)
print('Connected')

sftp = ssh.open_sftp()
size_mb = os.path.getsize(JAR) / (1024*1024)
print(f'Uploading JAR ({size_mb:.1f} MB)...')
sftp.put(JAR, REMOTE)
sftp.close()
print('Uploaded')

print('Copying into container...')
stdin, stdout, stderr = ssh.exec_command(f'docker cp {REMOTE} bus-gallery-backend:/app/app.jar')
stdout.channel.recv_exit_status()
print('docker cp done')

print('Restarting backend...')
stdin, stdout, stderr = ssh.exec_command('docker restart bus-gallery-backend')
stdout.channel.recv_exit_status()
print('Restart triggered')

ssh.exec_command(f'rm -f {REMOTE}')
ssh.close()
print('Done!')
