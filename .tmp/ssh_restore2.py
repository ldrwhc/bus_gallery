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
            print(out.strip()[:6000])
        if err:
            print("STDERR:", err.strip()[:2000])
    except Exception as e:
        print(f"FAILED: {e}")

# Step 1: Restore from backup
print("========== STEP 1: RESTORE FROM BACKUP ==========")
run_cmd("cd /root/code/bus_gallery/docker && cp docker-compose.yml docker-compose.yml.broken.$(date +%Y%m%d_%H%M%S) && cp docker-compose.yml.bak.20260707_171506 docker-compose.yml 2>&1")

time.sleep(1)

# Step 2: Verify the restored file has environment for backend
print("\n========== STEP 2: VERIFY RESTORED FILE ==========")
run_cmd("grep -c 'DB_URL' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")
run_cmd("grep -c 'DB_PASSWORD' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# Step 3: Now edit to disable nacos properly while keeping env vars
# Remove nacos from backend depends_on
print("\n========== STEP 3: DISABLE NACOS IN DEPENDS_ON ==========")
# Use sed to comment out nacos lines in depends_on sections
run_cmd("cd /root/code/bus_gallery/docker && sed -i 's/      nacos:/      # nacos:/g' docker-compose.yml && sed -i 's/        condition: service_started/        # condition: service_started/g' docker-compose.yml 2>&1")
# But that might comment out legitimate conditions too. Let me be more precise.
# Actually, let me check what happened
run_cmd("grep -n 'nacos' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# Step 4: Comment out the nacos service block (keep it but disable it)
# The nacos service block starts at around line 87
print("\n========== STEP 4: CHECK NACOS SERVICE BLOCK ==========")
run_cmd("grep -n -A 12 '^  nacos:' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

# Step 5: Now redeploy
print("\n========== STEP 5: REDEPLOY ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d backend group 2>&1")

time.sleep(10)

# Step 6: Verify backend has env vars now
print("\n========== STEP 6: VERIFY BACKEND ENV ==========")
run_cmd("docker inspect bus-gallery-backend --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")

time.sleep(2)

# Step 7: Check logs
print("\n========== STEP 7: BACKEND LOGS ==========")
run_cmd("docker logs --tail=20 bus-gallery-backend 2>&1")

time.sleep(2)

print("\n========== STEP 8: CONTAINER STATUS ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' 2>&1")

client.close()
