import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

# Call through gateway
stdin, stdout, stderr = ssh.exec_command('curl -s http://bus-gallery-gateway:8094/api/users/2/footprint | python3 -m json.tool 2>/dev/null | head -30')
print('Footprint via gateway:')
print(stdout.read().decode())

# Also check a sample image URL
stdin2, stdout2, stderr2 = ssh.exec_command('curl -s http://bus-gallery-gateway:8094/api/users/2/images?page=1 | python3 -c "import sys,json; d=json.load(sys.stdin); print(json.dumps(d[\"records\"][0] if d.get(\"records\") else {}, indent=2))" 2>/dev/null')
print('Sample image:')
print(stdout2.read().decode())

ssh.close()
