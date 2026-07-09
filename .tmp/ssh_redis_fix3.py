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
    print(f"\nCMD: {cmd}")
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=20)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out: print(out.strip()[:4000])
        if err: print("ERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Step 1: Immediate block HLL on running Redis
print("========== STEP 1: Block HLL immediately ==========")
run_cmd("docker exec bus-gallery-redis redis-cli ACL SETUSER default -@hyperloglog 2>&1")

time.sleep(1)

# Step 2: Verify
print("\n========== STEP 2: Verify ==========")
run_cmd("docker exec bus-gallery-redis redis-cli ACL LIST 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli PFADD test_blocked x 2>&1 || echo 'EXPECTED: HLL blocked'")

time.sleep(1)

# Step 3: Write ACL file (keep password for app compatibility)
print("\n========== STEP 3: Write ACL file ==========")
# Use >12345678 to keep password auth working for the application
acl = "user default on >12345678 ~* &* +@all -@hyperloglog"
cmd = f"printf '%s\\n' '{acl}' > /root/code/bus_gallery/docker/redis/users.acl"
run_cmd(cmd)
run_cmd("cat /root/code/bus_gallery/docker/redis/users.acl")

time.sleep(1)

# Step 4: Verify redis.conf has aclfile directive
print("\n========== STEP 4: Verify redis.conf ==========")
run_cmd("grep aclfile /root/code/bus_gallery/docker/redis/redis.conf 2>&1 || echo 'NOT FOUND - adding...'")
# Add if not present
run_cmd("grep -q 'aclfile' /root/code/bus_gallery/docker/redis/redis.conf 2>&1 || echo 'aclfile /usr/local/etc/redis/users.acl' >> /root/code/bus_gallery/docker/redis/redis.conf")
run_cmd("tail -3 /root/code/bus_gallery/docker/redis/redis.conf")

time.sleep(1)

# Step 5: Update docker-compose
print("\n========== STEP 5: Update docker-compose ==========")

# Add volume mount for ACL file
# Current: "- ./redis/redis.conf:/usr/local/etc/redis/redis.conf"
# Target:  "- ./redis/redis.conf:/usr/local/etc/redis/redis.conf\n      - ./redis/users.acl:/usr/local/etc/redis/users.acl"
run_cmd("cd /root/code/bus_gallery/docker && sed -i 's|- ./redis/redis.conf:/usr/local/etc/redis/redis.conf|- ./redis/redis.conf:/usr/local/etc/redis/redis.conf\\n      - ./redis/users.acl:/usr/local/etc/redis/users.acl|' docker-compose.yml 2>&1")

time.sleep(1)

# Verify
print("\n========== STEP 6: Verify docker-compose ==========")
run_cmd("grep -A 25 '^  redis:' /root/code/bus_gallery/docker/docker-compose.yml")

time.sleep(1)

# Step 7: Recreate Redis container to apply ACL file
print("\n========== STEP 7: Recreate Redis ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps redis 2>&1")

time.sleep(10)

# Step 8: Final verification
print("\n========== STEP 8: Final verification ==========")
run_cmd("docker ps --format '{{.Names}} {{.Status}}' | grep redis")
run_cmd("docker logs --tail=5 bus-gallery-redis 2>&1")

time.sleep(1)

print("\n--- ACL LIST ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")

print("\n--- Test HLL blocked ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD final a b 2>&1 || echo 'OK: HLL BLOCKED'")

print("\n--- Test normal commands ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 SET after_fix ok 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 GET after_fix 2>&1")

print("\n--- Health check ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 ping 2>&1")

client.close()
