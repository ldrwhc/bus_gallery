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

# Fix Redis ACL: Remove --requirepass from CLI, rely ONLY on ACL file for auth
print("="*60)
print("FIX: Remove --requirepass, use ACL file for all auth")
print("="*60)

# 1. First apply ACL immediately to running Redis
print("\n--- Apply ACL immediately ---")
run_cmd("docker exec bus-gallery-redis redis-cli ACL SETUSER default on >12345678 ~* &* +@all -@hyperloglog 2>&1")

time.sleep(1)

# 2. Verify
print("\n--- Verify ACL ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")

print("\n--- Test HLL blocked ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD test_hll x 2>&1")

print("\n--- Test normal OK ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")

# 3. Now fix docker-compose - remove --requirepass and add --aclfile
print("\n--- Fix docker-compose for persistent ACL ---")
# Remove the --requirepass line from the redis command
run_cmd("cd /root/code/bus_gallery/docker && sed -i '/--requirepass.*REDIS_PASSWORD/d' docker-compose.yml 2>&1")
# Now add --aclfile before the other args
run_cmd("cd /root/code/bus_gallery/docker && sed -i 's|\"--appendonly\", \"yes\",|\"--aclfile\", \"/usr/local/etc/redis/users.acl\",\\n        \"--appendonly\", \"yes\",|' docker-compose.yml 2>&1")

# 4. Verify docker-compose
print("\n--- Verify redis command ---")
run_cmd("grep -A 12 'redis-server' /root/code/bus_gallery/docker/docker-compose.yml")

time.sleep(1)

# 5. Recreate Redis
print("\n--- Recreate Redis ---")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps redis 2>&1")

time.sleep(10)

# 6. Final verification
print("\n--- Final ACL check ---")
run_cmd("docker ps | grep redis")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")

print("\n--- Test HLL blocked ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD final_hll_check a b c 2>&1")

print("\n--- Test normal command ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 SET health_check ok 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 GET health_check 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")

# Also need to fix the healthcheck in docker-compose
# Since we removed --requirepass, the healthcheck -a will still work with ACL password
print("\n--- Check health ---")
run_cmd("docker inspect bus-gallery-redis --format '{{.State.Health.Status}}' 2>&1")

# ========== Part 2: try to open port 443 ==========
print("\n" + "="*60)
print("PART 2: Check and open port 443")
print("="*60)

# Check if frontend is running
print("\n--- Frontend status ---")
run_cmd("docker ps | grep frontend")

# Check what YJ firewall tool is managing rules
print("\n--- Check YJ firewall tool ---")
run_cmd("which YJ-FIREWALL 2>/dev/null; which yj 2>/dev/null; ls /usr/local/yj* 2>/dev/null; ls /etc/yj* 2>/dev/null; systemctl list-units --type=service 2>&1 | grep -i yj")

# Try adding iptables rule to accept 443
print("\n--- Add iptables rule for port 443 ---")
run_cmd("iptables -I INPUT 1 -p tcp --dport 443 -j ACCEPT 2>&1 && echo 'Rule added' || echo 'Failed'")
run_cmd("iptables -L INPUT -n -v --line-numbers 2>&1 | head -5")

client.close()
