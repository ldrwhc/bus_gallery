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
        stdin, stdout, stderr = client.exec_command(cmd, timeout=30)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out: print(out.strip()[:4000])
        if err: print("ERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Upload Python 3.6 compatible script
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

if not os.path.isdir("/root/code/bus_gallery/docker/nginx/ssl"):
    os.makedirs("/root/code/bus_gallery/docker/nginx/ssl")

with open("/tmp/openssl.cnf", "w") as f:
    f.write(conf)

result = subprocess.run([
    "openssl", "req", "-x509", "-nodes", "-days", "3650",
    "-newkey", "rsa:2048",
    "-keyout", "/root/code/bus_gallery/docker/nginx/ssl/privkey.pem",
    "-out", "/root/code/bus_gallery/docker/nginx/ssl/fullchain.pem",
    "-config", "/tmp/openssl.cnf",
    "-extensions", "v3_req"
], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)

print("STDOUT:", result.stdout.decode())
print("STDERR:", result.stderr.decode())
print("RETURN:", result.returncode)

# Verify
result2 = subprocess.run(["openssl", "x509", "-in", "/root/code/bus_gallery/docker/nginx/ssl/fullchain.pem", "-text", "-noout"],
    stdout=subprocess.PIPE, stderr=subprocess.PIPE)
out = result2.stdout.decode()
for line in out.split("\\n"):
    if "Subject:" in line or "IP Address:" in line:
        print("VERIFY:", line.strip())
'''

print("=== Uploading via SFTP ===")
sftp = client.open_sftp()
bio = io.BytesIO(gen_script.encode())
sftp.putfo(bio, '/tmp/gen_cert.py')
sftp.close()
print("OK")

# Run
run_cmd("python3 /tmp/gen_cert.py")

time.sleep(1)

# Check
print("\n=== Verify cert files ===")
run_cmd("ls -la /root/code/bus_gallery/docker/nginx/ssl/")
run_cmd("file /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem")

# Restart frontend
print("\n=== Restart frontend ===")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps frontend")

time.sleep(10)

# Check
print("\n=== Status ===")
run_cmd("docker ps | grep frontend")
run_cmd("docker logs --tail=5 bus-gallery-frontend 2>&1")

# Test
print("\n=== Test ===")
run_cmd("curl -sk -o /dev/null -w 'HTTPS: %{http_code}\\n' https://localhost/")
run_cmd("curl -sk https://localhost/api/regions 2>&1 | head -50")
run_cmd("ss -tlnp | grep -E ':(80|443) '")

client.close()
