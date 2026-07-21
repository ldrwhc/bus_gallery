import paramiko

HOST = '192.144.227.251'
USER = 'root'
PWD = 'whc@njupt020704'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PWD, timeout=10)

# Bump vehicle page version to invalidate all caches
cmd = "docker exec bus-gallery-backend redis-cli -a 12345678 -h redis INCR bg:vehicle:page:version"
stdin, stdout, stderr = ssh.exec_command(cmd)
out = stdout.read().decode().strip()
err = stderr.read().decode().strip()
print(f'Redis INCR result: {out}')
if err: print(f'stderr: {err}')

# Also clear any snapshot caches related to CK6120LGEV
cmd2 = "docker exec bus-gallery-backend redis-cli -a 12345678 -h redis KEYS '*CK6120*'"
stdin, stdout, stderr = ssh.exec_command(cmd2)
keys = stdout.read().decode().strip()
print(f'CK6120 keys: {keys}')

ssh.close()
print('Cache flushed')
