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

# Step 1: Immediately block HLL on running Redis (no -a, since default user has nopass)
print("========== STEP 1: Block HLL immediately ==========")
run_cmd("docker exec bus-gallery-redis redis-cli ACL SETUSER default -@hyperloglog 2>&1")

time.sleep(1)

# Step 2: Verify
print("\n========== STEP 2: Verify HLL blocked ==========")
run_cmd("docker exec bus-gallery-redis redis-cli PFADD test_blocked x y z 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli GET test_normal 2>&1 || echo 'key not found (ok)'")

time.sleep(1)

# Verify ACL
print("\n========== STEP 3: Check ACL ==========")
run_cmd("docker exec bus-gallery-redis redis-cli ACL LIST 2>&1")

# Step 4: Update ACL file with nopass (to work with requirepass)
# The existing requirepass handles auth, so ACL should use nopass
print("\n========== STEP 4: Fix ACL file ==========")
run_cmd("cat > /root/code/bus_gallery/docker/redis/users.acl << 'EOF'\nuser default on nopass ~* &* +@all -@hyperloglog\nEOF\n")
run_cmd("cat /root/code/bus_gallery/docker/redis/users.acl")

# Step 5: Update docker-compose - mount users.acl and add aclfile to command
print("\n========== STEP 5: Update docker-compose ==========")
# Add volume mount for users.acl
# And add --aclfile to the command list

# Read the current redis section
run_cmd("grep -n 'redis.conf' /root/code/bus_gallery/docker/docker-compose.yml")

# Add ACL file command arg after redis.conf line
run_cmd("cd /root/code/bus_gallery/docker && sed -i 's|\"/usr/local/etc/redis/redis.conf\",|\"/usr/local/etc/redis/redis.conf\",\\n        \"--aclfile\", \"/usr/local/etc/redis/users.acl\",|' docker-compose.yml 2>&1")

# Add volume mount before the redis.conf volume
# Find the volumes section for redis
run_cmd("grep -n 'redis.conf' /root/code/bus_gallery/docker/docker-compose.yml")

# Add the ACL file volume mount
run_cmd("cd /root/code/bus_gallery/docker && sed -i 's|- ./redis/redis.conf:/usr/local/etc/redis/redis.conf|- ./redis/redis.conf:/usr/local/etc/redis/redis.conf\\n      - ./redis/users.acl:/usr/local/etc/redis/users.acl|' docker-compose.yml 2>&1")

# Step 6: Verify docker-compose changes
print("\n========== STEP 6: Verify docker-compose redis section ==========")
run_cmd("grep -A 22 '^  redis:' /root/code/bus_gallery/docker/docker-compose.yml")

# Step 7: Recreate Redis container
print("\n========== STEP 7: Recreate Redis container ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps redis 2>&1")

time.sleep(8)

# Step 8: Final verification
print("\n========== STEP 8: Final verification ==========")
run_cmd("docker ps | grep redis")
run_cmd("docker exec bus-gallery-redis redis-cli ACL LIST")
run_cmd("docker exec bus-gallery-redis redis-cli PFADD final_test 1 2 3 2>&1 || echo 'HLL BLOCKED - OK'")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 SET after_acl working 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 GET after_acl 2>&1")

client.close()
