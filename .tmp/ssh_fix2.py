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
        stdin, stdout, stderr = client.exec_command(cmd, timeout=60)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out:
            print(out.strip()[:5000])
        if err:
            print("STDERR:", err.strip()[:2000])
    except Exception as e:
        print(f"FAILED: {e}")

# First validate docker compose config
run_cmd("cd /root/code/bus_gallery/docker && docker compose config --services 2>&1")

time.sleep(2)

# Recreate backend and group containers with proper env vars
print("\n\n========== RECREATING BACKEND ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d backend 2>&1")

time.sleep(5)

print("\n\n========== RECREATING GROUP ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d group 2>&1")

time.sleep(5)

# Verify backend now has env vars
print("\n\n========== VERIFY BACKEND ENV ==========")
run_cmd("docker inspect bus-gallery-backend --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")

time.sleep(2)

# Check if backend started successfully
print("\n\n========== BACKEND LOGS ==========")
run_cmd("docker logs --tail=30 bus-gallery-backend 2>&1")

time.sleep(2)

# Check container status
print("\n\n========== CONTAINER STATUS ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' 2>&1")

client.close()
