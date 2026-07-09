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

# Find docker-compose files anywhere
run_cmd("find / -maxdepth 5 -name 'docker-compose*' -type f 2>/dev/null")
run_cmd("ls -la /opt/ 2>&1")
run_cmd("ls -la /app/ 2>&1")

time.sleep(1)

# Check full container inspect for backend
run_cmd("docker inspect bus-gallery-backend --format '{{json .Config.Env}}' 2>&1")
run_cmd("docker inspect bus-gallery-backend --format '{{json .HostConfig.Binds}}' 2>&1")

time.sleep(1)

# Check if there's a docker-compose in the docker network project name
run_cmd("docker compose -f /dev/null ls 2>&1 || docker ps --format '{{.Label \"com.docker.compose.project.config_files\"}}' 2>&1")

# Check labels on the containers
run_cmd("docker inspect bus-gallery-backend --format '{{json .Config.Labels}}' 2>&1")

time.sleep(1)

# Check Gateway logs again - but redirect stderr to null to avoid encoding issue
run_cmd("docker logs --tail=30 bus-gallery-gateway 2>&1 | head -50")

# Check if Nacos is needed
run_cmd("docker inspect bus-gallery-backend --format '{{json .Config.Cmd}}' 2>&1")

# Actually check what port is MySQL using internally
run_cmd("docker exec bus-gallery-mysql sh -c 'echo > /dev/tcp/127.0.0.1/3306 && echo PORT_OPEN || echo PORT_CLOSED' 2>&1")

client.close()
