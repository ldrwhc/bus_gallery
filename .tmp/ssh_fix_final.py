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

# Write a fix script to server
print("========== Write fix script ==========")
fix_script = r'''#!/usr/bin/env python3
import re

with open('/root/code/bus_gallery/docker/docker-compose.yml', 'r') as f:
    content = f.read()

lines = content.split('\n')
new_lines = []
in_nacos = False
nacos_block_started = False

for line in lines:
    if line.strip() == 'nacos:' and not line.startswith('#'):
        in_nacos = True
        nacos_block_started = True
        continue
    if in_nacos:
        # Check if we're leaving the nacos block (next top-level key or end of file)
        if line and line[0] not in (' ', '\t') and line.strip():
            in_nacos = False
            # Add this non-nacos line
            new_lines.append(line)
        # Skip all nacos service lines
        continue
    new_lines.append(line)

content = '\n'.join(new_lines)

# Remove nacos from depends_on sections
# Pattern: "      nacos:\n        condition: service_started"
content = content.replace('      nacos:\n        condition: service_started\n', '')
content = content.replace('      - nacos\n', '')

with open('/root/code/bus_gallery/docker/docker-compose.yml', 'w') as f:
    f.write(content)

print("DONE")
'''

# Write the script via here-doc
run_cmd("cat > /tmp/fix_nacos.py << 'PYEOF'\n" + fix_script + "\nPYEOF\n")

time.sleep(1)

run_cmd("python3 /tmp/fix_nacos.py 2>&1")

time.sleep(1)

# Verify
print("\n========== Verify nacos removed ==========")
run_cmd("grep -n 'nacos' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")
run_cmd("cd /root/code/bus_gallery/docker && docker compose config --services 2>&1")

time.sleep(1)

# Now redeploy - skip nacos
print("\n========== Redeploy (no deps) ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps backend group 2>&1")

time.sleep(10)

# Verify env vars
print("\n========== Verify backend env ==========")
run_cmd("docker inspect bus-gallery-backend --format '{{range .Config.Env}}{{println .}}{{end}}' 2>&1")

time.sleep(2)

print("\n========== Backend logs (last 20) ==========")
run_cmd("docker logs --tail=20 bus-gallery-backend 2>&1")

time.sleep(1)

print("\n========== Container status ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' 2>&1")

time.sleep(1)

# Also recreate bridge and gateway as they may be affected
print("\n========== Redeploy bridge and gateway ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps bridge gateway 2>&1")

time.sleep(10)

print("\n========== Final status ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' 2>&1")
run_cmd("docker logs --tail=5 bus-gallery-backend 2>&1")
run_cmd("docker logs --tail=5 bus-gallery-group 2>&1")
run_cmd("docker logs --tail=5 bus-gallery-gateway 2>&1")

client.close()
