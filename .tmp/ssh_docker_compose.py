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

# Read the actual docker-compose on server
run_cmd("cat /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# Check if there's a .env file
run_cmd("cat /root/code/bus_gallery/docker/.env 2>&1")
run_cmd("ls -la /root/code/bus_gallery/docker/ 2>&1")

time.sleep(1)

# Check if nacos container is running (it's in docker-compose but not in ps)
run_cmd("docker ps -a --filter 'name=nacos' --format 'table {{.Names}}\t{{.Status}}' 2>&1")

time.sleep(1)

# Check the full env of all app containers more carefully
run_cmd("docker inspect bus-gallery-backend --format '{{range $k,$v := .Config.Env}}{{$v}}{{\"\\n\"}}{{end}}' 2>&1")
run_cmd("docker inspect bus-gallery-gateway --format '{{range $k,$v := .Config.Env}}{{$v}}{{\"\\n\"}}{{end}}' 2>&1")

client.close()
