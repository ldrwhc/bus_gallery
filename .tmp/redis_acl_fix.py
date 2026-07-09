#!/usr/bin/env python3
"""Fix Redis ACL - apply HLL restriction immediately and persistently"""
import subprocess

def run(cmd_list):
    p = subprocess.run(cmd_list, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15)
    return p.stdout.decode().strip(), p.stderr.decode().strip(), p.returncode

# Step 1: Apply immediately to running Redis
print("=== Step 1: Apply ACL to running Redis ===")
out, err, rc = run(["docker", "exec", "bus-gallery-redis", "redis-cli",
                     "ACL", "SETUSER", "default", "on", ">12345678",
                     "~*", "&*", "+@all", "-@hyperloglog"])
print("out:", out)
print("err:", err)
print("rc:", rc)

# Step 2: Verify ACL applied
print("\n=== Step 2: Verify ACL ===")
out, err, rc = run(["docker", "exec", "bus-gallery-redis", "redis-cli",
                      "-a", "12345678", "ACL", "LIST"])
print(out)
if err: print("STDERR:", err)

# Step 3: Test HLL blocked
print("\n=== Step 3: Test HLL blocked ===")
out, err, rc = run(["docker", "exec", "bus-gallery-redis", "redis-cli",
                      "-a", "12345678", "PFADD", "test_hll_sec", "x", "y"])
print("PFADD:", out)
if err: print("STDERR:", err)

# Step 4: Test normal commands still work
print("\n=== Step 4: Test normal commands ===")
for cmd in [["PING"], ["SET", "key1", "val1"], ["GET", "key1"], ["DEL", "key1"]]:
    out, err, rc = run(["docker", "exec", "bus-gallery-redis", "redis-cli",
                          "-a", "12345678"] + cmd)
    print("{}: {}".format(" ".join(cmd), out))

# Step 5: Check ACL file
print("\n=== Step 5: Check ACL file on host ===")
out, err, rc = run(["cat", "/root/code/bus_gallery/docker/redis/users.acl"])
print("ACL file:", out)

# Step 6: Check redis.conf for acl
print("\n=== Step 6: Check redis.conf ===")
out, err, rc = run(["docker", "exec", "bus-gallery-redis", "cat",
                      "/usr/local/etc/redis/redis.conf"])
for line in out.split("\n"):
    if "acl" in line.lower():
        print("CONFIG:", line.strip())

# Step 7: Check Redis logs for ACL loading
print("\n=== Step 7: Redis logs ===")
out, err, rc = run(["docker", "logs", "bus-gallery-redis"])
for line in out.split("\n")[-10:]:
    if line.strip():
        print(line.strip())

print("\n=== Complete ===")
