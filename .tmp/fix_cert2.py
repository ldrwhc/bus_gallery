import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Check cert/key match on host
stdin, stdout, stderr = ssh.exec_command('cd /root/code/bus_gallery/docker/nginx/ssl && openssl x509 -noout -modulus -in fullchain.pem | openssl md5 && openssl rsa -noout -modulus -in privkey.pem | openssl md5', timeout=10)
out = stdout.read().decode()
print('Cert/Key MD5:', out)

# Check cert validity
stdin, stdout, stderr = ssh.exec_command('openssl verify -CAfile /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem 2>&1', timeout=10)
print('Cert verify:', stdout.read().decode(), stderr.read().decode())

# Check access.log last few entries
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend sh -c "tail -3 /var/log/nginx/access.log 2>/dev/null"', timeout=10)
print('Access log:', stdout.read().decode())

# Try restarting instead of reloading
stdin, stdout, stderr = ssh.exec_command('docker restart bus-gallery-frontend 2>&1', timeout=20)
print('Restart:', stdout.read().decode())

import time
time.sleep(5)

# Check status
stdin, stdout, stderr = ssh.exec_command('docker ps --filter name=bus-gallery-frontend --format "{{.Status}}"', timeout=10)
print('Status:', stdout.read().decode())

ssh.close()
