#!/usr/bin/env python3
import subprocess, time

def run(cmd, timeout=15):
    p = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    return p.stdout.decode().strip(), p.stderr.decode().strip(), p.returncode

# Wait for any pending docker compose
time.sleep(3)

print("=== Redis container status ===")
o, e, rc = run("docker ps --format '{{.Names}} {{.Status}}' | grep redis")
print(o)
if e: print("ERR:", e)

print("\n=== Redis health ===")
o, e, rc = run("docker inspect bus-gallery-redis --format '{{.State.Health.Status}}'")
print(o)

print("\n=== ACL LIST ===")
o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")
print(o)
if e: print("STDERR:", e)

print("\n=== Test HLL blocked (should show NOPERM) ===")
o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD security_test x y z 2>&1")
print("PFADD:", o)
if e: print("STDERR:", e)

print("\n=== Test PING ===")
o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")
print("PING:", o)

print("\n=== Test SET/GET ===")
o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 SET testkey val 2>&1")
print("SET:", o)
o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 GET testkey 2>&1")
print("GET:", o)
o, e, rc = run("docker exec bus-gallery-redis redis-cli -a 12345678 DEL testkey 2>&1")
print("DEL:", o)

# If ACL still not loaded, apply it immediately and also to redis.conf
print("\n=== Checking if ACL is actually loaded ===")
if "hyperloglog" not in o.lower() and "nopass" not in o.lower():
    # Need to check the ACL output which is above
    pass

# Check redis.conf inside container
print("\n=== redis.conf ACL lines ===")
o, e, rc = run("docker exec bus-gallery-redis cat /usr/local/etc/redis/redis.conf 2>&1 | grep -i 'user\|acl'")
print(o)

print("\n=== Redis logs (last 5) ===")
o, e, rc = run("docker logs --tail=5 bus-gallery-redis 2>&1")
print(o)
if e: print("ERR:", e)

print("\n=== DONE ===")
