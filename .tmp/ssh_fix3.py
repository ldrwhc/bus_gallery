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
            print(out.strip()[:8000])
        if err:
            print("STDERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Check backend section in current docker-compose.yml
run_cmd("cd /root/code/bus_gallery/docker && grep -A 5 'backend:' docker-compose.yml | head -20 2>&1")
run_cmd("cd /root/code/bus_gallery/docker && grep -n 'DB_URL\\|DB_USERNAME\\|DB_PASSWORD\\|environment' docker-compose.yml 2>&1")

time.sleep(1)

# Compare with the most recent backup
run_cmd("cd /root/code/bus_gallery/docker && diff docker-compose.yml docker-compose.yml.bak.20260707_171506 2>&1 | head -60")

time.sleep(1)

# Check what docker compose sees for backend
run_cmd("cd /root/code/bus_gallery/docker && docker compose config 2>&1 | grep -A 80 'backend:' | head -90")

client.close()
