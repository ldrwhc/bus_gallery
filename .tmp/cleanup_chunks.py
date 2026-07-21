import paramiko, os

local_assets = set(os.listdir(r'D:\code\bus-gallery\frontend\dist\assets'))

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend ls /usr/share/nginx/html/assets/')
server_files = [f.strip() for f in stdout.read().decode().strip().split('\n') if f.strip()]

to_delete = [f for f in server_files if f not in local_assets]
print(f'Server total: {len(server_files)}, Local current: {len(local_assets)}, To delete: {len(to_delete)}')
for f in to_delete:
    print(f'  DELETE {f}')

if to_delete:
    delete_paths = ' '.join([f'/usr/share/nginx/html/assets/{f}' for f in to_delete])
    stdin2, stdout2, stderr2 = ssh.exec_command(f'docker exec bus-gallery-frontend rm -f {delete_paths}')
    err = stderr2.read().decode().strip()
    if err:
        print(f'ERROR: {err}')
    else:
        print('Cleanup done')

ssh.close()
