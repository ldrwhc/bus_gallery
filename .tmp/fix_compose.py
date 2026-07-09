#!/usr/bin/env python3
"""Fix Redis docker-compose.yml - remove duplicate aclfile entry"""
import subprocess

def run(cmd):
    p = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15)
    return p.stdout.decode().strip(), p.stderr.decode().strip()

# 1. Fix duplicate aclfile in docker-compose
with open("/root/code/bus_gallery/docker/docker-compose.yml", "r") as f:
    lines = f.readlines()

out = []
seen_aclfile = False
for line in lines:
    stripped = line.strip()
    if '--aclfile' in stripped and 'users.acl' in stripped:
        if seen_aclfile:
            continue  # skip duplicate
        seen_aclfile = True
    out.append(line)

with open("/root/code/bus_gallery/docker/docker-compose.yml", "w") as f:
    f.writelines(out)

print("Removed duplicate --aclfile entries")

# 2. Verify
print("\n=== Redis command after fix ===")
o, _ = run("grep -A 12 'redis-server' /root/code/bus_gallery/docker/docker-compose.yml")
print(o)

# 3. Verify users.acl exists
print("\n=== ACL file ===")
o, _ = run("cat /root/code/bus_gallery/docker/redis/users.acl")
print(o)

# 4. Recreate Redis
print("\n=== Recreating Redis ===")
o, e = run("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps redis 2>&1")
print(o)
if e: print("ERR:", e)

import time
time.sleep(8)

# 5. Verify ACL loaded from file
print("\n=== ACL after restart ===")
o, e = run("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")
print(o)
if e: print("STDERR:", e)

print("\n=== Test HLL blocked ===")
o, e = run("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD after_restart x y 2>&1")
print("PFADD:", o)

print("\n=== Test normal ===")
o, e = run("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")
print("PING:", o)

print("\n=== Redis status ===")
o, e = run("docker inspect bus-gallery-redis --format '{{.State.Health.Status}}'")
print("Health:", o)

# 6. Check logs for ACL loading
print("\n=== Redis logs ===")
o, e = run("docker logs bus-gallery-redis 2>&1 | tail -10")
print(o)
