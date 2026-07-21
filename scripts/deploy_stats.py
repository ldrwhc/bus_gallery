"""Deploy frontend + backend stats feature to production server."""
import paramiko, os, sys

HOST = '192.144.227.251'
USER = 'root'
PWD = 'whc@njupt020704'

FRONTEND_LOCAL = r'D:\code\bus-gallery\frontend\dist'
BACKEND_JAR = r'D:\code\bus-gallery\backend\target\bus-gallery-backend-0.0.1-SNAPSHOT.jar'
REMOTE_TMP = '/tmp/deploy-stats'
REMOTE_JAR_PATH = '/tmp/bus-gallery-backend.jar'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

print('Connecting to server...')
ssh.connect(HOST, username=USER, password=PWD, timeout=15)
print('Connected.')

sftp = ssh.open_sftp()

# ====== Deploy Frontend ======
print('\n--- Deploying Frontend ---')
ssh.exec_command(f'rm -rf {REMOTE_TMP}')

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
for root, dirs, files in os.walk(FRONTEND_LOCAL):
    for name in files:
        local_path = os.path.join(root, name)
        relative = os.path.relpath(local_path, FRONTEND_LOCAL).replace('\\', '/')
        remote_path = REMOTE_TMP + '/' + relative
        mkdir_p(sftp, os.path.dirname(remote_path))
        sftp.put(local_path, remote_path)
        count += 1
        if count % 20 == 0:
            print(f'  Uploaded {count} files...')

print(f'Frontend: uploaded {count} files')
stdin, stdout, stderr = ssh.exec_command(f'docker cp {REMOTE_TMP}/. bus-gallery-frontend:/usr/share/nginx/html/')
stdout.channel.recv_exit_status()
print('Frontend: docker cp done')
ssh.exec_command(f'rm -rf {REMOTE_TMP}')

# ====== Deploy Backend ======
print('\n--- Deploying Backend ---')
file_size_mb = os.path.getsize(BACKEND_JAR) / (1024 * 1024)
print(f'Uploading JAR ({file_size_mb:.1f} MB)...')
sftp.put(BACKEND_JAR, REMOTE_JAR_PATH)
print('Backend: JAR uploaded')

print('Copying JAR into container...')
stdin, stdout, stderr = ssh.exec_command(f'docker cp {REMOTE_JAR_PATH} bus-gallery-backend:/app/app.jar')
stdout.channel.recv_exit_status()
print('Backend: docker cp done')

print('Restarting backend container...')
stdin, stdout, stderr = ssh.exec_command('docker restart bus-gallery-backend')
stdout.channel.recv_exit_status()
print('Backend: restart triggered')

ssh.exec_command(f'rm -f {REMOTE_JAR_PATH}')
sftp.close()
ssh.close()

print('\n========================================')
print('Deploy complete!')
print('Frontend: https://192.144.227.251/stats')
print('Backend API: https://192.144.227.251/api/stats')
print('========================================')
