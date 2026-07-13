import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Check the YJ firewall chain
stdin, stdout, stderr = ssh.exec_command('iptables -L YJ-FIREWALL-INPUT -n 2>/dev/null | head -30', timeout=10)
print('=== YJ-FIREWALL-INPUT ===')
print(stdout.read().decode())

# Check the DOCKER-USER chain
stdin, stdout, stderr = ssh.exec_command('iptables -L DOCKER-USER -n 2>/dev/null', timeout=10)
print('=== DOCKER-USER ===')
print(stdout.read().decode())

# Quick SSL error check without grep
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend tail -5 /var/log/nginx/error.log 2>/dev/null', timeout=10)
print('=== Last error log lines ===')
print(stdout.read().decode())

# Check if there's a rate limiter or connection limiter
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend cat /etc/nginx/nginx.conf 2>/dev/null | grep -E "worker_connections|worker_processes|limit"', timeout=10)
print('=== Nginx main config ===')
print(stdout.read().decode())

ssh.close()
