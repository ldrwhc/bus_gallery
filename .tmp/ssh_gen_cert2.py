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
        stdin, stdout, stderr = client.exec_command(cmd, timeout=30)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out: print(out.strip()[:4000])
        if err: print("ERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Use Python on the server to write config and generate cert in one command
print("=== Writing cert gen script via Python ===")
gen_script = '''
import subprocess, os

conf = """[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = v3_req

[dn]
CN = 192.144.227.251
O = BusGallery
C = CN

[v3_req]
subjectAltName = @alt_names

[alt_names]
IP.1 = 192.144.227.251
"""

os.makedirs("/root/code/bus_gallery/docker/nginx/ssl", exist_ok=True)

with open("/tmp/openssl.cnf", "w") as f:
    f.write(conf)

result = subprocess.run([
    "openssl", "req", "-x509", "-nodes", "-days", "3650",
    "-newkey", "rsa:2048",
    "-keyout", "/root/code/bus_gallery/docker/nginx/ssl/privkey.pem",
    "-out", "/root/code/bus_gallery/docker/nginx/ssl/fullchain.pem",
    "-config", "/tmp/openssl.cnf",
    "-extensions", "v3_req"
], capture_output=True, text=True, timeout=30)

print("STDOUT:", result.stdout)
print("STDERR:", result.stderr)
print("RETURN:", result.returncode)

# Verify
result2 = subprocess.run(["openssl", "x509", "-in", "/root/code/bus_gallery/docker/nginx/ssl/fullchain.pem", "-text", "-noout"], capture_output=True, text=True)
for line in result2.stdout.split("\\n"):
    if "Subject:" in line or "IP Address:" in line:
        print("VERIFY:", line.strip())
'''

# Write the script to server
cmd = f"python3 -c '{gen_script}'"
print("Running Python cert gen on server...")

# Since directly executing Python with multi-line string is messy,
# let's write the script to a file first using SFTP
sftp = client.open_sftp()
sftp.putfo(
    __import__('io').BytesIO(gen_script.encode()),
    '/tmp/gen_cert.py'
)
sftp.close()
print("Script uploaded via SFTP")

# Now run it
run_cmd("python3 /tmp/gen_cert.py")

time.sleep(2)

# Verify files exist
print("\n=== Verify cert files ===")
run_cmd("ls -la /root/code/bus_gallery/docker/nginx/ssl/")

# Restart frontend
print("\n=== Restart frontend ===")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps frontend")

time.sleep(8)

# Check
print("\n=== Check status ===")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' | grep frontend")
run_cmd("docker logs --tail=5 bus-gallery-frontend")
run_cmd("ss -tlnp | grep -E ':(80|443) '")

# Test HTTPS
print("\n=== Test HTTPS ===")
run_cmd("curl -sk -o /dev/null -w 'HTTPS_PAGE:%{http_code}' https://localhost/")
run_cmd("curl -sk -o /dev/null -w 'HTTPS_API:%{http_code}' https://localhost/api/regions")
run_cmd("curl -sI http://localhost/ 2>&1 | head -3")

client.close()
