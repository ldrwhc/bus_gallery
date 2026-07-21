import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Check if the correct chunk exists and has panBy
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend sh -c "grep -o panBy /usr/share/nginx/html/assets/UserProfile-DS71LsJz.js | wc -l"')
print('panBy in DS71LsJz:', stdout.read().decode().strip())

# Check index.html to see if it references the right chunk
stdin2, stdout2, stderr2 = ssh.exec_command('docker exec bus-gallery-frontend sh -c "cat /usr/share/nginx/html/assets/index-ClfgxUwC.js | grep -o UserProfile-[A-Za-z0-9_-]*"')
print('UserProfile chunk ref:', stdout2.read().decode().strip())

ssh.close()
