import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Check which FootprintMap chunk is on the server and verify its content
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend sh -c "cat /usr/share/nginx/html/index.html"')
idx = stdout.read().decode()
print('index.html references:')
for line in idx.split('\n'):
    if 'script' in line or 'link' in line:
        print(' ', line.strip())

# Find and check FootprintMap chunk
stdin2, stdout2, stderr2 = ssh.exec_command('docker exec bus-gallery-frontend sh -c "ls /usr/share/nginx/html/assets/UserProfile-*.js"')
pf = stdout2.read().decode().strip()
print('\nUserProfile chunks:', pf)

# Check the content of the UserProfile chunk for panBy
for f in pf.split('\n'):
    f = f.strip()
    if f:
        stdin3, stdout3, stderr3 = ssh.exec_command(f'docker exec bus-gallery-frontend sh -c "grep -c panBy /usr/share/nginx/html/assets/{f}" 2>/dev/null')
        cnt = stdout3.read().decode().strip()
        print(f'  {f}: panBy count={cnt}')

ssh.close()
