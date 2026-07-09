import paramiko
import time

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

client.connect(
    '192.144.227.251', username='root', password='whc@njupt020704',
    timeout=30, banner_timeout=30, auth_timeout=30,
    allow_agent=False, look_for_keys=False,
)

def run_cmd(cmd):
    print(f"\n{'='*60}")
    print(f"CMD: {cmd}")
    print('='*60)
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=20)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out:
            print(out.strip()[:5000])
        if err:
            print("STDERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Find docker-compose files
run_cmd("find / -maxdepth 4 -name 'docker-compose*.yml' -o -name 'docker-compose*.yaml' 2>/dev/null | head -20")
run_cmd("ls /root/docker/ 2>&1")
run_cmd("ls /home/ 2>&1")

time.sleep(1)

# Check backend env vars
run_cmd("docker inspect bus-gallery-backend --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")

time.sleep(1)

# Check group env vars
run_cmd("docker inspect bus-gallery-group --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")

time.sleep(1)

# Check MySQL connectivity from host
run_cmd("docker exec bus-gallery-mysql mysql -uroot -p123456 -e 'SELECT 1;' 2>&1")

# Check if MySQL port is listening
run_cmd("docker exec bus-gallery-mysql netstat -tlnp 2>&1 || docker exec bus-gallery-mysql ss -tlnp 2>&1")

client.close()
