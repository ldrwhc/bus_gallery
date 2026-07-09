#!/usr/bin/env python3
import subprocess, time

def run(cmd, timeout=15):
    p = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    return p.stdout.decode().strip(), p.stderr.decode().strip(), p.returncode

print("=== 1. Check redis.conf inside container ===")
o, e, rc = run("docker exec bus-gallery-redis head -5 /usr/local/etc/redis/redis.conf")
print("HEAD:", o)

o, e, rc = run("docker exec bus-gallery-redis tail -10 /usr/local/etc/redis/redis.conf")
print("TAIL:")
for line in o.split("\n"):
    print("  ", line)

print("\n=== 2. Check if user directive is there ===")
o, e, rc = run("docker exec bus-gallery-redis grep 'user default' /usr/local/etc/redis/redis.conf")
print("USER LINE:", o)

print("\n=== 3. Try with clean aclfile approach ===")
# Remove user directive from redis.conf, keep aclfile
run("sed -i '/^user default/d' /root/code/bus_gallery/docker/redis/redis.conf")
run("sed -i '/^# ACL user rule/d' /root/code/bus_gallery/docker/redis/redis.conf")
run("sed -i '/^# ACL restriction/d' /root/code/bus_gallery/docker/redis/redis.conf")

# Add aclfile back
run("echo '' >> /root/code/bus_gallery/docker/redis/redis.conf")
run("echo 'aclfile /usr/local/etc/redis/users.acl' >> /root/code/bus_gallery/docker/redis/redis.conf")

# Verify redis.conf
o, e, rc = run("tail -3 /root/code/bus_gallery/docker/redis/redis.conf")
print("redis.conf end:")
print(o)

# Verify users.acl
o, e, rc = run("cat /root/code/bus_gallery/docker/redis/users.acl")
print("users.acl:", o)

# Remove --aclfile from docker-compose command (let redis.conf handle it)
run("cd /root/code/bus_gallery/docker && sed -i '/--aclfile.*users.acl/d' docker-compose.yml")

o, e, rc = run("grep -A 10 'redis-server' /root/code/bus_gallery/docker/docker-compose.yml")
print("\ndocker-compose redis cmd:")
print(o)

# Recreate Redis
print("\n=== 4. Recreating Redis ===")
# Use Popen for long-running command
p = subprocess.Popen("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps redis",
                      shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
try:
    out, err = p.communicate(timeout=30)
    print(out.decode().strip())
    if err.decode().strip():
        print("ERR:", err.decode().strip())
except subprocess.TimeoutExpired:
    p.kill()
    print("docker compose still running, waiting...")
    time.sleep(10)

time.sleep(8)

# Final check
print("\n=== 5. Final check ===")
o, e, rc = run("docker ps | grep redis")
print(o)

o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")
print("\nACL:", o)

o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD check_now x 2>&1")
print("\nPFADD:", o)

o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")
print("\nPING:", o)

# If still not working, apply directly
if "hyperloglog" not in o.lower():
    print("\n=== ACL STILL NOT LOADING - applying directly ===")
    run("docker exec bus-gallery-redis redis-cli ACL SETUSER default on '>12345678' '~*' '&*' +@all -@hyperloglog")
    run("docker exec bus-gallery-redis redis-cli ACL SAVE 2>&1")

    time.sleep(1)
    o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")
    print("ACL after direct:", o)

    o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD final_test2 x 2>&1")
    print("PFADD after direct:", o)

print("\n=== DONE ===")
