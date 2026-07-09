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

# ========== PART 1: Fix Redis ACL ==========
print("="*60)
print("PART 1: Fix Redis ACL")
print("="*60)

# Check current docker-compose redis volumes
print("\n--- Current redis volumes ---")
run_cmd("grep -A 3 'volumes:' /root/code/bus_gallery/docker/docker-compose.yml | grep -A 3 'redis:'")

# Actually, let me fix volumes properly. Let me check the exact redis section
run_cmd("grep -n 'redis.conf' /root/code/bus_gallery/docker/docker-compose.yml")

# The volumes section exists but users.acl is not mounted. Let me fix this.
# First find the exact line with redis.conf mount
run_cmd("grep -B1 -A1 'redis.conf' /root/code/bus_gallery/docker/docker-compose.yml")

# Add users.acl mount. Use a Python approach on server.
fix_vol_script = '''
with open("/root/code/bus_gallery/docker/docker-compose.yml", "r") as f:
    content = f.read()

# The redis.conf volume line needs users.acl added after it
old = "- ./redis/redis.conf:/usr/local/etc/redis/redis.conf"
new = "- ./redis/redis.conf:/usr/local/etc/redis/redis.conf\\n      - ./redis/users.acl:/usr/local/etc/redis/users.acl"
# Only replace the FIRST occurrence
content = content.replace(old, new, 1)

with open("/root/code/bus_gallery/docker/docker-compose.yml", "w") as f:
    f.write(content)
print("VOLUMES FIXED")
'''

run_cmd("cat > /tmp/fix_redis_vol.py << 'PYEOF'\n" + fix_vol_script + "\nPYEOF\n")
run_cmd("python3 /tmp/fix_redis_vol.py")

# Also fix the ACL file - user needs nopass since requirepass handles auth
# Actually wait, let me look at this differently
# The issue is that the ACL file says >12345678 but requirepass also sets a password
# In Redis 7.0, requirepass sets the default user password. Then aclfile REPLACES the user.
# Since the aclfile user has >12345678, the user should require password auth.
# BUT the ACL LIST shows nopass - this suggests the aclfile is NOT being loaded.

# The issue might be the aclfile path. The redis.conf says:
# aclfile /usr/local/etc/redis/users.acl
# And we mount users.acl to /usr/local/etc/redis/users.acl
# This should work...

# Let me check if the file exists inside the container
print("\n--- Check ACL file inside container ---")
run_cmd("docker exec bus-gallery-redis cat /usr/local/etc/redis/users.acl 2>&1")

# If not found, the volume mount is the issue
print("\n--- Check redis.conf inside container ---")
run_cmd("docker exec bus-gallery-redis cat /usr/local/etc/redis/redis.conf 2>&1 | tail -5")

# ========== PART 2: Fix port 443 firewall ==========
print("\n" + "="*60)
print("PART 2: Fix port 443 external access")
print("="*60)

# Check firewall
print("\n--- Check iptables ---")
run_cmd("iptables -L INPUT -n -v --line-numbers 2>&1 | head -20")
run_cmd("iptables -L YJ-FIREWALL-INPUT -n -v --line-numbers 2>&1 | head -30")

# Check if firewalld is active
print("\n--- Check firewalld ---")
run_cmd("systemctl is-active firewalld 2>&1 || echo 'firewalld not active'")

# Check cloud security group - this server appears to be on Huawei Cloud
# Huawei Cloud uses security groups that might be blocking 443
print("\n--- Port listening check ---")
run_cmd("ss -tlnp | grep -E ':(80|443) '")

# Try opening port 443 in iptables if it's being blocked
print("\n--- Check if 443 is allowed in iptables ---")
run_cmd("iptables -L YJ-FIREWALL-INPUT -n 2>&1 | grep -E '443|http'")

client.close()
