import subprocess, sys, time

results = []

def run(cmd, timeout=30):
    try:
        p = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        out = p.stdout.decode(errors='replace').strip()
        err = p.stderr.decode(errors='replace').strip()
        results.append(f"CMD: {cmd}")
        if out: results.append(f"OUT: {out}")
        if err: results.append(f"ERR: {err}")
        return out, err
    except Exception as ex:
        results.append(f"EXCEPTION: {ex}")
        return "", str(ex)

# Step 1: Apply ACL immediately
run("docker exec bus-gallery-redis redis-cli ACL SETUSER default on '>12345678' '~*' '&*' '+@all' '-@hyperloglog'")

time.sleep(1)

# Step 2: Verify
o, _ = run("docker exec bus-gallery-redis redis-cli -a 12345678 ACL LIST 2>&1")

# Step 3: Test HLL blocked
o, _ = run("docker exec bus-gallery-redis redis-cli -a 12345678 PFADD test_hll_sec x 2>&1")

# Step 4: Test normal
run("docker exec bus-gallery-redis redis-cli -a 12345678 PING 2>&1")

# Step 5: ACL SAVE for persistence
run("docker exec bus-gallery-redis redis-cli -a 12345678 ACL SAVE 2>&1")

# Step 6: Show results
for line in results:
    print(line)

# Step 7: Write to file
with open("/tmp/redis_fix_result.txt", "w") as f:
    f.write("\n".join(results))
print("Results written to /tmp/redis_fix_result.txt")
