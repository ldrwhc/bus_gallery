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

# Step 1: First, restore from the earlier working backup
# The file docker-compose.yml.bak.20260707_171506 should have the env vars
print("========== STEP 1: Force restore from backup ==========")
run_cmd("cp -f /root/code/bus_gallery/docker/docker-compose.yml.bak.20260707_171506 /root/code/bus_gallery/docker/docker-compose.yml 2>&1 && echo 'RESTORED' 2>&1 || echo 'FAILED' 2>&1")

time.sleep(1)

# Step 2: Verify
print("\n========== STEP 2: Verify file has environment ==========")
run_cmd("grep -c 'DB_URL' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")
run_cmd("grep 'environment:' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# Step 3: Fix depends_on for nacos - remove nacos from depends_on sections
# Use Python to edit the file
print("\n========== STEP 3: Fix nacos depends_on ==========")
# We'll use a sed that only comments out nacos dependencies properly
# First check current nacos references
run_cmd("grep -n -B1 -A1 'nacos' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# Step 4: Properly comment out nacos service and remove from depends_on
# Use a Python one-liner approach to fix the file
fix_script = '''
import re

with open('/root/code/bus_gallery/docker/docker-compose.yml', 'r') as f:
    content = f.read()

# 1. Comment out the nacos service block (from "  nacos:" until next top-level service)
# Find nacos service and comment it out
lines = content.split('\\n')
new_lines = []
in_nacos_service = False
for line in lines:
    if line == '  nacos:':
        in_nacos_service = True
    elif in_nacos_service and line and not line.startswith('  '):
        in_nacos_service = False
        new_lines.append(f'#  # nacos service disabled - insufficient memory')

    if in_nacos_service:
        new_lines.append('#' + line)
    else:
        new_lines.append(line)

content = '\\n'.join(new_lines)

# 2. Remove nacos from depends_on blocks
# Pattern: "      nacos:\\n        condition: service_started"
content = content.replace('      nacos:\\n        condition: service_started\\n', '')
# Also handle "      - nacos" in depends_on
content = content.replace('      - nacos\\n', '')

# 3. Remove nacos from gateway depends_on
content = content.replace('      - nacos\\n', '')

with open('/root/code/bus_gallery/docker/docker-compose.yml', 'w') as f:
    f.write(content)

print("FIXED")
'''

run_cmd(f"python3 -c '{fix_script}' 2>&1 || python -c '{fix_script}' 2>&1")

time.sleep(1)

# Step 5: Verify the fix
print("\n========== STEP 5: Verify fix ==========")
run_cmd("grep -n 'nacos' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")
run_cmd("cd /root/code/bus_gallery/docker && docker compose config --services 2>&1")

time.sleep(1)

# Step 6: Redeploy
print("\n========== STEP 6: Redeploy ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d backend group bridge gateway 2>&1")

time.sleep(15)

# Step 7: Verify env vars
print("\n========== STEP 7: Verify backend env ==========")
run_cmd("docker inspect bus-gallery-backend --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")

time.sleep(2)

# Step 8: Check logs and status
print("\n========== STEP 8: Backend logs ==========")
run_cmd("docker logs --tail=15 bus-gallery-backend 2>&1")

time.sleep(1)

print("\n========== STEP 9: Container status ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' 2>&1")

client.close()
