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

# ========== PART 1: Redis ACL Fix ==========
print("="*60)
print("PART 1: Redis ACL - Restart with ACL file")
print("="*60)

# First check Redis logs for ACL errors
print("\n--- Redis logs for ACL errors ---")
run_cmd("docker logs bus-gallery-redis 2>&1 | grep -i 'acl\|error\|warn' | tail -10")

# The requirepass overrides the ACL file. Let me check what happens.
# In Redis 7.0, when you have BOTH aclfile AND --requirepass:
# --requirepass sets the default user password, which MAY override aclfile settings.
# To use aclfile properly, we should either REMOVE --requirepass and put password in ACL,
# OR apply ACL via redis-cli at startup.

# Solution: Remove --requirepass from docker-compose, rely on ACL file for auth
# The ACL file already has the password: user default on >12345678

# BUT - changing this could break existing connections. Let's be safe:
# Instead, add an @postconnect script... No, that won't work.
# Simplest fix: use ACL SETUSER at runtime via a startup command

# Actually, the cleanest approach: modify redis command to use ACL file properly
# Remove --requirepass and add --aclfile instead
print("\n--- Fix redis docker-compose command ---")

# Current command has --requirepass. We want to ALSO specify the aclfile via CLI.
# In Redis 7.0, --aclfile overrides the config file's aclfile directive.
# So let me add --aclfile to the command args:
run_cmd("grep -A 10 'command:' /root/code/bus_gallery/docker/docker-compose.yml | grep -A 10 'redis:' | head -15")
# Just check the redis command specifically
run_cmd("grep -B5 -A10 'redis-server' /root/code/bus_gallery/docker/docker-compose.yml")

# I'll add --aclfile to the command args using sed
run_cmd("cd /root/code/bus_gallery/docker && sed -i 's|\"--requirepass\", \"${REDIS_PASSWORD:-12345678}\",|\"--aclfile\", \"/usr/local/etc/redis/users.acl\",\\n        \"--requirepass\", \"${REDIS_PASSWORD:-12345678}\",|' docker-compose.yml 2>&1")

# Verify
run_cmd("grep -A 12 'redis-server' /root/code/bus_gallery/docker/docker-compose.yml")

# Recreate Redis
print("\n--- Recreate Redis ---")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps redis 2>&1")

time.sleep(10)

# Verify
print("\n--- Final ACL check ---")
run_cmd("docker ps | grep redis")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")
print("\n--- Test HLL blocked ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD test_final x 2>&1")
print("\n--- Test normal command ---")
run_cmd("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")

# ========== PART 2: Port 443 external access ==========
print("\n" + "="*60)
print("PART 2: Port 443 external access")
print("="*60)

# Check full YJ-FIREWALL-INPUT rules
print("\n--- Full YJ-FIREWALL-INPUT chain ---")
run_cmd("iptables -L YJ-FIREWALL-INPUT -n 2>&1 | tail -30")

# Check if there's specific port filtering
run_cmd("iptables -L YJ-FIREWALL-INPUT -n 2>&1 | grep -E 'dpt:|443|https'")

# Check cloud metadata for type
print("\n--- Check if Huawei Cloud ---")
run_cmd("curl -s --connect-timeout 3 http://169.254.169.254/openstack/latest/meta_data.json 2>&1 | head -3 || echo 'not openstack'")

# The server uses Huawei Cloud images, so it's likely on Huawei Cloud.
# Huawei Cloud ECS uses security groups to control port access.
# Port 443 needs to be allowed in the security group.
print("\nNOTE: Port 443 is listening on server but external connection times out.")
print("This is likely a cloud security group blocking TCP 443.")
print("You need to add an inbound rule in Huawei Cloud console:")
print("  Security Group -> Inbound Rules -> Add: TCP 443 from 0.0.0.0/0")

client.close()
