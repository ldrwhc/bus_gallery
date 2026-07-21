import paramiko

HOST = '192.144.227.251'
USER = 'root'
PWD = 'whc@njupt020704'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PWD, timeout=10)

# Use redis container for redis-cli
cmds = [
    'docker exec bus-gallery-redis redis-cli -a 12345678 INCR bg:vehicle:page:version',
    'docker exec bus-gallery-redis redis-cli -a 12345678 KEYS "bg:vehicle:page:*"',
]
for cmd in cmds:
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    print(f'CMD: {cmd[-50:]}')
    print(f'  out: {out}')
    if err and 'Warning' not in err: print(f'  err: {err}')

ssh.close()
