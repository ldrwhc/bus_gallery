import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Check SSL cert expiry
stdin, stdout, stderr = ssh.exec_command('openssl x509 -in /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -noout -dates -subject 2>&1')
print('SSL Cert:')
print(stdout.read().decode())

# Check nginx server_name
stdin2, stdout2, stderr2 = ssh.exec_command('grep server_name /root/code/bus_gallery/docker/nginx/default-https.conf | head -5')
print('Nginx server_name:')
print(stdout2.read().decode())

ssh.close()
