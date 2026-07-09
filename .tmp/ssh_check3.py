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

# Check logs one by one with exec_command
run_cmd("docker logs --tail=100 bus-gallery-gateway 2>&1")

time.sleep(2)
run_cmd("docker logs --tail=100 bus-gallery-backend 2>&1")

time.sleep(2)
run_cmd("docker logs --tail=100 bus-gallery-bridge 2>&1")

time.sleep(2)
run_cmd("docker logs --tail=50 bus-gallery-group 2>&1")

time.sleep(2)
run_cmd("docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}' 2>&1")

client.close()
