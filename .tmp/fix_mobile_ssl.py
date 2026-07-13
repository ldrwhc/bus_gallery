import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Disable HTTP/2 for mobile compatibility
ssh.exec_command("sed -i 's/ http2 on;/ #http2 on; # mobile compat/' /root/code/bus_gallery/docker/nginx/default-https.conf", timeout=10)

# Use broad cipher suite (nginx's built-in HIGH level, excludes weak ciphers)
ssh.exec_command("sed -i 's/ssl_ciphers .*;/ssl_ciphers HIGH:!aNULL:!MD5:!RC4;/' /root/code/bus_gallery/docker/nginx/default-https.conf", timeout=10)

# Verify
stdin, stdout, stderr = ssh.exec_command('grep -E "http2|ssl_ciphers" /root/code/bus_gallery/docker/nginx/default-https.conf', timeout=10)
print('Config:')
print(stdout.read().decode())

# Test and reload
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend nginx -t 2>&1', timeout=10)
out = stdout.read().decode() + stderr.read().decode()
print('Test:', out)

stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend nginx -s reload 2>&1', timeout=10)
out = stdout.read().decode() + stderr.read().decode()
print('Reload:', out)

ssh.close()
print('Done - test with phone now')
