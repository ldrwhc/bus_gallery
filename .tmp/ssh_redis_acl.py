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

# Warmup
run_cmd("echo warmup")

# Step 1: Check Redis version and current ACL config
print("\n========== STEP 1: Check Redis version ==========")
run_cmd("docker exec bus-gallery-redis redis-cli INFO server | grep redis_version")

# Step 2: Check current ACL list
print("\n========== STEP 2: Check current ACL users ==========")
run_cmd("docker exec bus-gallery-redis redis-cli ACL LIST")

# Step 3: Check if ACL file is configured
print("\n========== STEP 3: Check ACL file config ==========")
run_cmd("docker exec bus-gallery-redis redis-cli CONFIG GET aclfile")

# Step 4: Check redis.conf mount location
print("\n========== STEP 4: Check redis startup ==========")
run_cmd("docker inspect bus-gallery-redis --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}'")

# Step 5: Check existing redis.conf on host
print("\n========== STEP 5: Check host redis.conf ==========")
run_cmd("cat /root/code/bus_gallery/docker/redis/redis.conf 2>&1 | head -30")

# Step 6: Test HLL commands currently work
print("\n========== STEP 6: Test HLL command ==========")
run_cmd("docker exec bus-gallery-redis redis-cli PFADD testhll a b c 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli PFCOUNT testhll 2>&1")
run_cmd("docker exec bus-gallery-redis redis-cli DEL testhll 2>&1")

client.close()
