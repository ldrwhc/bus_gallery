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
            print(out.strip()[:4000])
        if err:
            print("STDERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Wait for gateway to finish starting
print("Waiting 30s for gateway to fully start...")
time.sleep(30)

# Check gateway logs
print("\n========== Gateway logs ==========")
run_cmd("docker logs --tail=10 bus-gallery-gateway 2>&1")

time.sleep(1)

# Test API endpoints
print("\n========== Test /api/regions ==========")
run_cmd("curl -s -o /dev/null -w '%{http_code}' http://localhost:80/api/regions 2>&1")

time.sleep(1)

print("\n========== Test /api/vehicles?size=10 ==========")
run_cmd("curl -s -o /dev/null -w '%{http_code}' http://localhost:80/api/vehicles?size=10 2>&1")

time.sleep(1)

print("\n========== Test /api/catalog/models ==========")
run_cmd("curl -s -o /dev/null -w '%{http_code}' http://localhost:80/api/catalog/models 2>&1")

time.sleep(1)

# Check all services status
print("\n========== Final container status ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' 2>&1")

# Also check if gateway finished starting
print("\n========== Gateway startup status ==========")
run_cmd("docker logs --tail=3 bus-gallery-gateway 2>&1 | grep -i 'started\|error\|exception'")

client.close()
