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
        stdin, stdout, stderr = client.exec_command(cmd, timeout=30)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out:
            print(out.strip()[:5000])
        if err:
            print("STDERR:", err.strip()[:2000])
    except Exception as e:
        print(f"FAILED: {e}")

# Step 1: Check if group also has the same issue
print("========== CHECK GROUP ENV ==========")
run_cmd("docker compose -f /root/code/bus_gallery/docker/docker-compose.yml config 2>&1 | grep -A 40 '^  group:' | head -50")

time.sleep(1)

# Step 2: Restore from the backup that has the environment section
# The .bak.20260707_171506 has the environment sections
print("\n========== RESTORE FROM BACKUP ==========")
run_cmd("cd /root/code/bus_gallery/docker && cp docker-compose.yml docker-compose.yml.broken && cp docker-compose.yml.bak.20260707_171506 docker-compose.yml 2>&1")

time.sleep(1)

# Step 3: Now redeploy backend and group
print("\n========== REDEPLOY BACKEND ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d backend 2>&1")

time.sleep(5)

print("\n========== REDEPLOY GROUP ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d group 2>&1")

time.sleep(8)

# Step 4: Verify
print("\n========== VERIFY BACKEND ENV ==========")
run_cmd("docker inspect bus-gallery-backend --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")

time.sleep(2)

print("\n========== BACKEND LOGS (last 20) ==========")
run_cmd("docker logs --tail=20 bus-gallery-backend 2>&1")

time.sleep(2)

print("\n========== CONTAINER STATUS ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' 2>&1")

client.close()
