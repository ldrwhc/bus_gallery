import paramiko
import time
import io

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

# Step 1: Immediate mitigation - block HLL commands on running Redis
print("========== STEP 1: Block HLL commands immediately (runtime) ==========")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 ACL SETUSER default -@hyperloglog 2>&1")

# Step 2: Verify the ACL change
print("\n========== STEP 2: Verify ACL ==========")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")

# Step 3: Verify HLL is now blocked
print("\n========== STEP 3: Test HLL blocked ==========")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD test_blocked a b c 2>&1")

# Step 4: Verify normal commands still work
print("\n========== STEP 4: Test normal commands still work ==========")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 SET test_normal hello 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 GET test_normal 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 DEL test_normal 2>&1")

# Step 5: Create ACL file for persistence
print("\n========== STEP 5: Create persistent ACL file ==========")
acl_content = 'user default on >12345678 ~* &* +@all -@hyperloglog\n'
run_cmd("cat > /root/code/bus_gallery/docker/redis/users.acl << 'EOF'\n" + acl_content + "EOF\n")
run_cmd("cat /root/code/bus_gallery/docker/redis/users.acl 2>&1")

# Step 6: Update redis.conf to use ACL file
print("\n========== STEP 6: Update redis.conf ==========")
run_cmd("echo '' >> /root/code/bus_gallery/docker/redis/redis.conf && echo '# ACL restriction for HLL security' >> /root/code/bus_gallery/docker/redis/redis.conf && echo 'aclfile /usr/local/etc/redis/users.acl' >> /root/code/bus_gallery/docker/redis/redis.conf 2>&1")
# Remove the requirepass since ACL handles auth; keep it for backward compat
run_cmd("tail -5 /root/code/bus_gallery/docker/redis/redis.conf 2>&1")

# Step 7: Mount ACL file in docker-compose
print("\n========== STEP 7: Update docker-compose to mount ACL file ==========")
# Add volume mount for users.acl in redis service
# Also add aclfile to the redis command args
run_cmd("grep -A 20 '^  redis:' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

client.close()
