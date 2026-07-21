import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Check nginx config to see how images are served
stdin, stdout, stderr = ssh.exec_command('cat /root/code/bus_gallery/docker/nginx/default-https.conf')
print(stdout.read().decode()[:3000])

ssh.close()
