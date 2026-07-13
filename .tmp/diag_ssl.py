import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# 1. Check iptables rules
stdin, stdout, stderr = ssh.exec_command('iptables -L -n | head -30', timeout=10)
print('=== iptables ===')
print(stdout.read().decode())

# 2. Check if multiple nginx processes
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend ps aux | grep nginx', timeout=10)
print('=== nginx procs ===')
print(stdout.read().decode())

# 3. Check nginx error log for SSL errors
stdin, stdout, stderr = ssh.exec_command('docker exec bus-gallery-frontend grep -i "ssl\|error\|emerg\|alert" /var/log/nginx/error.log 2>/dev/null | tail -20', timeout=10)
print('=== SSL errors ===')
out = stdout.read().decode()
print(out if out.strip() else '(none)')

# 4. Check if port 443 is correctly mapped
stdin, stdout, stderr = ssh.exec_command('docker port bus-gallery-frontend', timeout=10)
print('=== Port mapping ===')
print(stdout.read().decode())

# 5. Test from within Docker network (bypass host iptables)
stdin, stdout, stderr = ssh.exec_command('docker run --rm --network container:bus-gallery-frontend curlimages/curl:latest curl -sk --connect-timeout 5 -o /dev/null -w "%{http_code}" https://localhost/ 2>&1 || echo "curl image not available"', timeout=30)
print('=== Docker network test ===')
print(stdout.read().decode())

ssh.close()
